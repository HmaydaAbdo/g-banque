package staport.finance.gBanque.droitConstate.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import staport.finance.gBanque.droitConstate.dtos.request.DroitConstateRequestDto;
import staport.finance.gBanque.droitConstate.dtos.request.DroitConstateSearchDto;
import staport.finance.gBanque.droitConstate.dtos.request.EncoursDroitConstateSearchDto;
import staport.finance.gBanque.droitConstate.dtos.response.DroitConstateResponseDto;
import staport.finance.gBanque.droitConstate.dtos.response.EncoursDroitConstateDto;

/**
 * Contrat de service pour le module Droit Constaté.
 *
 * <p>Le contrôleur, et tout autre module consommateur, injectent uniquement cette interface
 * (DIP) — {@code DroitConstateServiceImpl} n'est jamais référencée depuis l'extérieur du module
 * (architecture.md, "Encapsulation des domaines").
 */
public interface IDroitConstateService {

    DroitConstateResponseDto createDroitConstate(DroitConstateRequestDto requestDTO) throws Exception;

    DroitConstateResponseDto updateDroitConstate(Long id, DroitConstateRequestDto requestDTO) throws Exception;

    void deleteDroitConstate(Long id);

    DroitConstateResponseDto findById(Long id);

    Page<DroitConstateResponseDto> findAll(DroitConstateSearchDto searchDTO, Pageable pageable);

    DroitConstateResponseDto toggleStatus(Long id);

    EncoursDroitConstateDto getRemboursementEncoursForBanque(EncoursDroitConstateSearchDto searchDto, Long banqueId, Pageable pageable);

    EncoursDroitConstateDto getDeblocageEncoursForBanque(EncoursDroitConstateSearchDto searchDto, Long banqueId, Pageable pageable);
}
