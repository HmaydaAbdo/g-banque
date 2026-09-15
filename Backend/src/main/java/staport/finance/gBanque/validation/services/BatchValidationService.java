package staport.finance.gBanque.validation.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Agios.dtos.AgiosTransactionResponse;
import staport.finance.gBanque.Agios.dtos.AgiosValidationRequest;
import staport.finance.gBanque.Agios.service.interfaces.IAgiosValidationService;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.moyensDePaiements.cheques.validation.dto.ChequeTransactionRequest;
import staport.finance.gBanque.moyensDePaiements.cheques.validation.dto.ChequeTransactionResponseDto;
import staport.finance.gBanque.moyensDePaiements.cheques.validation.services.interfaces.IChequeValidationService;
import staport.finance.gBanque.transactions.enums.SensOperation;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.commision.validation.dto.CommissionTransactionRequestDto;
import staport.finance.gBanque.commision.validation.dto.CommissionTransactionResponseDto;
import staport.finance.gBanque.commision.validation.services.interfaces.ICommissionValidationService;
import staport.finance.gBanque.decomptes.validation.dto.DecompteTransactionRequestDto;
import staport.finance.gBanque.decomptes.validation.dto.DecompteTransactionResponseDto;
import staport.finance.gBanque.decomptes.validation.services.interfaces.IDecompteValidationService;
import staport.finance.gBanque.droitConstate.validation.dto.DroitConstateTransactionRequest;
import staport.finance.gBanque.droitConstate.validation.dto.DroitConstateTransactionResponse;
import staport.finance.gBanque.droitConstate.validation.services.interfaces.IDroitConstateValidationService;
import staport.finance.gBanque.factoring.validation.dto.FactoringTransactionRequest;
import staport.finance.gBanque.factoring.validation.dto.FactoringTransactionResponse;
import staport.finance.gBanque.factoring.validation.services.interfaces.IFactoringValidationService;
import staport.finance.gBanque.fraisBancaires.validation.dto.FraisBancaireTransactionRequestDto;
import staport.finance.gBanque.fraisBancaires.validation.dto.FraisBancaireTransactionResponseDto;
import staport.finance.gBanque.fraisBancaires.validation.services.interfaces.IFraisBancaireValidationService;
import staport.finance.gBanque.impayees.validation.dto.ImpayeeTransactionRequestDto;
import staport.finance.gBanque.impayees.validation.dto.ImpayeeTransactionResponseDto;
import staport.finance.gBanque.impayees.validation.services.interfaces.IImpayeeValidationService;
import staport.finance.gBanque.impot_taxes.validation.dto.ImpotEtTaxeTransactionRequestDto;
import staport.finance.gBanque.impot_taxes.validation.dto.ImpotEtTaxeTransactionResponseDto;
import staport.finance.gBanque.impot_taxes.validation.services.interfaces.IImpotEtTaxeValidationService;
import staport.finance.gBanque.provision.validation.dto.ProvisionTransactionRequestDto;
import staport.finance.gBanque.provision.validation.dto.ProvisionTransactionResponseDto;
import staport.finance.gBanque.provision.validation.services.interfaces.IProvisionValidationService;
import staport.finance.gBanque.moyensDePaiements.lcn.validation.dto.LcnTransactionRequest;
import staport.finance.gBanque.moyensDePaiements.lcn.validation.dto.LcnTransactionResponseDto;
import staport.finance.gBanque.moyensDePaiements.lcn.validation.services.interfaces.ILcnValidationService;
import staport.finance.gBanque.validation.dtos.lot.BatchValidationRequestDto;
import staport.finance.gBanque.validation.dtos.lot.BatchValidationResponseDto;
import staport.finance.gBanque.validation.dtos.lot.OperationValideeDto;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.validation.dto.MiseAdispositionTransactionRequestDto;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.validation.dto.MiseAdispositionTransactionResponseDto;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.validation.services.interfaces.IMiseADispositionValidationService;
import staport.finance.gBanque.prefinancement.validation.dto.PrefinancementTransactionRequest;
import staport.finance.gBanque.prefinancement.validation.dto.PrefinancementTransactionResponse;
import staport.finance.gBanque.prefinancement.validation.services.interfaces.IPrefinancementValidationService;
import staport.finance.gBanque.prelevements.validation.dto.PrelevementTransactionResponse;
import staport.finance.gBanque.prelevements.validation.dto.PrelevementValidationRequest;
import staport.finance.gBanque.prelevements.validation.services.interfaces.IPrelevementValidationService;
import staport.finance.gBanque.restitution.validation.dto.RestitutionTransactionRequestDto;
import staport.finance.gBanque.restitution.validation.dto.RestitutionTransactionResponseDto;
import staport.finance.gBanque.restitution.validation.services.interfaces.IRestitutionValidationService;
import staport.finance.gBanque.operationErrone.validation.dto.OperationErroneTransactionRequestDto;
import staport.finance.gBanque.operationErrone.validation.dto.OperationErroneTransactionResponseDto;
import staport.finance.gBanque.operationErrone.validation.services.interfaces.IOperationErroneValidationService;
import staport.finance.gBanque.moyensDePaiements.virementInternational.validation.dto.VirementInternationalTransactionRequestDto;
import staport.finance.gBanque.moyensDePaiements.virementInternational.validation.dto.VirementInternationalTransactionResponseDto;
import staport.finance.gBanque.moyensDePaiements.virementInternational.validation.services.interfaces.IVirementInternationalValidationService;
import staport.finance.gBanque.moyensDePaiements.virements.validation.dto.VirementTransactionRequestDto;
import staport.finance.gBanque.moyensDePaiements.virements.validation.dto.VirementTransactionResponseDto;
import staport.finance.gBanque.moyensDePaiements.virements.validation.services.interfaces.IVirementValidationService;
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
 * unitaires existantes de chaque {@code XxxValidationService} de module / {@link IAgiosValidationService} —
 * aucune logique métier n'est dupliquée ici, seul le dispatch et l'agrégation du résultat le sont.
 * <p>
 * Migration en cours (14-15/09/2026) vers un registre de {@link BatchValidationHandler} plutôt
 * qu'un unique switch : Leasing et CMT sont les deux premières sources migrées
 * ({@code LeasingBatchValidationHandler}, {@code CmtBatchValidationHandler}, découverts et injectés
 * automatiquement par Spring) — {@link #processOne} vérifie d'abord le registre, puis retombe sur
 * le switch historique pour les 20 sources pas encore migrées. Ajouter une source migrée ne
 * demande plus de modifier cette classe, seulement d'ajouter une implémentation de
 * {@link BatchValidationHandler} dans le module de cette source.
 * <p>
 * La saisie libre du montant de TVA n'est volontairement pas disponible en lot : un montant fixe
 * appliqué à N opérations de montants différents n'a pas de sens comptable. Le lot ne transporte
 * donc qu'un taux, que le service de chaque module convertit sur le TTC de chaque opération via
 * {@code TvaResolver} — le champ {@code montantTva} des DTO unitaires reste {@code null} ici.
 * <p>
 * Volontairement absents du DTO de lot : libellé, observation, date d'émission. Propres à chaque
 * opération, ils ne sont jamais écrasés par une saisie unique appliquée à tout le lot — chaque
 * DTO unitaire construit par {@link #processOne} laisse ces champs à {@code null}, ce qui
 * déclenche le fallback sur l'entité source dans le mapper correspondant (voir
 * {@link BatchValidationRequestDto}).
 * <p>
 * Le traitement est tout-ou-rien : la méthode est {@code @Transactional}, donc si une opération du
 * lot échoue (déjà réglée entre-temps, rapprochement clôturé, etc.), l'exception remonte et aucune
 * des opérations du lot n'est persistée.
 * <p>
 * Cas particulier LCN : contrairement à Virement/Cheque/MiseADisposition/VirementInternational, le
 * sens n'est pas porté par {@code sens} mais directement par {@code transactionSource}
 * ({@code LCN_DECAISSEMENT} / {@code LCN_ENCAISSEMENT}) — {@code TransactionSource.LCN} (valeur
 * générique) n'existe que pour d'anciens enregistrements en base et n'est pas utilisé ici.
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

    private final IFraisBancaireValidationService fraisBancaireValidationService;
    private final IRestitutionValidationService restitutionValidationService;
    private final IOperationErroneValidationService operationErroneValidationService;
    private final IImpayeeValidationService impayeeValidationService;
    private final IAgiosValidationService agiosValidationService;
    /** Sources migrées vers {@link BatchValidationHandler} (Leasing, CMT à ce jour) — voir Javadoc de la classe. */
    private final Map<TransactionSource, BatchValidationHandler> handlerRegistry;
    private final IChequeValidationService chequeValidationService;
    private final ILcnValidationService lcnValidationService;
    private final IVirementValidationService virementValidationService;
    private final IVirementInternationalValidationService virementInternationalValidationService;
    private final IMiseADispositionValidationService miseADispositionValidationService;
    private final IPrelevementValidationService prelevementValidationService;
    private final IPrefinancementValidationService prefinancementValidationService;
    private final IFactoringValidationService factoringValidationService;
    private final IDroitConstateValidationService droitConstateValidationService;
    private final IDecompteValidationService decompteValidationService;
    private final IImpotEtTaxeValidationService impotEtTaxeValidationService;
    private final ICommissionValidationService commissionValidationService;
    private final IProvisionValidationService provisionValidationService;

    public BatchValidationService(IFraisBancaireValidationService fraisBancaireValidationService,
                                   IRestitutionValidationService restitutionValidationService,
                                   IOperationErroneValidationService operationErroneValidationService,
                                   IImpayeeValidationService impayeeValidationService,
                                   IAgiosValidationService agiosValidationService,
                                   List<BatchValidationHandler> batchValidationHandlers,
                                   IChequeValidationService chequeValidationService,
                                   ILcnValidationService lcnValidationService,
                                   IVirementValidationService virementValidationService,
                                   IVirementInternationalValidationService virementInternationalValidationService,
                                   IMiseADispositionValidationService miseADispositionValidationService,
                                   IPrelevementValidationService prelevementValidationService,
                                   IPrefinancementValidationService prefinancementValidationService,
                                   IFactoringValidationService factoringValidationService,
                                   IDroitConstateValidationService droitConstateValidationService,
                                   IDecompteValidationService decompteValidationService,
                                   IImpotEtTaxeValidationService impotEtTaxeValidationService,
                                   ICommissionValidationService commissionValidationService,
                                   IProvisionValidationService provisionValidationService) {
        this.fraisBancaireValidationService = fraisBancaireValidationService;
        this.restitutionValidationService = restitutionValidationService;
        this.operationErroneValidationService = operationErroneValidationService;
        this.impayeeValidationService = impayeeValidationService;
        this.agiosValidationService = agiosValidationService;
        this.handlerRegistry = batchValidationHandlers.stream()
                .collect(Collectors.toMap(BatchValidationHandler::source, h -> h, (a, b) -> {
                    throw new IllegalStateException(
                            "Deux BatchValidationHandler enregistrés pour la même source: " + a.source());
                }));
        this.chequeValidationService = chequeValidationService;
        this.lcnValidationService = lcnValidationService;
        this.virementValidationService = virementValidationService;
        this.virementInternationalValidationService = virementInternationalValidationService;
        this.miseADispositionValidationService = miseADispositionValidationService;
        this.prelevementValidationService = prelevementValidationService;
        this.prefinancementValidationService = prefinancementValidationService;
        this.factoringValidationService = factoringValidationService;
        this.droitConstateValidationService = droitConstateValidationService;
        this.decompteValidationService = decompteValidationService;
        this.impotEtTaxeValidationService = impotEtTaxeValidationService;
        this.commissionValidationService = commissionValidationService;
        this.provisionValidationService = provisionValidationService;
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
        if (handler != null) {
            return handler.processOne(id, requestDto);
        }

        switch (requestDto.transactionSource()) {
            case Virement -> {
                VirementTransactionRequestDto dto = new VirementTransactionRequestDto();
                dto.setId(id);
                copyChampsCommuns(requestDto, dto::setDateValeur, dto::setDateOperation,
                        dto::setTauxTva, dto::setRapprochementId);
                VirementTransactionResponseDto r = requestDto.sens() == SensOperation.DECAISSEMENT
                        ? virementValidationService.processVirementDecaissementTransaction(dto)
                        : virementValidationService.processVirementEncaissementTransaction(dto);
                return new OperationValideeDto(id, r.getId(), r.getLibelle(), r.getDebit(), r.getCredit());
            }
            case CHEQUE -> {
                ChequeTransactionRequest dto = new ChequeTransactionRequest();
                dto.setIdCheque(id);
                copyChampsCommuns(requestDto, dto::setDateValeur, dto::setDateOperation,
                        dto::setTauxTva, dto::setRapprochementId);
                ChequeTransactionResponseDto r = requestDto.sens() == SensOperation.DECAISSEMENT
                        ? chequeValidationService.processChequeDecaissementTransaction(dto)
                        : chequeValidationService.processChequeEncaissementTransaction(dto);
                return new OperationValideeDto(id, r.getId(), r.getLibelle(), r.getDebit(), r.getCredit());
            }
            case LCN_DECAISSEMENT -> {
                LcnTransactionRequest dto = new LcnTransactionRequest();
                dto.setId(id);
                copyChampsCommuns(requestDto, dto::setDateValeur, dto::setDateOperation,
                        dto::setTauxTva, dto::setRapprochementId);
                LcnTransactionResponseDto r = lcnValidationService.processLcnDecaissementTransaction(dto);
                return new OperationValideeDto(id, r.getId(), r.getLibelle(), r.getDebit(), r.getCredit());
            }
            case LCN_ENCAISSEMENT -> {
                LcnTransactionRequest dto = new LcnTransactionRequest();
                dto.setId(id);
                copyChampsCommuns(requestDto, dto::setDateValeur, dto::setDateOperation,
                        dto::setTauxTva, dto::setRapprochementId);
                LcnTransactionResponseDto r = lcnValidationService.processLcnEncaissementTransaction(dto);
                return new OperationValideeDto(id, r.getId(), r.getLibelle(), r.getDebit(), r.getCredit());
            }
            case MiseADisposition -> {
                MiseAdispositionTransactionRequestDto dto = new MiseAdispositionTransactionRequestDto();
                dto.setId(id);
                copyChampsCommuns(requestDto, dto::setDateValeur, dto::setDateOperation,
                        dto::setTauxTva, dto::setRapprochementId);
                MiseAdispositionTransactionResponseDto r = requestDto.sens() == SensOperation.DECAISSEMENT
                        ? miseADispositionValidationService.processMiseAdispositionDecaissementTransaction(dto)
                        : miseADispositionValidationService.processMiseAdispositionEncaissementTransaction(dto);
                return new OperationValideeDto(id, r.getId(), r.getLibelle(), r.getDebit(), r.getCredit());
            }
            case VIREMENT_INTERNATIONAL -> {
                VirementInternationalTransactionRequestDto dto = new VirementInternationalTransactionRequestDto();
                dto.setId(id);
                dto.setDateValeur(requestDto.dateValeur());
                dto.setDateOperation(requestDto.dateOperation());
                dto.setTauxTva(requestDto.tauxTva());
                dto.setRapprochementId(requestDto.rapprochementId());
                VirementInternationalTransactionResponseDto r = requestDto.sens() == SensOperation.DECAISSEMENT
                        ? virementInternationalValidationService.processVirementInternationalDecaissementTransaction(dto)
                        : virementInternationalValidationService.processVirementInternationalEncaissementTransaction(dto);
                return new OperationValideeDto(id, r.getId(), r.getLibelle(), r.getDebit(), r.getCredit());
            }
            case COMMISSION -> {
                CommissionTransactionRequestDto dto = new CommissionTransactionRequestDto(
                        id, requestDto.tauxTva(), null, null,
                        requestDto.dateValeur(), requestDto.dateOperation(),
                        null, null, requestDto.rapprochementId());
                CommissionTransactionResponseDto r = commissionValidationService.processCommissionTransaction(dto);
                return new OperationValideeDto(id, r.id(), r.libelle(), r.debit(), r.credit());
            }
            case AGIOS -> {
                AgiosValidationRequest dto = new AgiosValidationRequest(
                        id,
                        requestDto.tauxTva(),
                        null,
                        null,
                        requestDto.dateValeur(),
                        requestDto.dateOperation(),
                        null,
                        null,
                        requestDto.rapprochementId());
                AgiosTransactionResponse r = agiosValidationService.processAgiosToTransaction(dto);
                return new OperationValideeDto(id, r.id(), r.libelle(), r.debit(), r.credit());
            }
            case PRELEVEMENT -> {
                PrelevementValidationRequest dto = new PrelevementValidationRequest();
                dto.setIdPrelevement(id);
                dto.setDateValeur(requestDto.dateValeur());
                dto.setDateOperation(requestDto.dateOperation());
                dto.setTauxTva(requestDto.tauxTva());
                dto.setRapprochementId(requestDto.rapprochementId());
                PrelevementTransactionResponse r = prelevementValidationService.processPrelevementToTransaction(dto);
                return new OperationValideeDto(id, r.getId(), r.getLibelle(), r.getDebit(), r.getCredit());
            }
            case FRAIS_BANCAIRE -> {
                FraisBancaireTransactionRequestDto dto = new FraisBancaireTransactionRequestDto(
                        id, requestDto.tauxTva(), null, null,
                        requestDto.dateValeur(), requestDto.dateOperation(),
                        null, null, requestDto.rapprochementId());
                FraisBancaireTransactionResponseDto r = fraisBancaireValidationService.processFraisBancaireTransaction(dto);
                return new OperationValideeDto(id, r.id(), r.libelle(), r.debit(), r.credit());
            }
            case IMPOT_TAXE -> {
                ImpotEtTaxeTransactionRequestDto dto = new ImpotEtTaxeTransactionRequestDto(
                        id, requestDto.tauxTva(), null, null,
                        requestDto.dateValeur(), requestDto.dateOperation(),
                        null, null, requestDto.rapprochementId());
                ImpotEtTaxeTransactionResponseDto r = impotEtTaxeValidationService.processImpotEtTaxeTransaction(dto);
                return new OperationValideeDto(id, r.id(), r.libelle(), r.debit(), r.credit());
            }
            case RESTITUTION -> {
                RestitutionTransactionRequestDto dto = new RestitutionTransactionRequestDto(
                        id, requestDto.tauxTva(), null, null,
                        requestDto.dateValeur(), requestDto.dateOperation(),
                        null, null, requestDto.rapprochementId());
                RestitutionTransactionResponseDto r = restitutionValidationService.processRestitutionTransaction(dto);
                return new OperationValideeDto(id, r.id(), r.libelle(), r.debit(), r.credit());
            }
            case OPERATION_ERRONE -> {
                OperationErroneTransactionRequestDto dto = new OperationErroneTransactionRequestDto(
                        id, requestDto.tauxTva(), null, null,
                        requestDto.dateValeur(), requestDto.dateOperation(),
                        null, null, requestDto.rapprochementId());
                OperationErroneTransactionResponseDto r = operationErroneValidationService.processOperationErroneTransaction(dto);
                return new OperationValideeDto(id, r.id(), r.libelle(), r.debit(), r.credit());
            }
            case PROVISION -> {
                ProvisionTransactionRequestDto dto = new ProvisionTransactionRequestDto(
                        id, requestDto.tauxTva(), null, null,
                        requestDto.dateValeur(), requestDto.dateOperation(),
                        null, null, requestDto.rapprochementId());
                ProvisionTransactionResponseDto r = provisionValidationService.processProvisionTransaction(dto);
                return new OperationValideeDto(id, r.id(), r.libelle(), r.debit(), r.credit());
            }
            case FACTORING -> {
                FactoringTransactionRequest dto = new FactoringTransactionRequest();
                dto.setFactoringId(id);
                dto.setBanqueId(requestDto.banqueId());
                dto.setDateValeur(requestDto.dateValeur());
                dto.setDateOperation(requestDto.dateOperation());
                dto.setTauxTva(requestDto.tauxTva());
                dto.setRapprochementId(requestDto.rapprochementId());
                FactoringTransactionResponse r = factoringValidationService.processFactoringTransaction(dto);
                return new OperationValideeDto(id, r.getId(), r.getLibelle(), r.getDebit(), r.getCredit());
            }
            case PREFINANCEMENT -> {
                PrefinancementTransactionRequest dto = new PrefinancementTransactionRequest();
                dto.setPrefinancementId(id);
                dto.setBanqueId(requestDto.banqueId());
                dto.setDateValeur(requestDto.dateValeur());
                dto.setDateOperation(requestDto.dateOperation());
                dto.setTauxTva(requestDto.tauxTva());
                dto.setRapprochementId(requestDto.rapprochementId());
                PrefinancementTransactionResponse r = prefinancementValidationService.processPrefinancementTransaction(dto);
                return new OperationValideeDto(id, r.getId(), r.getLibelle(), r.getDebit(), r.getCredit());
            }
            case DROIT_CONSTATE -> {
                DroitConstateTransactionRequest dto = new DroitConstateTransactionRequest();
                dto.setDroitConstateId(id);
                dto.setBanqueId(requestDto.banqueId());
                dto.setDateValeur(requestDto.dateValeur());
                dto.setDateOperation(requestDto.dateOperation());
                dto.setTauxTva(requestDto.tauxTva());
                dto.setRapprochementId(requestDto.rapprochementId());
                DroitConstateTransactionResponse r = droitConstateValidationService.processDroitConstateTransaction(dto);
                return new OperationValideeDto(id, r.getId(), r.getLibelle(), r.getDebit(), r.getCredit());
            }
            case DECOMPTES -> {
                DecompteTransactionRequestDto dto = new DecompteTransactionRequestDto(
                        id,
                        requestDto.banqueId(),
                        null,
                        requestDto.dateValeur(),
                        requestDto.dateOperation(),
                        requestDto.tauxTva(),
                        null,
                        null,
                        null,
                        requestDto.rapprochementId());
                DecompteTransactionResponseDto r = decompteValidationService.processDecompteTransaction(dto);
                return new OperationValideeDto(id, r.id(), r.libelle(), r.debit(), r.credit());
            }
            case IMPAYEE -> {
                ImpayeeTransactionRequestDto dto = new ImpayeeTransactionRequestDto(
                        id, requestDto.tauxTva(), null, null,
                        requestDto.dateValeur(), requestDto.dateOperation(),
                        null, null, requestDto.rapprochementId());
                ImpayeeTransactionResponseDto r = impayeeValidationService.processImpayeeTransaction(dto);
                return new OperationValideeDto(id, r.id(), r.libelle(), r.debit(), r.credit());
            }
            default -> throw new ValidationException(
                    "La validation en lot n'est pas prise en charge pour le type d'opération: "
                            + requestDto.transactionSource());
        }
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
     * Applique les champs communs (date opération, date valeur, taux de TVA, rapprochementId) à un
     * DTO unitaire via ses setters, pour les types dont le champ de taux de TVA est un {@code Double}
     * et qui n'ont pas de champ banqueId. Passage par un {@code Consumer} plutôt que par une
     * interface commune : les DTOs unitaires n'ont pas de type commun.
     * <p>
     * Ne touche volontairement pas à dateEmission/observation/libelle : ces champs restent {@code null}
     * sur le DTO unitaire, ce qui déclenche le fallback sur l'entité source dans le mapper
     * correspondant (voir {@link BatchValidationRequestDto}). Ne touche pas non plus à montantTva
     * (saisie libre) : le mode lot ne transporte jamais qu'un taux — voir {@link BatchValidationRequestDto}.
     */
    private void copyChampsCommuns(BatchValidationRequestDto source,
                                    java.util.function.Consumer<java.time.LocalDate> setDateValeur,
                                    java.util.function.Consumer<java.time.LocalDate> setDateOperation,
                                    java.util.function.Consumer<Double> setTauxTva,
                                    java.util.function.Consumer<Long> setRapprochementId) {
        setDateValeur.accept(source.dateValeur());
        setDateOperation.accept(source.dateOperation());
        setTauxTva.accept(source.tauxTva());
        setRapprochementId.accept(source.rapprochementId());
    }
}
