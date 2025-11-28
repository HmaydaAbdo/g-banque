package staport.finance.gBanque.rapprochement.controler;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import staport.finance.gBanque.rapprochement.dtos.agios.AgiosTransactionResponse;
import staport.finance.gBanque.rapprochement.dtos.agios.AgiosValidationRequest;
import staport.finance.gBanque.rapprochement.dtos.commision.CommissionTransactionResponse;
import staport.finance.gBanque.rapprochement.dtos.commision.CommissionValidationDto;
import staport.finance.gBanque.rapprochement.dtos.decomptes.DecompteTransactionRequestDto;
import staport.finance.gBanque.rapprochement.dtos.decomptes.DecompteTransactionResponseDto;
import staport.finance.gBanque.rapprochement.dtos.droitConstate.DroitConstateTransactionRequest;
import staport.finance.gBanque.rapprochement.dtos.droitConstate.DroitConstateTransactionResponse;
import staport.finance.gBanque.rapprochement.dtos.factoring.FactoringTransactionRequest;
import staport.finance.gBanque.rapprochement.dtos.factoring.FactoringTransactionResponse;
import staport.finance.gBanque.rapprochement.dtos.fraisBancaire.FraisBancaireTransactionResponse;
import staport.finance.gBanque.rapprochement.dtos.fraisBancaire.FraisBanciareValidationRequest;
import staport.finance.gBanque.rapprochement.dtos.interet.InteretTransactionResponse;
import staport.finance.gBanque.rapprochement.dtos.interet.InteretValidationRequest;
import staport.finance.gBanque.rapprochement.dtos.prefinancements.PrefinancementTransactionRequest;
import staport.finance.gBanque.rapprochement.dtos.prefinancements.PrefinancementTransactionResponse;
import staport.finance.gBanque.rapprochement.dtos.cheques.ChequeTransactionRequest;
import staport.finance.gBanque.rapprochement.dtos.cheques.ChequeTransactionResponseDto;
import staport.finance.gBanque.rapprochement.dtos.cmt.CmtTransactionRequestDto;
import staport.finance.gBanque.rapprochement.dtos.cmt.CmtTransactionResponseDto;
import staport.finance.gBanque.rapprochement.dtos.lcn.LcnTransactionRequest;
import staport.finance.gBanque.rapprochement.dtos.lcn.LcnTransactionResponse;
import staport.finance.gBanque.rapprochement.dtos.leasing.LeasingTransactionRequestDto;
import staport.finance.gBanque.rapprochement.dtos.leasing.LeasingTransactionResponseDto;
import staport.finance.gBanque.rapprochement.dtos.miseAdispo.MiseAdispositionTransactionRequestDto;
import staport.finance.gBanque.rapprochement.dtos.miseAdispo.MiseAdispositionTransactionResponseDto;
import staport.finance.gBanque.rapprochement.dtos.prelevements.PrelevementTransactionResponse;
import staport.finance.gBanque.rapprochement.dtos.prelevements.PrelevementValidationRequest;
import staport.finance.gBanque.rapprochement.dtos.virements.VirementTransactionRequestDto;
import staport.finance.gBanque.rapprochement.dtos.virements.VirementTransactionResponseDto;
import staport.finance.gBanque.rapprochement.services.RapprochementService;

@RestController
@RequestMapping("/rapprochement")
public class RapprochementBancaireController {

    private final RapprochementService rapprochementService;

    public RapprochementBancaireController(RapprochementService rapprochementService) {
        this.rapprochementService = rapprochementService;
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
        LeasingTransactionResponseDto responseDto = rapprochementService.processLeasingTransaction(requestDto);
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
        CmtTransactionResponseDto responseDto = rapprochementService.processCmtTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }


    @PostMapping("/process/mise-a-disposition")
    public ResponseEntity<MiseAdispositionTransactionResponseDto> processMiseAdispositionTransaction(@Valid @RequestBody MiseAdispositionTransactionRequestDto requestDto) throws Exception {
        MiseAdispositionTransactionResponseDto responseDto = rapprochementService.processMiseAdispoTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }



    @PostMapping("/process/decaissement/virement")
    public ResponseEntity<VirementTransactionResponseDto> processVirementDecaissementTransaction(@Valid @RequestBody VirementTransactionRequestDto requestDto) throws Exception {
        VirementTransactionResponseDto responseDto = rapprochementService.processVirementDecaissementTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }


    @PostMapping("/process/encaissement/virement")
    public ResponseEntity<VirementTransactionResponseDto> processVirementEncaissementTransaction(@Valid @RequestBody VirementTransactionRequestDto requestDto) throws Exception {
        VirementTransactionResponseDto responseDto = rapprochementService.processVirementEncaissementTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }


    @PostMapping("/process/decaissement/cheque")
    public ResponseEntity<ChequeTransactionResponseDto> processDecaissementChequeTransaction(@Valid @RequestBody ChequeTransactionRequest requestDto) throws Exception {
        ChequeTransactionResponseDto responseDto = rapprochementService.processChequeDecaissementTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @PostMapping("/process/encaissement/cheque")
    public ResponseEntity<ChequeTransactionResponseDto> processEncaissementChequeTransaction(@Valid @RequestBody ChequeTransactionRequest requestDto) throws Exception {
        ChequeTransactionResponseDto responseDto = rapprochementService.processChequeEncaissementTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @PostMapping("/process/decaissement/lcn")
    public ResponseEntity<LcnTransactionResponse> processDecaissementLcnTransaction(@Valid @RequestBody LcnTransactionRequest requestDto) throws Exception {
        LcnTransactionResponse responseDto = rapprochementService.processDecaissementLcnTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @PostMapping("/process/encaissement/lcn")
    public ResponseEntity<LcnTransactionResponse> processEncaissementLcnTransaction(@Valid @RequestBody LcnTransactionRequest requestDto) throws Exception {
        LcnTransactionResponse responseDto = rapprochementService.processEncaissementLcnTransaction(requestDto);
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
        PrefinancementTransactionResponse responseDto = rapprochementService.processPrefinancementTransaction(requestDto);
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
        DroitConstateTransactionResponse responseDto = rapprochementService.processDroitConstateTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @PostMapping("/process/factoring")
    public ResponseEntity<FactoringTransactionResponse> processFactoringTransaction(@Valid @RequestBody FactoringTransactionRequest requestDto) throws Exception {
        FactoringTransactionResponse responseDto = rapprochementService.processFactoringTransaction(requestDto);
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
        DecompteTransactionResponseDto responseDto = rapprochementService.processDecompteToTransaction(requestDto);
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
        CommissionTransactionResponse responseDto = rapprochementService.processCommissionToTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @PostMapping("/process/agios")
    public ResponseEntity<AgiosTransactionResponse> processAgiosToTransaction(@Valid @RequestBody AgiosValidationRequest requestDto) throws Exception {
        AgiosTransactionResponse responseDto = rapprochementService.processAgiosToTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @PostMapping("/process/prelevement")
    public ResponseEntity<PrelevementTransactionResponse> processPrelevementToTransaction(@Valid @RequestBody PrelevementValidationRequest requestDto) throws Exception {
        PrelevementTransactionResponse responseDto = rapprochementService.processPrelevementToTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @PostMapping("/process/frais-bancaire")
    public ResponseEntity<FraisBancaireTransactionResponse> processFraisBancaireToTransaction(@Valid @RequestBody FraisBanciareValidationRequest requestDto) throws Exception {
        FraisBancaireTransactionResponse responseDto = rapprochementService.processFraisBancaireToTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @PostMapping("/process/interet")
    public ResponseEntity<InteretTransactionResponse> processInteretToTransaction(@Valid @RequestBody InteretValidationRequest requestDto) throws Exception {
        InteretTransactionResponse responseDto = rapprochementService.processInteretToTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }



}