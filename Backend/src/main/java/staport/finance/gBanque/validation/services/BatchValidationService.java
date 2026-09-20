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
 * Orchestrateur de validation (rapprochement) — point d'entrée unique pour toutes les sources,
 * qu'il s'agisse de valider une seule opération ({@code ids} à un élément) ou un lot ({@code ids}
 * à plusieurs éléments). Voir {@code conventions/architecture.md}, Règle 1 (révisée le 19/09/2026) :
 * valider une opération n'est qu'un cas particulier de valider un lot de taille 1 — il n'existe
 * donc plus ni endpoint ni DTO unitaire séparés (les ~22 endpoints {@code /process/{source}}
 * historiques ont été supprimés de {@code ValidationController} au profit du seul
 * {@code POST /rapprochement/process}).
 * <p>
 * Applique les mêmes champs communs (TVA, date opération, date valeur, rapprochement cible) à
 * l'ensemble des opérations de la requête, en réutilisant telles quelles les méthodes de
 * validation existantes de chaque {@code XxxValidationService} de module — aucune logique métier
 * n'est dupliquée ici, seul le dispatch et l'agrégation du résultat le sont.
 * <p>
 * Toutes les sources sont portées par un registre de {@link BatchValidationHandler} (une
 * implémentation {@code @Component} par source, découverte et injectée automatiquement par
 * Spring) — {@link #processOne} délègue systématiquement au handler enregistré pour la
 * {@link TransactionSource} de la requête. Ajouter une nouvelle source ne demande jamais de
 * modifier cette classe, seulement d'ajouter une implémentation de {@link BatchValidationHandler}
 * dans le module de cette source.
 * <p>
 * Trois familles de patron cohabitent parmi les handlers (voir leurs Javadocs respectifs pour le
 * détail) :
 * <ul>
 *     <li>Sens exigé par la requête (Virement, Chèque, LCN, MiseADisposition, Virement
 *     international) — {@link #requireSensIfNeeded} exige {@code sens} pour homogénéité de la
 *     sélection en lot côté frontend, mais depuis la fusion du 19/09/2026 des méthodes de
 *     validation à deux sens en une seule par source (le sens réel est lu directement sur l'entité
 *     par le mapper), le handler n'a plus besoin de brancher dessus (LCN excepté : deux entrées de
 *     registre distinctes, voir plus bas).</li>
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
 * {@code montantTva} (saisie libre de TVA), {@code observation} et {@code libelle} ne sont
 * autorisés que lorsque {@code ids} contient un seul élément (voir
 * {@link #requireSingleOperationForPerOperationFields}) : un montant de TVA fixe, ou un
 * libellé/une observation fixes, appliqués à N opérations différentes n'ont pas de sens. Sur un
 * vrai lot (ou une opération unique sans édition), ces trois champs restent {@code null} et
 * chaque handler retombe sur l'entité source via son mapper (voir {@link BatchValidationRequestDto}).
 * {@code dateEmission} ne fait volontairement pas partie de cette requête : le dialogue de
 * validation unitaire la verrouille toujours sur la valeur de l'entité, le fallback sur l'entité
 * produit donc déjà exactement le même résultat.
 * <p>
 * Le traitement est tout-ou-rien : la méthode est {@code @Transactional}, donc si une opération de
 * la requête échoue (déjà réglée entre-temps, rapprochement clôturé, etc.), l'exception remonte et
 * aucune des opérations n'est persistée.
 * <p>
 * Cas particulier CMT / LEASING : l'identifiant transmis est celui de l'échéance (pas celui du
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
        requireSingleOperationForPerOperationFields(requestDto);

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

    /**
     * {@code montantTva}, {@code observation} et {@code libelle} sont propres à une opération —
     * les autoriser sur un vrai lot écraserait des valeurs différentes d'une opération à l'autre
     * avec une seule valeur commune (voir {@link BatchValidationRequestDto}).
     */
    private void requireSingleOperationForPerOperationFields(BatchValidationRequestDto requestDto) {
        boolean champPropreAUneOperationRenseigne = requestDto.montantTva() != null
                || requestDto.observation() != null
                || requestDto.libelle() != null;
        if (champPropreAUneOperationRenseigne && requestDto.ids().size() > 1) {
            throw new ValidationException(
                    "montantTva, observation et libelle ne sont autorisés que pour la validation d'une seule opération (ids à un seul élément).");
        }
    }
}
