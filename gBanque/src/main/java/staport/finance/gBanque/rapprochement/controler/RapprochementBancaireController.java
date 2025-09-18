package staport.finance.gBanque.rapprochement.controler;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import staport.finance.gBanque.decomptes.dto.DecompteTransactionRequestDto;
import staport.finance.gBanque.decomptes.dto.DecompteTransactionResponseDto;
import staport.finance.gBanque.droitConstate.dtos.DroitConstateTransactionRequest;
import staport.finance.gBanque.droitConstate.dtos.DroitConstateTransactionResponse;
import staport.finance.gBanque.prefinancement.dtos.PrefinancementTransactionRequest;
import staport.finance.gBanque.prefinancement.dtos.PrefinancementTransactionResponse;
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


    /**
     * Endpoint pour traiter une transaction de rapprochement de cheque.
     * Prend en charge un DTO de requête et retourne un DTO de réponse avec les détails de la transaction créée.
     *
     * @param requestDto Le DTO de la requête de transaction de cheque.
     * @return Une ResponseEntity contenant le DTO de réponse de la transaction et un statut HTTP 201 Created.
     */
    @PostMapping("/process/cheque")
    public ResponseEntity<ChequeTransactionResponseDto> processChequeTransaction(@Valid @RequestBody ChequeTransactionRequest requestDto) throws Exception {
        ChequeTransactionResponseDto responseDto = rapprochementService.processChequeTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }


    /**
     * Endpoint pour traiter une transaction de rapprochement de lcn.
     * Prend en charge un DTO de requête et retourne un DTO de réponse avec les détails de la transaction créée.
     *
     * @param requestDto Le DTO de la requête de transaction de lcn.
     * @return Une ResponseEntity contenant le DTO de réponse de la transaction et un statut HTTP 201 Created.
     */
    @PostMapping("/process/lcn")
    public ResponseEntity<LcnTransactionResponse> processLcnTransaction(@Valid @RequestBody LcnTransactionRequest requestDto) throws Exception {
        LcnTransactionResponse responseDto = rapprochementService.processLcnTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }


    /**
     * Endpoint pour traiter une transaction de rapprochement de miseADisposition.
     * Prend en charge un DTO de requête et retourne un DTO de réponse avec les détails de la transaction créée.
     *
     * @param requestDto Le DTO de la requête de transaction de miseADisposition.
     * @return Une ResponseEntity contenant le DTO de réponse de la transaction et un statut HTTP 201 Created.
     */
    @PostMapping("/process/mise-a-disposition")
    public ResponseEntity<MiseAdispositionTransactionResponseDto> processMiseAdispositionTransaction(@Valid @RequestBody MiseAdispositionTransactionRequestDto requestDto) throws Exception {
        MiseAdispositionTransactionResponseDto responseDto = rapprochementService.processMiseAdispoTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }


    /**
     * Endpoint pour traiter une transaction de rapprochement de virement.
     * Prend en charge un DTO de requête et retourne un DTO de réponse avec les détails de la transaction créée.
     *
     * @param requestDto Le DTO de la requête de transaction de virement.
     * @return Une ResponseEntity contenant le DTO de réponse de la transaction et un statut HTTP 201 Created.
     */
    @PostMapping("/process/virement")
    public ResponseEntity<VirementTransactionResponseDto> processVirementTransaction(@Valid @RequestBody VirementTransactionRequestDto requestDto) throws Exception {
        VirementTransactionResponseDto responseDto = rapprochementService.processVirementTransaction(requestDto);
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


}