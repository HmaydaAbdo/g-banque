package staport.finance.gBanque.operationErrone.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import staport.finance.gBanque.operationErrone.dtos.OperationErroneRequestDto;
import staport.finance.gBanque.operationErrone.dtos.OperationErroneResponseDto;
import staport.finance.gBanque.operationErrone.dtos.OperationErroneSearchDto;

/**
 * Contrat de service pour le module Opérations Erronées.
 *
 * <p>Le contrôleur, et tout autre module consommateur, injectent uniquement cette interface
 * (DIP). La classe concrète {@link OperationErroneServiceImpl} n'est jamais référencée depuis
 * l'extérieur du module — voir {@code architecture.md}, "Encapsulation des domaines".
 */
public interface IOperationErroneService {

    OperationErroneResponseDto createOperationErrone(OperationErroneRequestDto requestDto);

    OperationErroneResponseDto getById(Long id);

    Page<OperationErroneResponseDto> searchOperationErrone(OperationErroneSearchDto searchDto, Pageable pageable);

    OperationErroneResponseDto updateOperationErrone(Long id, OperationErroneRequestDto requestDto);

    void deleteOperationErrone(Long id);

    OperationErroneResponseDto toggleStatus(Long id);

    byte[] exporterPdf(OperationErroneSearchDto searchDto);
}
