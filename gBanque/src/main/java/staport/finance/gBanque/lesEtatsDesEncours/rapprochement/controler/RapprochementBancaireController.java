package staport.finance.gBanque.lesEtatsDesEncours.rapprochement.controler;


import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.lesEtatsDesEncours.rapprochement.dtos.*;
import staport.finance.gBanque.lesEtatsDesEncours.rapprochement.services.RapprochementService;

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


}