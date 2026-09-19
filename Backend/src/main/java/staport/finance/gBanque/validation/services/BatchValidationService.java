package staport.finance.gBanque.validation.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.transactions.enums.SensOperation;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.dtos.lot.BatchValidationRequestDto;
import staport.finance.gBanque.validation.dtos.lot.BatchValidationResponseDto;
import staport.finance.gBanque.validation.dtos.lot.OperationValideeDto;
import staport.finance.gBanque.validation.services.handlers.BatchValidationHandler;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Orchestrateur de validation (rapprochement) en lot.
 * <p>
 * Applique les mêmes champs communs (TVA, date opération, date valeur, rapprochement cible) à un
 * ensemble d'opérations de même {@link TransactionSource} (et même {@link SensOperation} pour les
 * types qui distinguent décaissement/encaissement), en réutilisant telles quelles les méthodes
 * unitaires existantes de chaque {@code XxxValidationService} de module — aucune logique métier
 * n'est dupliquée ici, seul le dispatch et l'agrégation du résultat le sont.
 * <p>
 * Toutes les 20 sources en lot sont désormais portées par un registre de
 * {@link BatchValidationHandler} (migration menée du 14 au 19/09/2026, une implémentation
 * {@code @Component} par source, découverte et injectée automatiquement par Spring) —
 * {@link #processOne} délègue systématiquement au handler enregistré pour la
 * {@link TransactionSource} de la requête ; il n'y a plus de switch historique. Ajouter une
 * nouvelle source ne demande plus de modifier cette classe, seulement d'ajouter une implémentation
 * de {@link BatchValidationHandler} dans le module de cette source.
 * {@code TransactionSource.LCN_ESCOMPTE} n'a jamais eu de handler — non pris en charge pour la
 * validation en lot (voir Javadoc de {@code LcnDecaissementBatchValidationHandler}).
 * <p>
 * Trois familles de patron cohabitent parmi les handlers (voir leurs Javadocs respectifs pour le
 * détail) :
 * <ul>
 *     <li>Sens exigé par la validation en lot (Virement, Chèque, MiseADisposition, Virement
 *     international) — {@link #requireSensIfNeeded} exige toujours {@code sens} pour ces
 *     sources (homogénéité de la sélection en lot côté frontend), mais depuis la fusion du
 *     19/09/2026 des méthodes de validation unitaires à deux sens en une seule par source (le sens
 *     réel est lu directement sur l'entité par le mapper), le handler n'a plus besoin de brancher
 *     dessus : un seul appel de service, comme la famille "sens porté par l'entité" ci-dessous.</li>
 *     <li>Sens porté par {@code transactionSource} lui-même (LCN — {@code LCN_DECAISSEMENT}/
 *     {@code LCN_ENCAISSEMENT}), un handler par valeur, aucun branchement.</li>
 *     <li>Sens porté par l'entité, jamais transmis par l'appelant, un seul appel de service par
 *     source (Préfinancement/Factoring/Droit constaté via {@code Type}, Décomptes toujours
 *     encaissement, Restitution/Provision/Impayée idem, et les 6 sources toujours-décaissement
 *     Agios/Commission/Frais bancaire/Prélèvement/Impôt-Taxe/Opération erronée). Parmi celles-ci,
 *     seules Préfinancement/Factoring/Droit constaté/Décomptes exigent un {@code banqueId}
 *     explicite (voir {@link #requireBanqueIdIfNeeded}) pour vérifier la cohérence avec la banque
 *     du marché associé, la banque propre à l'entité, ou la banque d'encaissement — toutes les
 *     autres résolvent la banque directement depuis l'entité.</li>
 * </ul>
 * <p>
 * La saisie libre du montant de TVA n'est volontairement pas disponible en lot : un montant fixe
 * appliqué à N opérations de montants différents n'a pas de sens comptable. Le lot ne transporte
 * donc qu'un taux, que le service de chaque module convertit sur le TTC de chaque opération via
 * {@code TvaResolver} — le champ {@code montantTva} des DTO unitaires reste {@code null} ici.
 * <p>
 * Volontairement absents du DTO de lot : libellé, observation, date d'émission. Propres à chaque
 * opération, ils ne sont jamais écrasés par une saisie unique appliquée à tout le lot — chaque
 * handler laisse ces champs à {@code null} sur son DTO unitaire, ce qui déclenche le fallback sur
 * l'entité source dans le mapper correspondant (voir {@link BatchValidationRequestDto}).
 * <p>
 * Le traitement est tout-ou-rien : la méthode est {@code @Transactional}, donc si une opération du
 * lot échoue (déjà réglée entre-temps, rapprochement clôturé, etc.), l'exception remonte et aucune
 * des opérations du lot n'est persistée.
 * <p>
 * Cas particulier CMT / LEASING : l'identifiant de lot porte l'ID de l'échéance (pas celui du
 * contrat parent). Le contrat (CreditMoyenTerme / ContratLeasing) est dérivé côté serveur à partir
 * de la relation {@code @ManyToOne} de l'échéance elle-même — jamais fourni par le client, qui ne
 * transmet qu'une liste d'IDs génériques ({@link BatchValidationRequestDto()}).
 * <p>
 * Hors périmètre : CAUTION (géré par le module {@code cautions}, ne transite pas par ce contrôleur).
 */
@Service
public class BatchValidationService {

    /** Un handler par {@link TransactionSource} en lot — voir Javadoc de la classe. */
    private final Map<TransactionSource, BatchValidationHandler> handlerRegistry;

    public BatchValidationService(List<BatchValidationHandler> batchValidationHandlers) {
        this.handlerRegistry = batchValidationHandlers.stream()
                .collect(Collectors.toMap(BatchValidationHandler::source, h -> h, (a, b) -> {
                    throw new IllegalStateException(
                            "Deux BatchValidationHandler enregistrés pour la même source: " + a.source());
                }));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BatchValidationResponseDto processBatch(BatchValidationRequestDto requestDto) {
        requireSensIfNeeded(requestDto);
        requireBanqueIdIfNeeded(requestDto);

        List<OperationValideeDto> resultats = new ArrayList<>();
        for (Long id : requestDto.ids()) {
            resultats.add(processOne(id, requestDto));
        }

        BigDecimal totalDebit = resultats.stream().map(OperationValideeDto::debit)
                .filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = resultats.stream().map(OperationValideeDto::credit)
                .filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new BatchValidationResponseDto(resultats, totalDebit, totalCredit);
    }

    private OperationValideeDto processOne(Long id, BatchValidationRequestDto requestDto) {
        BatchValidationHandler handler = handlerRegistry.get(requestDto.transactionSource());
        if (handler == null) {
            throw new ValidationException(
                    "La validation en lot n'est pas prise en charge pour le type d'opération: "
                            + requestDto.transactionSource());
        }
        return handler.processOne(id, requestDto);
    }

    /**
     * Types qui distinguent décaissement/encaissement : {@code sens} est obligatoire pour eux.
     */
    private void requireSensIfNeeded(BatchValidationRequestDto requestDto) {
        boolean sensRequis = switch (requestDto.transactionSource()) {
            case Virement, CHEQUE, MiseADisposition, VIREMENT_INTERNATIONAL -> true;
            default -> false;
        };
        if (sensRequis && requestDto.sens() == null) {
            throw new ValidationException(
                    "Le sens (DECAISSEMENT/ENCAISSEMENT) est obligatoire pour le type d'opération: "
                            + requestDto.transactionSource());
        }
    }

    /**
     * Types dont l'endpoint unitaire exige explicitement un banqueId (non déduit de l'entité).
     */
    private void requireBanqueIdIfNeeded(BatchValidationRequestDto requestDto) {
        boolean banqueIdRequis = switch (requestDto.transactionSource()) {
            case DECOMPTES, DROIT_CONSTATE, FACTORING, PREFINANCEMENT -> true;
            default -> false;
        };
        if (banqueIdRequis && requestDto.banqueId() == null) {
            throw new ValidationException(
                    "banqueId est obligatoire pour le type d'opération: " + requestDto.transactionSource());
        }
    }
}
