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
import staport.finance.gBanque.transactions.enums.SensOperation;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.dtos.cheques.ChequeTransactionRequest;
import staport.finance.gBanque.validation.dtos.cheques.ChequeTransactionResponseDto;
import staport.finance.gBanque.validation.dtos.commision.CommissionTransactionResponse;
import staport.finance.gBanque.validation.dtos.commision.CommissionValidationDto;
import staport.finance.gBanque.validation.dtos.decomptes.DecompteTransactionRequestDto;
import staport.finance.gBanque.validation.dtos.decomptes.DecompteTransactionResponseDto;
import staport.finance.gBanque.validation.dtos.droitConstate.DroitConstateTransactionRequest;
import staport.finance.gBanque.validation.dtos.droitConstate.DroitConstateTransactionResponse;
import staport.finance.gBanque.validation.dtos.factoring.FactoringTransactionRequest;
import staport.finance.gBanque.validation.dtos.factoring.FactoringTransactionResponse;
import staport.finance.gBanque.validation.dtos.fraisBancaire.FraisBancaireTransactionResponse;
import staport.finance.gBanque.validation.dtos.fraisBancaire.FraisBanciareValidationRequest;
import staport.finance.gBanque.validation.dtos.impayee.ImpayeeTransactionResponse;
import staport.finance.gBanque.validation.dtos.impayee.ImpayeeValidationRequest;
import staport.finance.gBanque.validation.dtos.impotsEtTaxes.ImpotEtTaxeTransactionResponse;
import staport.finance.gBanque.validation.dtos.impotsEtTaxes.ImpotEtTaxeValidationRequest;
import staport.finance.gBanque.validation.dtos.lcn.LcnTransactionRequest;
import staport.finance.gBanque.validation.dtos.lcn.LcnTransactionResponse;
import staport.finance.gBanque.validation.dtos.lot.BatchValidationRequestDto;
import staport.finance.gBanque.validation.dtos.lot.BatchValidationResponseDto;
import staport.finance.gBanque.validation.dtos.lot.OperationValideeDto;
import staport.finance.gBanque.validation.dtos.miseAdispo.MiseAdispositionTransactionRequestDto;
import staport.finance.gBanque.validation.dtos.miseAdispo.MiseAdispositionTransactionResponseDto;
import staport.finance.gBanque.validation.dtos.prefinancements.PrefinancementTransactionRequest;
import staport.finance.gBanque.validation.dtos.prefinancements.PrefinancementTransactionResponse;
import staport.finance.gBanque.validation.dtos.prelevements.PrelevementTransactionResponse;
import staport.finance.gBanque.validation.dtos.prelevements.PrelevementValidationRequest;
import staport.finance.gBanque.validation.dtos.restitutions.RestitutionTransactionResponse;
import staport.finance.gBanque.validation.dtos.restitutions.RestitutionValidationRequest;
import staport.finance.gBanque.validation.dtos.operationErrone.OperationErroneTransactionResponse;
import staport.finance.gBanque.validation.dtos.operationErrone.OperationErroneValidationRequest;
import staport.finance.gBanque.validation.dtos.virementInternationaux.VirementInternationalTransactionRequestDto;
import staport.finance.gBanque.validation.dtos.virementInternationaux.VirementInternationalTransactionResponseDto;
import staport.finance.gBanque.validation.dtos.virements.VirementTransactionRequestDto;
import staport.finance.gBanque.validation.dtos.virements.VirementTransactionResponseDto;

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

    public BatchValidationService(ValidationService validationService,
                                   IAgiosValidationService agiosValidationService,
                                   EcheanceCMTRepository echeanceCMTRepository,
                                   ICmtValidationService cmtValidationService,
                                   EcheanceLeasingRepository echeanceLeasingRepository,
                                   ILeasingValidationService leasingValidationService) {
        this.validationService = validationService;
        this.agiosValidationService = agiosValidationService;
        this.echeanceCMTRepository = echeanceCMTRepository;
        this.cmtValidationService = cmtValidationService;
        this.echeanceLeasingRepository = echeanceLeasingRepository;
        this.leasingValidationService = leasingValidationService;
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
                        ? validationService.processVirementDecaissementTransaction(dto)
                        : validationService.processVirementEncaissementTransaction(dto);
                return new OperationValideeDto(id, r.getId(), r.getLibelle(), r.getDebit(), r.getCredit());
            }
            case CHEQUE -> {
                ChequeTransactionRequest dto = new ChequeTransactionRequest();
                dto.setIdCheque(id);
                copyChampsCommuns(requestDto, dto::setDateValeur, dto::setDateOperation,
                        dto::setTvaDeductible, dto::setRapprochementId);
                ChequeTransactionResponseDto r = requestDto.getSens() == SensOperation.DECAISSEMENT
                        ? validationService.processChequeDecaissementTransaction(dto)
                        : validationService.processChequeEncaissementTransaction(dto);
                return new OperationValideeDto(id, r.getId(), r.getLibelle(), r.getDebit(), r.getCredit());
            }
            case LCN_DECAISSEMENT -> {
                LcnTransactionRequest dto = new LcnTransactionRequest();
                dto.setId(id);
                copyChampsCommuns(requestDto, dto::setDateValeur, dto::setDateOperation,
                        dto::setTvaDeductible, dto::setRapprochementId);
                LcnTransactionResponse r = validationService.processDecaissementLcnTransaction(dto);
                return new OperationValideeDto(id, r.getId(), r.getLibelle(), r.getDebit(), r.getCredit());
            }
            case LCN_ENCAISSEMENT -> {
                LcnTransactionRequest dto = new LcnTransactionRequest();
                dto.setId(id);
                copyChampsCommuns(requestDto, dto::setDateValeur, dto::setDateOperation,
                        dto::setTvaDeductible, dto::setRapprochementId);
                LcnTransactionResponse r = validationService.processEncaissementLcnTransaction(dto);
                return new OperationValideeDto(id, r.getId(), r.getLibelle(), r.getDebit(), r.getCredit());
            }
            case MiseADisposition -> {
                MiseAdispositionTransactionRequestDto dto = new MiseAdispositionTransactionRequestDto();
                dto.setId(id);
                copyChampsCommuns(requestDto, dto::setDateValeur, dto::setDateOperation,
                        dto::setTvaDeductible, dto::setRapprochementId);
                MiseAdispositionTransactionResponseDto r = requestDto.getSens() == SensOperation.DECAISSEMENT
                        ? validationService.processMiseAdispositionDecaissementTransaction(dto)
                        : validationService.processMiseAdispositionEncaissementTransaction(dto);
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
                        ? validationService.processVirementInternationalDecaissementTransaction(dto)
                        : validationService.processVirementInternationalEncaissementTransaction(dto);
                return new OperationValideeDto(id, r.getId(), r.getLibelle(), r.getDebit(), r.getCredit());
            }
            case COMMISSION -> {
                CommissionValidationDto dto = new CommissionValidationDto();
                dto.setIdCommission(id);
                dto.setDateValeur(requestDto.getDateValeur());
                dto.setDateOperation(requestDto.getDateOperation());
                dto.setTva(requestDto.getTvaDeductible());
                dto.setRapprochementId(requestDto.getRapprochementId());
                CommissionTransactionResponse r = validationService.processCommissionToTransaction(dto);
                return new OperationValideeDto(id, r.getId(), r.getLibelle(), r.getDebit(), r.getCredit());
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
                PrelevementTransactionResponse r = validationService.processPrelevementToTransaction(dto);
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
                ImpotEtTaxeValidationRequest dto = new ImpotEtTaxeValidationRequest();
                dto.setIdImpotEtTaxe(id);
                dto.setDateValeur(requestDto.getDateValeur());
                dto.setDateOperation(requestDto.getDateOperation());
                dto.setTva(requestDto.getTvaDeductible());
                dto.setRapprochementId(requestDto.getRapprochementId());
                ImpotEtTaxeTransactionResponse r = validationService.processImpotEtTaxeToTransaction(dto);
                return new OperationValideeDto(id, r.getId(), r.getLibelle(), r.getDebit(), r.getCredit());
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
                FactoringTransactionResponse r = validationService.processFactoringTransaction(dto);
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
                PrefinancementTransactionResponse r = validationService.processPrefinancementTransaction(dto);
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
                DroitConstateTransactionResponse r = validationService.processDroitConstateTransaction(dto);
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
                DecompteTransactionResponseDto r = validationService.processDecompteToTransaction(dto);
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
