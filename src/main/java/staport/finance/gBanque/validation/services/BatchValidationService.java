package staport.finance.gBanque.validation.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Agios.dtos.AgiosTransactionResponse;
import staport.finance.gBanque.Agios.dtos.AgiosValidationRequest;
import staport.finance.gBanque.Agios.service.interfaces.IAgiosValidationService;
import staport.finance.gBanque.cmt.echeances.dtos.CmtTransactionRequestDto;
import staport.finance.gBanque.cmt.echeances.dtos.CmtTransactionResponseDto;
import staport.finance.gBanque.cmt.echeances.entities.EcheanceCMT;
import staport.finance.gBanque.cmt.echeances.repositories.EcheanceCMTRepository;
import staport.finance.gBanque.cmt.echeances.services.interfaces.ICmtValidationService;
import staport.finance.gBanque.cmt.entities.CreditMoyenTerme;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.dto.LeasingTransactionRequestDTO;
import staport.finance.gBanque.leasing.echeancesLeasing.dto.LeasingTransactionResponseDTO;
import staport.finance.gBanque.leasing.echeancesLeasing.entities.EcheanceLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.repositories.EcheanceLeasingRepository;
import staport.finance.gBanque.leasing.echeancesLeasing.services.interfaces.ILeasingValidationService;
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
import staport.finance.gBanque.validation.dtos.fraisBancaire.FraisBancaireTransactionResponse;
import staport.finance.gBanque.validation.dtos.fraisBancaire.FraisBanciareValidationRequest;
import staport.finance.gBanque.validation.dtos.impayee.ImpayeeTransactionResponse;
import staport.finance.gBanque.validation.dtos.impayee.ImpayeeValidationRequest;
import staport.finance.gBanque.impot_taxes.validation.dto.ImpotEtTaxeTransactionRequestDto;
import staport.finance.gBanque.impot_taxes.validation.dto.ImpotEtTaxeTransactionResponseDto;
import staport.finance.gBanque.impot_taxes.validation.services.interfaces.IImpotEtTaxeValidationService;
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
import staport.finance.gBanque.validation.dtos.restitutions.RestitutionTransactionResponse;
import staport.finance.gBanque.validation.dtos.restitutions.RestitutionValidationRequest;
import staport.finance.gBanque.validation.dtos.operationErrone.OperationErroneTransactionResponse;
import staport.finance.gBanque.validation.dtos.operationErrone.OperationErroneValidationRequest;
import staport.finance.gBanque.moyensDePaiements.virementInternational.validation.dto.VirementInternationalTransactionRequestDto;
import staport.finance.gBanque.moyensDePaiements.virementInternational.validation.dto.VirementInternationalTransactionResponseDto;
import staport.finance.gBanque.moyensDePaiements.virementInternational.validation.services.interfaces.IVirementInternationalValidationService;
import staport.finance.gBanque.moyensDePaiements.virements.validation.dto.VirementTransactionRequestDto;
import staport.finance.gBanque.moyensDePaiements.virements.validation.dto.VirementTransactionResponseDto;
import staport.finance.gBanque.moyensDePaiements.virements.validation.services.interfaces.IVirementValidationService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrateur de validation (rapprochement) en lot.
 * <p>
 * Applique les mêmes champs communs (TVA, date opération, date valeur, rapprochement cible) à un
 * ensemble d'opérations de même {@link TransactionSource} (et même {@link SensOperation} pour les
 * types qui distinguent décaissement/encaissement), en réutilisant telles quelles les méthodes
 * unitaires existantes de {@link ValidationService} / {@link IAgiosValidationService} / {@link ICmtValidationService}
 * / {@link ILeasingValidationService} — aucune logique métier n'est dupliquée ici, seul le dispatch
 * et l'agrégation du résultat le sont.
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
 * transmet qu'une liste d'IDs génériques ({@link BatchValidationRequestDto#getIds()}).
 * <p>
 * Hors périmètre : CAUTION (géré par le module {@code cautions}, ne transite pas par ce contrôleur).
 */
@Service
public class BatchValidationService {

    private final ValidationService validationService;
    private final IAgiosValidationService agiosValidationService;
    private final EcheanceCMTRepository echeanceCMTRepository;
    private final ICmtValidationService cmtValidationService;
    private final EcheanceLeasingRepository echeanceLeasingRepository;
    private final ILeasingValidationService leasingValidationService;
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

    public BatchValidationService(ValidationService validationService,
                                   IAgiosValidationService agiosValidationService,
                                   EcheanceCMTRepository echeanceCMTRepository,
                                   ICmtValidationService cmtValidationService,
                                   EcheanceLeasingRepository echeanceLeasingRepository,
                                   ILeasingValidationService leasingValidationService,
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
                                   ICommissionValidationService commissionValidationService) {
        this.validationService = validationService;
        this.agiosValidationService = agiosValidationService;
        this.echeanceCMTRepository = echeanceCMTRepository;
        this.cmtValidationService = cmtValidationService;
        this.echeanceLeasingRepository = echeanceLeasingRepository;
        this.leasingValidationService = leasingValidationService;
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
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BatchValidationResponseDto processBatch(BatchValidationRequestDto requestDto) throws Exception {
        requireSensIfNeeded(requestDto);
        requireBanqueIdIfNeeded(requestDto);

        List<OperationValideeDto> resultats = new ArrayList<>();
        for (Long id : requestDto.getIds()) {
            resultats.add(processOne(id, requestDto));
        }

        BigDecimal totalDebit = resultats.stream().map(OperationValideeDto::getDebit)
                .filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = resultats.stream().map(OperationValideeDto::getCredit)
                .filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new BatchValidationResponseDto(resultats, totalDebit, totalCredit);
    }

    private OperationValideeDto processOne(Long id, BatchValidationRequestDto requestDto) throws Exception {
        switch (requestDto.getTransactionSource()) {
            case Virement -> {
                VirementTransactionRequestDto dto = new VirementTransactionRequestDto();
                dto.setId(id);
                copyChampsCommuns(requestDto, dto::setDateValeur, dto::setDateOperation,
                        dto::setTvaDeductible, dto::setRapprochementId);
                VirementTransactionResponseDto r = requestDto.getSens() == SensOperation.DECAISSEMENT
                        ? virementValidationService.processVirementDecaissementTransaction(dto)
                        : virementValidationService.processVirementEncaissementTransaction(dto);
                return new OperationValideeDto(id, r.getId(), r.getLibelle(), r.getDebit(), r.getCredit());
            }
            case CHEQUE -> {
                ChequeTransactionRequest dto = new ChequeTransactionRequest();
                dto.setIdCheque(id);
                copyChampsCommuns(requestDto, dto::setDateValeur, dto::setDateOperation,
                        dto::setTvaDeductible, dto::setRapprochementId);
                ChequeTransactionResponseDto r = requestDto.getSens() == SensOperation.DECAISSEMENT
                        ? chequeValidationService.processChequeDecaissementTransaction(dto)
                        : chequeValidationService.processChequeEncaissementTransaction(dto);
                return new OperationValideeDto(id, r.getId(), r.getLibelle(), r.getDebit(), r.getCredit());
            }
            case LCN_DECAISSEMENT -> {
                LcnTransactionRequest dto = new LcnTransactionRequest();
                dto.setId(id);
                copyChampsCommuns(requestDto, dto::setDateValeur, dto::setDateOperation,
                        dto::setTvaDeductible, dto::setRapprochementId);
                LcnTransactionResponseDto r = lcnValidationService.processLcnDecaissementTransaction(dto);
                return new OperationValideeDto(id, r.getId(), r.getLibelle(), r.getDebit(), r.getCredit());
            }
            case LCN_ENCAISSEMENT -> {
                LcnTransactionRequest dto = new LcnTransactionRequest();
                dto.setId(id);
                copyChampsCommuns(requestDto, dto::setDateValeur, dto::setDateOperation,
                        dto::setTvaDeductible, dto::setRapprochementId);
                LcnTransactionResponseDto r = lcnValidationService.processLcnEncaissementTransaction(dto);
                return new OperationValideeDto(id, r.getId(), r.getLibelle(), r.getDebit(), r.getCredit());
            }
            case MiseADisposition -> {
                MiseAdispositionTransactionRequestDto dto = new MiseAdispositionTransactionRequestDto();
                dto.setId(id);
                copyChampsCommuns(requestDto, dto::setDateValeur, dto::setDateOperation,
                        dto::setTvaDeductible, dto::setRapprochementId);
                MiseAdispositionTransactionResponseDto r = requestDto.getSens() == SensOperation.DECAISSEMENT
                        ? miseADispositionValidationService.processMiseAdispositionDecaissementTransaction(dto)
                        : miseADispositionValidationService.processMiseAdispositionEncaissementTransaction(dto);
                return new OperationValideeDto(id, r.getId(), r.getLibelle(), r.getDebit(), r.getCredit());
            }
            case VIREMENT_INTERNATIONAL -> {
                VirementInternationalTransactionRequestDto dto = new VirementInternationalTransactionRequestDto();
                dto.setId(id);
                dto.setDateValeur(requestDto.getDateValeur());
                dto.setDateOperation(requestDto.getDateOperation());
                dto.setTva(requestDto.getTvaDeductible());
                dto.setRapprochementId(requestDto.getRapprochementId());
                VirementInternationalTransactionResponseDto r = requestDto.getSens() == SensOperation.DECAISSEMENT
                        ? virementInternationalValidationService.processVirementInternationalDecaissementTransaction(dto)
                        : virementInternationalValidationService.processVirementInternationalEncaissementTransaction(dto);
                return new OperationValideeDto(id, r.getId(), r.getLibelle(), r.getDebit(), r.getCredit());
            }
            case COMMISSION -> {
                CommissionTransactionRequestDto dto = new CommissionTransactionRequestDto(
                        id, requestDto.getTvaDeductible(), null,
                        requestDto.getDateValeur(), requestDto.getDateOperation(),
                        null, null, requestDto.getRapprochementId());
                CommissionTransactionResponseDto r = commissionValidationService.processCommissionTransaction(dto);
                return new OperationValideeDto(id, r.id(), r.libelle(), r.debit(), r.credit());
            }
            case AGIOS -> {
                AgiosValidationRequest dto = new AgiosValidationRequest(
                        id,
                        BigDecimal.valueOf(requestDto.getTvaDeductible()),
                        null,
                        requestDto.getDateValeur(),
                        requestDto.getDateOperation(),
                        null,
                        null,
                        requestDto.getRapprochementId());
                AgiosTransactionResponse r = agiosValidationService.processAgiosToTransaction(dto);
                return new OperationValideeDto(id, r.id(), r.libelle(), r.debit(), r.credit());
            }
            case PRELEVEMENT -> {
                PrelevementValidationRequest dto = new PrelevementValidationRequest();
                dto.setIdPrelevement(id);
                dto.setDateValeur(requestDto.getDateValeur());
                dto.setDateOperation(requestDto.getDateOperation());
                dto.setTva(requestDto.getTvaDeductible());
                dto.setRapprochementId(requestDto.getRapprochementId());
                PrelevementTransactionResponse r = prelevementValidationService.processPrelevementToTransaction(dto);
                return new OperationValideeDto(id, r.getId(), r.getLibelle(), r.getDebit(), r.getCredit());
            }
            case FRAIS_BANCAIRE -> {
                FraisBanciareValidationRequest dto = new FraisBanciareValidationRequest();
                dto.setIdFraisBancaire(id);
                dto.setDateValeur(requestDto.getDateValeur());
                dto.setDateOperation(requestDto.getDateOperation());
                dto.setTva(requestDto.getTvaDeductible());
                dto.setRapprochementId(requestDto.getRapprochementId());
                FraisBancaireTransactionResponse r = validationService.processFraisBancaireToTransaction(dto);
                return new OperationValideeDto(id, r.getId(), r.getLibelle(), r.getDebit(), r.getCredit());
            }
            case IMPOT_TAXE -> {
                ImpotEtTaxeTransactionRequestDto dto = new ImpotEtTaxeTransactionRequestDto(
                        id, requestDto.getTvaDeductible(), null,
                        requestDto.getDateValeur(), requestDto.getDateOperation(),
                        null, null, requestDto.getRapprochementId());
                ImpotEtTaxeTransactionResponseDto r = impotEtTaxeValidationService.processImpotEtTaxeTransaction(dto);
                return new OperationValideeDto(id, r.id(), r.libelle(), r.debit(), r.credit());
            }
            case RESTITUTION -> {
                RestitutionValidationRequest dto = new RestitutionValidationRequest();
                dto.setIdRestitution(id);
                dto.setDateValeur(requestDto.getDateValeur());
                dto.setDateOperation(requestDto.getDateOperation());
                dto.setTva(requestDto.getTvaDeductible());
                dto.setRapprochementId(requestDto.getRapprochementId());
                RestitutionTransactionResponse r = validationService.processRestitutionToTransaction(dto);
                return new OperationValideeDto(id, r.getId(), r.getLibelle(), r.getDebit(), r.getCredit());
            }
            case OPERATION_ERRONE -> {
                OperationErroneValidationRequest dto = new OperationErroneValidationRequest();
                dto.setIdOperationErrone(id);
                dto.setDateValeur(requestDto.getDateValeur());
                dto.setDateOperation(requestDto.getDateOperation());
                dto.setTva(requestDto.getTvaDeductible());
                dto.setRapprochementId(requestDto.getRapprochementId());
                OperationErroneTransactionResponse r = validationService.processOperationErroneToTransaction(dto);
                return new OperationValideeDto(id, r.getId(), r.getLibelle(), r.getDebit(), r.getCredit());
            }
            case FACTORING -> {
                FactoringTransactionRequest dto = new FactoringTransactionRequest();
                dto.setFactoringId(id);
                dto.setBanqueId(requestDto.getBanqueId());
                dto.setDateValeur(requestDto.getDateValeur());
                dto.setDateOperation(requestDto.getDateOperation());
                dto.setTva(requestDto.getTvaDeductible());
                dto.setRapprochementId(requestDto.getRapprochementId());
                FactoringTransactionResponse r = factoringValidationService.processFactoringTransaction(dto);
                return new OperationValideeDto(id, r.getId(), r.getLibelle(), r.getDebit(), r.getCredit());
            }
            case PREFINANCEMENT -> {
                PrefinancementTransactionRequest dto = new PrefinancementTransactionRequest();
                dto.setPrefinancementId(id);
                dto.setBanqueId(requestDto.getBanqueId());
                dto.setDateValeur(requestDto.getDateValeur());
                dto.setDateOperation(requestDto.getDateOperation());
                dto.setTva(requestDto.getTvaDeductible());
                dto.setRapprochementId(requestDto.getRapprochementId());
                PrefinancementTransactionResponse r = prefinancementValidationService.processPrefinancementTransaction(dto);
                return new OperationValideeDto(id, r.getId(), r.getLibelle(), r.getDebit(), r.getCredit());
            }
            case DROIT_CONSTATE -> {
                DroitConstateTransactionRequest dto = new DroitConstateTransactionRequest();
                dto.setDroitConstateId(id);
                dto.setBanqueId(requestDto.getBanqueId());
                dto.setDateValeur(requestDto.getDateValeur());
                dto.setDateOperation(requestDto.getDateOperation());
                dto.setTva(requestDto.getTvaDeductible());
                dto.setRapprochementId(requestDto.getRapprochementId());
                DroitConstateTransactionResponse r = droitConstateValidationService.processDroitConstateTransaction(dto);
                return new OperationValideeDto(id, r.getId(), r.getLibelle(), r.getDebit(), r.getCredit());
            }
            case DECOMPTES -> {
                DecompteTransactionRequestDto dto = new DecompteTransactionRequestDto();
                dto.setDecompteId(id);
                dto.setBanqueId(requestDto.getBanqueId());
                dto.setDateValeur(requestDto.getDateValeur());
                dto.setDateOperation(requestDto.getDateOperation());
                dto.setTva(requestDto.getTvaDeductible());
                dto.setRapprochementId(requestDto.getRapprochementId());
                DecompteTransactionResponseDto r = decompteValidationService.processDecompteTransaction(dto);
                return new OperationValideeDto(id, r.getId(), r.getLibelle(), r.getDebit(), r.getCredit());
            }
            case IMPAYEE -> {
                ImpayeeValidationRequest dto = new ImpayeeValidationRequest();
                dto.setIdImpayee(id);
                dto.setDateValeur(requestDto.getDateValeur());
                dto.setDateOperation(requestDto.getDateOperation());
                dto.setTva(requestDto.getTvaDeductible());
                dto.setRapprochementId(requestDto.getRapprochementId());
                ImpayeeTransactionResponse r = validationService.processImpayeeToTransaction(dto);
                return new OperationValideeDto(id, r.getId(), r.getLibelle(), r.getDebit(), r.getCredit());
            }
            case CMT -> {
                EcheanceCMT echeance = echeanceCMTRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Échéance CMT introuvable avec l'ID: " + id));
                CreditMoyenTerme creditMoyenTerme = echeance.getCreditMoyenTerme();
                CmtTransactionRequestDto dto = new CmtTransactionRequestDto(
                        creditMoyenTerme.getId(),
                        id,
                        null,
                        requestDto.getDateValeur(),
                        requestDto.getDateOperation(),
                        null,
                        requestDto.getTvaDeductible(),
                        null,
                        requestDto.getRapprochementId());
                CmtTransactionResponseDto r = cmtValidationService.processCmtTransaction(creditMoyenTerme, dto);
                return new OperationValideeDto(id, r.id(), r.libelle(), r.debit(), r.credit());
            }
            case LEASING -> {
                EcheanceLeasing echeance = echeanceLeasingRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Échéance de leasing introuvable avec l'ID: " + id));
                ContratLeasing contratLeasing = echeance.getContratLeasing();
                LeasingTransactionRequestDTO dto = new LeasingTransactionRequestDTO(
                        contratLeasing.getId(),
                        id,
                        null,
                        requestDto.getDateValeur(),
                        requestDto.getDateOperation(),
                        null,
                        requestDto.getTvaDeductible(),
                        null,
                        requestDto.getRapprochementId());
                LeasingTransactionResponseDTO r = leasingValidationService.processLeasingTransaction(contratLeasing, dto);
                return new OperationValideeDto(id, r.id(), r.libelle(), r.debit(), r.credit());
            }
            default -> throw new ValidationException(
                    "La validation en lot n'est pas prise en charge pour le type d'opération: "
                            + requestDto.getTransactionSource());
        }
    }

    /**
     * Types qui distinguent décaissement/encaissement : {@code sens} est obligatoire pour eux.
     */
    private void requireSensIfNeeded(BatchValidationRequestDto requestDto) {
        boolean sensRequis = switch (requestDto.getTransactionSource()) {
            case Virement, CHEQUE, MiseADisposition, VIREMENT_INTERNATIONAL -> true;
            default -> false;
        };
        if (sensRequis && requestDto.getSens() == null) {
            throw new ValidationException(
                    "Le sens (DECAISSEMENT/ENCAISSEMENT) est obligatoire pour le type d'opération: "
                            + requestDto.getTransactionSource());
        }
    }

    /**
     * Types dont l'endpoint unitaire exige explicitement un banqueId (non déduit de l'entité).
     */
    private void requireBanqueIdIfNeeded(BatchValidationRequestDto requestDto) {
        boolean banqueIdRequis = switch (requestDto.getTransactionSource()) {
            case DECOMPTES, DROIT_CONSTATE, FACTORING, PREFINANCEMENT -> true;
            default -> false;
        };
        if (banqueIdRequis && requestDto.getBanqueId() == null) {
            throw new ValidationException(
                    "banqueId est obligatoire pour le type d'opération: " + requestDto.getTransactionSource());
        }
    }

    /**
     * Applique les champs communs (date opération, date valeur, TVA, rapprochementId) à un DTO
     * unitaire via ses setters, pour les types dont le champ TVA s'appelle {@code tvaDeductible}
     * (Double) et qui n'ont pas de champ banqueId.
     * <p>
     * Ne touche volontairement pas à dateEmission/observation/libelle : ces champs restent {@code null}
     * sur le DTO unitaire, ce qui déclenche le fallback sur l'entité source dans le mapper
     * correspondant (voir {@link BatchValidationRequestDto}).
     */
    private void copyChampsCommuns(BatchValidationRequestDto source,
                                    java.util.function.Consumer<java.time.LocalDate> setDateValeur,
                                    java.util.function.Consumer<java.time.LocalDate> setDateOperation,
                                    java.util.function.Consumer<Double> setTvaDeductible,
                                    java.util.function.Consumer<Long> setRapprochementId) {
        setDateValeur.accept(source.getDateValeur());
        setDateOperation.accept(source.getDateOperation());
        setTvaDeductible.accept(source.getTvaDeductible());
        setRapprochementId.accept(source.getRapprochementId());
    }
}
