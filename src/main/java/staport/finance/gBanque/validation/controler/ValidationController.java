package staport.finance.gBanque.validation.controler;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import staport.finance.gBanque.validation.dtos.agios.AgiosTransactionResponse;
import staport.finance.gBanque.validation.dtos.agios.AgiosValidationRequest;
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
import staport.finance.gBanque.validation.dtos.prefinancements.PrefinancementTransactionRequest;
import staport.finance.gBanque.validation.dtos.prefinancements.PrefinancementTransactionResponse;
import staport.finance.gBanque.validation.dtos.cheques.ChequeTransactionRequest;
import staport.finance.gBanque.validation.dtos.cheques.ChequeTransactionResponseDto;
import staport.finance.gBanque.validation.dtos.cmt.CmtTransactionRequestDto;
import staport.finance.gBanque.validation.dtos.cmt.CmtTransactionResponseDto;
import staport.finance.gBanque.validation.dtos.lcn.LcnTransactionRequest;
import staport.finance.gBanque.validation.dtos.lcn.LcnTransactionResponse;
import staport.finance.gBanque.validation.dtos.leasing.LeasingTransactionRequestDto;
import staport.finance.gBanque.validation.dtos.leasing.LeasingTransactionResponseDto;
import staport.finance.gBanque.validation.dtos.miseAdispo.MiseAdispositionTransactionRequestDto;
import staport.finance.gBanque.validation.dtos.miseAdispo.MiseAdispositionTransactionResponseDto;
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
import staport.finance.gBanque.validation.services.ReverseTransactionService;
import staport.finance.gBanque.validation.services.ValidationService;

@RestController
@RequestMapping("/rapprochement")
public class ValidationController {

    private final ValidationService validationService;
    private final ReverseTransactionService reverseTransactionService;

    public ValidationController(ValidationService validationService, ReverseTransactionService reverseTransactionService) {
        this.validationService = validationService;
        this.reverseTransactionService = reverseTransactionService;
    }


    /**
     * Endpoint pour traiter une transaction de rapprochement de leasing.
     * Prend en charge un DTO de requête et retourne un DTO de réponse avec les détails de la transaction créée.
     *
     * @param requestDto Le DTO de la requête de transaction de leasing.
     * @return Une ResponseEntity contenant le DTO de réponse de la transaction et un statut HTTP 201 Created.
     */
    @PostMapping("/process/echeance-leasing")
    public ResponseEntity<LeasingTransactionResponseDto> processLeasingTransaction(@Valid @RequestBody LeasingTransactionRequestDto requestDto) throws Exception {
        LeasingTransactionResponseDto responseDto = validationService.processLeasingTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }


    /**
     * Endpoint pour traiter une transaction de rapprochement de leasing.
     * Prend en charge un DTO de requête et retourne un DTO de réponse avec les détails de la transaction créée.
     *
     * @param requestDto Le DTO de la requête de transaction de leasing.
     * @return Une ResponseEntity contenant le DTO de réponse de la transaction et un statut HTTP 201 Created.
     */
    @PostMapping("/process/echeance-cmt")
    public ResponseEntity<CmtTransactionResponseDto> processCmtTransaction(@Valid @RequestBody CmtTransactionRequestDto requestDto) throws Exception {
        CmtTransactionResponseDto responseDto = validationService.processCmtTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }


    @PostMapping("/process/decaissement/mise-a-disposition")
    public ResponseEntity<MiseAdispositionTransactionResponseDto> processMiseAdispositionDecaissementTransaction(@Valid @RequestBody MiseAdispositionTransactionRequestDto requestDto) throws Exception {
        MiseAdispositionTransactionResponseDto responseDto = validationService.processMiseAdispositionDecaissementTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }


    @PostMapping("/process/encaissement/mise-a-disposition")
    public ResponseEntity<MiseAdispositionTransactionResponseDto> processMiseAdispositionEncaissementTransaction(@Valid @RequestBody MiseAdispositionTransactionRequestDto requestDto) throws Exception {
        MiseAdispositionTransactionResponseDto responseDto = validationService.processMiseAdispositionEncaissementTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }



    @PostMapping("/process/decaissement/virement")
    public ResponseEntity<VirementTransactionResponseDto> processVirementDecaissementTransaction(@Valid @RequestBody VirementTransactionRequestDto requestDto) throws Exception {
        VirementTransactionResponseDto responseDto = validationService.processVirementDecaissementTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }


    @PostMapping("/process/encaissement/virement")
    public ResponseEntity<VirementTransactionResponseDto> processVirementEncaissementTransaction(@Valid @RequestBody VirementTransactionRequestDto requestDto) throws Exception {
        VirementTransactionResponseDto responseDto = validationService.processVirementEncaissementTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }


    @PostMapping("/process/decaissement/cheque")
    public ResponseEntity<ChequeTransactionResponseDto> processDecaissementChequeTransaction(@Valid @RequestBody ChequeTransactionRequest requestDto) throws Exception {
        ChequeTransactionResponseDto responseDto = validationService.processChequeDecaissementTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @PostMapping("/process/encaissement/cheque")
    public ResponseEntity<ChequeTransactionResponseDto> processEncaissementChequeTransaction(@Valid @RequestBody ChequeTransactionRequest requestDto) throws Exception {
        ChequeTransactionResponseDto responseDto = validationService.processChequeEncaissementTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @PostMapping("/process/decaissement/lcn")
    public ResponseEntity<LcnTransactionResponse> processDecaissementLcnTransaction(@Valid @RequestBody LcnTransactionRequest requestDto) throws Exception {
        LcnTransactionResponse responseDto = validationService.processDecaissementLcnTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @PostMapping("/process/encaissement/lcn")
    public ResponseEntity<LcnTransactionResponse> processEncaissementLcnTransaction(@Valid @RequestBody LcnTransactionRequest requestDto) throws Exception {
        LcnTransactionResponse responseDto = validationService.processEncaissementLcnTransaction(requestDto);
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
    public ResponseEntity<PrefinancementTransactionResponse> processPrefiTransaction(@Valid @RequestBody PrefinancementTransactionRequest requestDto) throws Exception {
        PrefinancementTransactionResponse responseDto = validationService.processPrefinancementTransaction(requestDto);
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
    public ResponseEntity<DroitConstateTransactionResponse> processDroitConstqteTransaction(@Valid @RequestBody DroitConstateTransactionRequest requestDto) throws Exception {
        DroitConstateTransactionResponse responseDto = validationService.processDroitConstateTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @PostMapping("/process/factoring")
    public ResponseEntity<FactoringTransactionResponse> processFactoringTransaction(@Valid @RequestBody FactoringTransactionRequest requestDto) throws Exception {
        FactoringTransactionResponse responseDto = validationService.processFactoringTransaction(requestDto);
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
    public ResponseEntity<DecompteTransactionResponseDto> processDecompteToTransaction(@Valid @RequestBody DecompteTransactionRequestDto requestDto) throws Exception {
        DecompteTransactionResponseDto responseDto = validationService.processDecompteToTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    /**
     * Endpoint pour traiter une transaction de rapprochement de commission.
     * Prend en charge un DTO de requête et retourne un DTO de réponse avec les détails de la transaction créée.
     *
     * @param requestDto Le DTO de la requête de transaction de décompte.
     * @return Une ResponseEntity contenant le DTO de réponse de la transaction et un statut HTTP 201 Created.
     */
    @PostMapping("/process/commission")
    public ResponseEntity<CommissionTransactionResponse> processCommissionToTransaction(@Valid @RequestBody CommissionValidationDto requestDto) throws Exception {
        CommissionTransactionResponse responseDto = validationService.processCommissionToTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @PostMapping("/process/agios")
    public ResponseEntity<AgiosTransactionResponse> processAgiosToTransaction(@Valid @RequestBody AgiosValidationRequest requestDto) throws Exception {
        AgiosTransactionResponse responseDto = validationService.processAgiosToTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @PostMapping("/process/prelevement")
    public ResponseEntity<PrelevementTransactionResponse> processPrelevementToTransaction(@Valid @RequestBody PrelevementValidationRequest requestDto) throws Exception {
        PrelevementTransactionResponse responseDto = validationService.processPrelevementToTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @PostMapping("/process/frais-bancaire")
    public ResponseEntity<FraisBancaireTransactionResponse> processFraisBancaireToTransaction(@Valid @RequestBody FraisBanciareValidationRequest requestDto) throws Exception {
        FraisBancaireTransactionResponse responseDto = validationService.processFraisBancaireToTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }



    @PostMapping("/process/impot-taxe")
    public ResponseEntity<ImpotEtTaxeTransactionResponse> processImpotEtTaxeToTransaction(
            @Valid @RequestBody ImpotEtTaxeValidationRequest requestDto) throws Exception {
        ImpotEtTaxeTransactionResponse responseDto = validationService.processImpotEtTaxeToTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }


    @PostMapping("/process/restitution")
    public ResponseEntity<RestitutionTransactionResponse> processRestitutionToTransaction(
            @Valid @RequestBody RestitutionValidationRequest requestDto) throws Exception {
        RestitutionTransactionResponse responseDto = validationService.processRestitutionToTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @PostMapping("/process/operation-errone")
    public ResponseEntity<OperationErroneTransactionResponse> processOperationErroneToTransaction(
            @Valid @RequestBody OperationErroneValidationRequest requestDto) throws Exception {
        OperationErroneTransactionResponse responseDto = validationService.processOperationErroneToTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    /**
     * Endpoint pour traiter une transaction de rapprochement de virement international (DECAISSEMENT).
     *
     * @param requestDto Le DTO de la requête de transaction de virement international.
     * @return Une ResponseEntity contenant le DTO de réponse de la transaction et un statut HTTP 201 Created.
     */
    @PostMapping("/process/decaissement/virement-international")
    public ResponseEntity<VirementInternationalTransactionResponseDto> processVirementInternationalDecaissementTransaction(
            @Valid @RequestBody VirementInternationalTransactionRequestDto requestDto) throws Exception {
        VirementInternationalTransactionResponseDto responseDto =
                validationService.processVirementInternationalDecaissementTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }


    @PostMapping("/process/encaissement/virement-international")
    public ResponseEntity<VirementInternationalTransactionResponseDto> processVirementInternationalEncaissementTransaction(
            @Valid @RequestBody VirementInternationalTransactionRequestDto requestDto) throws Exception {
        VirementInternationalTransactionResponseDto responseDto =
                validationService.processVirementInternationalEncaissementTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @PostMapping("/process/impayee")
    public ResponseEntity<ImpayeeTransactionResponse> processImpayeeToTransaction(
            @Valid @RequestBody ImpayeeValidationRequest requestDto) throws Exception {
        ImpayeeTransactionResponse responseDto = validationService.processImpayeeToTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @DeleteMapping("/annuler/{transactionId}")
    public void annulerTransaction(@PathVariable Long transactionId) throws  Exception{
         reverseTransactionService.annulerTransaction(transactionId);
    }






}