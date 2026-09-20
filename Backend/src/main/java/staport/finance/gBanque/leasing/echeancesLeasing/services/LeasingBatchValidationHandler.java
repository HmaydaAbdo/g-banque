package staport.finance.gBanque.leasing.echeancesLeasing.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.dto.LeasingTransactionRequestDTO;
import staport.finance.gBanque.leasing.echeancesLeasing.dto.LeasingTransactionResponseDTO;
import staport.finance.gBanque.leasing.echeancesLeasing.entities.EcheanceLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.repositories.EcheanceLeasingRepository;
import staport.finance.gBanque.leasing.echeancesLeasing.services.interfaces.ILeasingValidationService;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.dtos.lot.BatchValidationRequestDto;
import staport.finance.gBanque.validation.dtos.lot.OperationValideeDto;
import staport.finance.gBanque.validation.services.handlers.BatchValidationHandler;

/**
 * Gère la validation en lot des échéances de leasing pour {@code BatchValidationService}.
 * <p>
 * Reprend à l'identique la logique du {@code case LEASING} qu'il remplace dans
 * {@code BatchValidationService.processOne} (aucun changement de comportement) : l'identifiant de
 * lot porte l'ID de l'échéance (pas celui du contrat parent), le {@code ContratLeasing} est dérivé
 * côté serveur via la relation {@code @ManyToOne} de l'échéance — jamais fourni par le client (voir
 * Javadoc historique de {@code BatchValidationService}).
 */
@Component
public class LeasingBatchValidationHandler implements BatchValidationHandler {

    private final EcheanceLeasingRepository echeanceLeasingRepository;
    private final ILeasingValidationService leasingValidationService;

    public LeasingBatchValidationHandler(EcheanceLeasingRepository echeanceLeasingRepository,
                                          ILeasingValidationService leasingValidationService) {
        this.echeanceLeasingRepository = echeanceLeasingRepository;
        this.leasingValidationService = leasingValidationService;
    }

    @Override
    public TransactionSource source() {
        return TransactionSource.LEASING;
    }

    @Override
    public OperationValideeDto processOne(Long id, BatchValidationRequestDto requestDto) {
        EcheanceLeasing echeance = echeanceLeasingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Échéance de leasing introuvable avec l'ID: " + id));
        ContratLeasing contratLeasing = echeance.getContratLeasing();
        LeasingTransactionRequestDTO dto = new LeasingTransactionRequestDTO(
                contratLeasing.getId(),
                id,
                null,
                requestDto.dateValeur(),
                requestDto.dateOperation(),
                requestDto.observation(),
                requestDto.tauxTva(),
                requestDto.montantTva(),
                requestDto.libelle(),
                requestDto.rapprochementId());
        LeasingTransactionResponseDTO r = leasingValidationService.processLeasingTransaction(contratLeasing, dto);
        return new OperationValideeDto(id, r.id(), r.libelle(), r.debit(), r.credit());
    }
}
