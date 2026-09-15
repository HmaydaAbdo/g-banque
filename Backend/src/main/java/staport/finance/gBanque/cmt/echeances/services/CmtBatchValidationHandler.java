package staport.finance.gBanque.cmt.echeances.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.cmt.echeances.dtos.CmtTransactionRequestDto;
import staport.finance.gBanque.cmt.echeances.dtos.CmtTransactionResponseDto;
import staport.finance.gBanque.cmt.echeances.entities.EcheanceCMT;
import staport.finance.gBanque.cmt.echeances.repositories.EcheanceCMTRepository;
import staport.finance.gBanque.cmt.echeances.services.interfaces.ICmtValidationService;
import staport.finance.gBanque.cmt.entities.CreditMoyenTerme;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.dtos.lot.BatchValidationRequestDto;
import staport.finance.gBanque.validation.dtos.lot.OperationValideeDto;
import staport.finance.gBanque.validation.services.handlers.BatchValidationHandler;

/**
 * Gère la validation en lot des échéances CMT pour {@code BatchValidationService}.
 * <p>
 * Reprend à l'identique la logique du {@code case CMT} qu'il remplace dans
 * {@code BatchValidationService.processOne} (aucun changement de comportement) : l'identifiant de
 * lot porte l'ID de l'échéance (pas celui du crédit parent), le {@code CreditMoyenTerme} est
 * dérivé côté serveur via la relation {@code @ManyToOne} de l'échéance — jamais fourni par le
 * client (voir Javadoc historique de {@code BatchValidationService}).
 */
@Component
public class CmtBatchValidationHandler implements BatchValidationHandler {

    private final EcheanceCMTRepository echeanceCMTRepository;
    private final ICmtValidationService cmtValidationService;

    public CmtBatchValidationHandler(EcheanceCMTRepository echeanceCMTRepository,
                                      ICmtValidationService cmtValidationService) {
        this.echeanceCMTRepository = echeanceCMTRepository;
        this.cmtValidationService = cmtValidationService;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.CMT;
    }

    @Override
    public OperationValideeDto processOne(Long id, BatchValidationRequestDto requestDto) {
        EcheanceCMT echeance = echeanceCMTRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Échéance CMT introuvable avec l'ID: " + id));
        CreditMoyenTerme creditMoyenTerme = echeance.getCreditMoyenTerme();
        CmtTransactionRequestDto dto = new CmtTransactionRequestDto(
                creditMoyenTerme.getId(),
                id,
                null,
                requestDto.dateValeur(),
                requestDto.dateOperation(),
                null,
                requestDto.tauxTva(),
                null,
                null,
                requestDto.rapprochementId());
        CmtTransactionResponseDto r = cmtValidationService.processCmtTransaction(creditMoyenTerme, dto);
        return new OperationValideeDto(id, r.id(), r.libelle(), r.debit(), r.credit());
    }
}
