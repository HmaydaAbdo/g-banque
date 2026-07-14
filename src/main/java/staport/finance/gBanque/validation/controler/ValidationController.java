package staport.finance.gBanque.validation.controler;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import staport.finance.gBanque.Agios.dtos.AgiosTransactionResponse;
import staport.finance.gBanque.Agios.dtos.AgiosValidationRequest;
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
import staport.finance.gBanque.prefinancement.validation.dto.PrefinancementTransactionRequest;
import staport.finance.gBanque.prefinancement.validation.dto.PrefinancementTransactionResponse;
import staport.finance.gBanque.prefinancement.validation.services.interfaces.IPrefinancementValidationService;
import staport.finance.gBanque.moyensDePaiements.cheques.validation.dto.ChequeTransactionRequest;
import staport.finance.gBanque.moyensDePaiements.cheques.validation.dto.ChequeTransactionResponseDto;
import staport.finance.gBanque.moyensDePaiements.cheques.validation.services.interfaces.IChequeValidationService;
import staport.finance.gBanque.cmt.echeances.dtos.CmtTransactionRequestDto;
import staport.finance.gBanque.cmt.echeances.dtos.CmtTransactionResponseDto;
import staport.finance.gBanque.cmt.echeances.services.interfaces.ICmtValidationService;
import staport.finance.gBanque.cmt.entities.CreditMoyenTerme;
import staport.finance.gBanque.cmt.services.CreditMoyenTermeService;
import staport.finance.gBanque.moyensDePaiements.lcn.validation.dto.LcnTransactionRequest;
import staport.finance.gBanque.moyensDePaiements.lcn.validation.dto.LcnTransactionResponseDto;
import staport.finance.gBanque.moyensDePaiements.lcn.validation.services.interfaces.ILcnValidationService;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;
import staport.finance.gBanque.leasing.contratLeasing.service.ContratLeasingService;
import staport.finance.gBanque.leasing.echeancesLeasing.dto.LeasingTransactionRequestDTO;
import staport.finance.gBanque.leasing.echeancesLeasing.dto.LeasingTransactionResponseDTO;
import staport.finance.gBanque.Agios.service.interfaces.IAgiosValidationService;
import staport.finance.gBanque.leasing.echeancesLeasing.services.interfaces.ILeasingValidationService;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.validation.dto.MiseAdispositionTransactionRequestDto;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.validation.dto.MiseAdispositionTransactionResponseDto;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.validation.services.interfaces.IMiseADispositionValidationService;
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
import staport.finance.gBanque.validation.dtos.lot.BatchValidationRequestDto;
import staport.finance.gBanque.validation.dtos.lot.BatchValidationResponseDto;
import staport.finance.gBanque.validation.services.BatchValidationService;
import staport.finance.gBanque.validation.services.ReverseTransactionService;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/rapprochement")
@PreAuthorize(ValidationController.FINANCE_OR_ADMIN)
public class ValidationController {

    static final String FINANCE_OR_ADMIN =
            "hasAnyAuthority('DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR', 'SYSTEM_ADMIN')";

    private final IFraisBancaireValidationService fraisBancaireValidationService;
    private final IRestitutionValidationService restitutionValidationService;
    private final IOperationErroneValidationService operationErroneValidationService;
    private final IImpayeeValidationService impayeeValidationService;
    private final ReverseTransactionService reverseTransactionService;
    private final ILeasingValidationService leasingValidationService;
    private final IAgiosValidationService agiosValidationService;
    private final ContratLeasingService contratLeasingService;
    private final ICmtValidationService cmtValidationService;
    private final CreditMoyenTermeService creditMoyenTermeService;
    private final BatchValidationService batchValidationService;
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

    public ValidationController(IFraisBancaireValidationService fraisBancaireValidationService,
                                IRestitutionValidationService restitutionValidationService,
                                IOperationErroneValidationService operationErroneValidationService,
                                IImpayeeValidationService impayeeValidationService,
                                ReverseTransactionService reverseTransactionService,
                                ILeasingValidationService leasingValidationService,
                                IAgiosValidationService agiosValidationService,
                                ContratLeasingService contratLeasingService,
                                ICmtValidationService cmtValidationService,
                                CreditMoyenTermeService creditMoyenTermeService,
                                BatchValidationService batchValidationService,
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
        this.fraisBancaireValidationService = fraisBancaireValidationService;
        this.restitutionValidationService = restitutionValidationService;
        this.operationErroneValidationService = operationErroneValidationService;
        this.impayeeValidationService = impayeeValidationService;
        this.reverseTransactionService = reverseTransactionService;
        this.leasingValidationService  = leasingValidationService;
        this.agiosValidationService    = agiosValidationService;
        this.contratLeasingService    = contratLeasingService;
        this.cmtValidationService     = cmtValidationService;
        this.creditMoyenTermeService   = creditMoyenTermeService;
        this.batchValidationService    = batchValidationService;
        this.chequeValidationService   = chequeValidationService;
        this.lcnValidationService      = lcnValidationService;
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


    /**
     * Valide une échéance de leasing : crée la transaction bancaire et marque l'échéance PAYE.
     * Délègue au {@code LeasingValidationService} du module leasing.
     * <p>
     * Le {@code ContratLeasing} est résolu ici (via {@code ContratLeasingService}, le point d'entrée
     * public du domaine {@code contratLeasing}) et transmis directement à {@code LeasingValidationService}
     * pour éviter que ce dernier n'ait besoin d'injecter {@code ContratLeasingService} — une telle
     * dépendance fermerait un cycle de beans Spring (LeasingValidationService → ContratLeasingService
     * → ... → TransactionService → LeasingValidationService, ce dernier étant déjà appelé par
     * TransactionService pour {@code syncEcheanceFromTransaction}).
     */
    @PostMapping("/process/echeance-leasing")
    public ResponseEntity<LeasingTransactionResponseDTO> processLeasingTransaction(
            @Valid @RequestBody LeasingTransactionRequestDTO requestDto) {
        ContratLeasing contratLeasing = contratLeasingService.findOrThrow(requestDto.contratLeasingId());
        return new ResponseEntity<>(
                leasingValidationService.processLeasingTransaction(contratLeasing, requestDto), HttpStatus.CREATED);
    }


    /**
     * Valide une échéance CMT : crée la transaction bancaire et marque l'échéance PAYE.
     * Délègue au {@code CmtValidationService} du module cmt.
     * <p>
     * Le {@code CreditMoyenTerme} est résolu ici (via {@code CreditMoyenTermeService}, le point d'entrée
     * public du domaine {@code cmt}) et transmis directement à {@code CmtValidationService} pour éviter
     * que ce dernier n'ait besoin d'injecter {@code CreditMoyenTermeService} — même précaution que pour
     * le Leasing (cf. Javadoc de {@code processLeasingTransaction} ci-dessus).
     */
    @PostMapping("/process/echeance-cmt")
    public ResponseEntity<CmtTransactionResponseDto> processCmtTransaction(@Valid @RequestBody CmtTransactionRequestDto requestDto) {
        CreditMoyenTerme creditMoyenTerme = creditMoyenTermeService.findOrThrow(requestDto.creditMoyenTermeId());
        return new ResponseEntity<>(
                cmtValidationService.processCmtTransaction(creditMoyenTerme, requestDto), HttpStatus.CREATED);
    }


    /**
     * Valide une mise à disposition de décaissement : crée la transaction bancaire et marque la
     * mise à disposition REGLE. Délègue au {@code MiseADispositionValidationService} du module
     * miseADisposition (architecture.md, Règle 1).
     */
    @PostMapping("/process/decaissement/mise-a-disposition")
    public ResponseEntity<MiseAdispositionTransactionResponseDto> processMiseAdispositionDecaissementTransaction(@Valid @RequestBody MiseAdispositionTransactionRequestDto requestDto) {
        MiseAdispositionTransactionResponseDto responseDto = miseADispositionValidationService.processMiseAdispositionDecaissementTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    /**
     * Valide une mise à disposition d'encaissement : crée la transaction bancaire et marque la
     * mise à disposition REGLE. Délègue au {@code MiseADispositionValidationService} du module
     * miseADisposition (architecture.md, Règle 1).
     */
    @PostMapping("/process/encaissement/mise-a-disposition")
    public ResponseEntity<MiseAdispositionTransactionResponseDto> processMiseAdispositionEncaissementTransaction(@Valid @RequestBody MiseAdispositionTransactionRequestDto requestDto) {
        MiseAdispositionTransactionResponseDto responseDto = miseADispositionValidationService.processMiseAdispositionEncaissementTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }



    /**
     * Valide un virement de décaissement : crée la transaction bancaire et marque le virement REGLE.
     * Délègue au {@code VirementValidationService} du module virements (architecture.md, Règle 1).
     */
    @PostMapping("/process/decaissement/virement")
    public ResponseEntity<VirementTransactionResponseDto> processVirementDecaissementTransaction(@Valid @RequestBody VirementTransactionRequestDto requestDto) {
        VirementTransactionResponseDto responseDto = virementValidationService.processVirementDecaissementTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    /**
     * Valide un virement d'encaissement : crée la transaction bancaire et marque le virement REGLE.
     * Délègue au {@code VirementValidationService} du module virements (architecture.md, Règle 1).
     */
    @PostMapping("/process/encaissement/virement")
    public ResponseEntity<VirementTransactionResponseDto> processVirementEncaissementTransaction(@Valid @RequestBody VirementTransactionRequestDto requestDto) {
        VirementTransactionResponseDto responseDto = virementValidationService.processVirementEncaissementTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }


    /**
     * Valide un chèque de décaissement : crée la transaction bancaire et marque le chèque REGLE.
     * Délègue au {@code ChequeValidationService} du module cheques (architecture.md, Règle 1).
     */
    @PostMapping("/process/decaissement/cheque")
    public ResponseEntity<ChequeTransactionResponseDto> processDecaissementChequeTransaction(@Valid @RequestBody ChequeTransactionRequest requestDto) {
        ChequeTransactionResponseDto responseDto = chequeValidationService.processChequeDecaissementTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    /**
     * Valide un chèque d'encaissement : crée la transaction bancaire et marque le chèque REGLE.
     * Délègue au {@code ChequeValidationService} du module cheques (architecture.md, Règle 1).
     */
    @PostMapping("/process/encaissement/cheque")
    public ResponseEntity<ChequeTransactionResponseDto> processEncaissementChequeTransaction(@Valid @RequestBody ChequeTransactionRequest requestDto) {
        ChequeTransactionResponseDto responseDto = chequeValidationService.processChequeEncaissementTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    /**
     * Valide un LCN de décaissement : crée la transaction bancaire et marque le LCN REGLE.
     * Délègue au {@code LcnValidationService} du module lcn (architecture.md, Règle 1).
     */
    @PostMapping("/process/decaissement/lcn")
    public ResponseEntity<LcnTransactionResponseDto> processDecaissementLcnTransaction(@Valid @RequestBody LcnTransactionRequest requestDto) {
        LcnTransactionResponseDto responseDto = lcnValidationService.processLcnDecaissementTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    /**
     * Valide un LCN d'encaissement : crée la transaction bancaire et marque le LCN REGLE.
     * Délègue au {@code LcnValidationService} du module lcn (architecture.md, Règle 1).
     */
    @PostMapping("/process/encaissement/lcn")
    public ResponseEntity<LcnTransactionResponseDto> processEncaissementLcnTransaction(@Valid @RequestBody LcnTransactionRequest requestDto) {
        LcnTransactionResponseDto responseDto = lcnValidationService.processLcnEncaissementTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    /**
     * Endpoint pour traiter une transaction de rapprochement de prefinancement.
     * Prend en charge un DTO de requête et retourne un DTO de réponse avec les détails de la transaction créée.
     *
     * @param requestDto Le DTO de la requête de transaction de prefinancement.
     * @return Une ResponseEntity contenant le DTO de réponse de la transaction et un statut HTTP 201 Created.
     */
    @PostMapping("/process/prefinancement")
    public ResponseEntity<PrefinancementTransactionResponse> processPrefiTransaction(@Valid @RequestBody PrefinancementTransactionRequest requestDto) {
        PrefinancementTransactionResponse responseDto = prefinancementValidationService.processPrefinancementTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    /**
     * Endpoint pour traiter une transaction de rapprochement de droit constate.
     * Prend en charge un DTO de requête et retourne un DTO de réponse avec les détails de la transaction créée.
     *
     * @param requestDto Le DTO de la requête de transaction de droit constate.
     * @return Une ResponseEntity contenant le DTO de réponse de la transaction et un statut HTTP 201 Created.
     */
    @PostMapping("/process/droit-constate")
    public ResponseEntity<DroitConstateTransactionResponse> processDroitConstqteTransaction(@Valid @RequestBody DroitConstateTransactionRequest requestDto) {
        DroitConstateTransactionResponse responseDto = droitConstateValidationService.processDroitConstateTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @PostMapping("/process/factoring")
    public ResponseEntity<FactoringTransactionResponse> processFactoringTransaction(@Valid @RequestBody FactoringTransactionRequest requestDto) {
        FactoringTransactionResponse responseDto = factoringValidationService.processFactoringTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    /**
     * Endpoint pour traiter une transaction de rapprochement de décompte.
     * Prend en charge un DTO de requête et retourne un DTO de réponse avec les détails de la transaction créée.
     *
     * @param requestDto Le DTO de la requête de transaction de décompte.
     * @return Une ResponseEntity contenant le DTO de réponse de la transaction et un statut HTTP 201 Created.
     */
    @PostMapping("/process/decompte")
    public ResponseEntity<DecompteTransactionResponseDto> processDecompteToTransaction(@Valid @RequestBody DecompteTransactionRequestDto requestDto) {
        DecompteTransactionResponseDto responseDto = decompteValidationService.processDecompteTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    /**
     * Valide une commission : crée la transaction bancaire et marque la commission PAYE.
     * Délègue au {@code CommissionValidationService} du module commision.
     */
    @PostMapping("/process/commission")
    public ResponseEntity<CommissionTransactionResponseDto> processCommissionToTransaction(@Valid @RequestBody CommissionTransactionRequestDto requestDto) {
        CommissionTransactionResponseDto responseDto = commissionValidationService.processCommissionTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @PostMapping("/process/agios")
    public ResponseEntity<AgiosTransactionResponse> processAgiosToTransaction(@Valid @RequestBody AgiosValidationRequest requestDto) throws Exception {
        return new ResponseEntity<>(agiosValidationService.processAgiosToTransaction(requestDto), HttpStatus.CREATED);
    }

    @PostMapping("/process/prelevement")
    public ResponseEntity<PrelevementTransactionResponse> processPrelevementToTransaction(@Valid @RequestBody PrelevementValidationRequest requestDto) {
        PrelevementTransactionResponse responseDto = prelevementValidationService.processPrelevementToTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @PostMapping("/process/frais-bancaire")
    public ResponseEntity<FraisBancaireTransactionResponseDto> processFraisBancaireToTransaction(@Valid @RequestBody FraisBancaireTransactionRequestDto requestDto) {
        FraisBancaireTransactionResponseDto responseDto = fraisBancaireValidationService.processFraisBancaireTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }



    @PostMapping("/process/impot-taxe")
    public ResponseEntity<ImpotEtTaxeTransactionResponseDto> processImpotEtTaxeToTransaction(
            @Valid @RequestBody ImpotEtTaxeTransactionRequestDto requestDto) {
        ImpotEtTaxeTransactionResponseDto responseDto = impotEtTaxeValidationService.processImpotEtTaxeTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }


    @PostMapping("/process/restitution")
    public ResponseEntity<RestitutionTransactionResponseDto> processRestitutionToTransaction(
            @Valid @RequestBody RestitutionTransactionRequestDto requestDto) {
        RestitutionTransactionResponseDto responseDto = restitutionValidationService.processRestitutionTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @PostMapping("/process/operation-errone")
    public ResponseEntity<OperationErroneTransactionResponseDto> processOperationErroneToTransaction(
            @Valid @RequestBody OperationErroneTransactionRequestDto requestDto) {
        OperationErroneTransactionResponseDto responseDto = operationErroneValidationService.processOperationErroneTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    /**
     * Valide un virement international de décaissement : crée la transaction bancaire et marque le
     * virement international REGLE. Délègue au {@code VirementInternationalValidationService} du
     * module virementInternational (architecture.md, Règle 1).
     */
    @PostMapping("/process/decaissement/virement-international")
    public ResponseEntity<VirementInternationalTransactionResponseDto> processVirementInternationalDecaissementTransaction(
            @Valid @RequestBody VirementInternationalTransactionRequestDto requestDto) {
        VirementInternationalTransactionResponseDto responseDto =
                virementInternationalValidationService.processVirementInternationalDecaissementTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    /**
     * Valide un virement international d'encaissement : crée la transaction bancaire et marque le
     * virement international REGLE. Délègue au {@code VirementInternationalValidationService} du
     * module virementInternational (architecture.md, Règle 1).
     */
    @PostMapping("/process/encaissement/virement-international")
    public ResponseEntity<VirementInternationalTransactionResponseDto> processVirementInternationalEncaissementTransaction(
            @Valid @RequestBody VirementInternationalTransactionRequestDto requestDto) {
        VirementInternationalTransactionResponseDto responseDto =
                virementInternationalValidationService.processVirementInternationalEncaissementTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @PostMapping("/process/impayee")
    public ResponseEntity<ImpayeeTransactionResponseDto> processImpayeeToTransaction(
            @Valid @RequestBody ImpayeeTransactionRequestDto requestDto) {
        ImpayeeTransactionResponseDto responseDto = impayeeValidationService.processImpayeeTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    /**
     * Valide en une seule fois plusieurs opérations de même type (et même sens pour les types qui
     * distinguent décaissement/encaissement), en appliquant les mêmes champs communs à toutes.
     * Délègue à {@code BatchValidationService}, qui réutilise les méthodes unitaires ci-dessus —
     * voir sa Javadoc pour le détail du dispatch et le comportement tout-ou-rien.
     */
    @PostMapping("/process/lot")
    public ResponseEntity<BatchValidationResponseDto> processBatch(
            @Valid @RequestBody BatchValidationRequestDto requestDto) throws Exception {
        BatchValidationResponseDto responseDto = batchValidationService.processBatch(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @DeleteMapping("/annuler/{transactionId}")
    public void annulerTransaction(@PathVariable Long transactionId) throws  Exception{
         reverseTransactionService.annulerTransaction(transactionId);
    }





}
