package staport.finance.gBanque.restitution.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import staport.finance.gBanque.restitution.dtos.EncoursRestitution;
import staport.finance.gBanque.restitution.dtos.EncoursRestitutionSearchDto;
import staport.finance.gBanque.restitution.dtos.RestitutionRequestDto;
import staport.finance.gBanque.restitution.dtos.RestitutionResponseDto;
import staport.finance.gBanque.restitution.dtos.RestitutionSearchDto;

/**
 * Contrat de service pour le module Restitution.
 *
 * <p>Le contrôleur, et tout autre module consommateur, injectent uniquement cette interface
 * (DIP). La classe concrète {@link RestitutionServiceImpl} n'est jamais référencée depuis
 * l'extérieur du module — voir {@code architecture.md}, "Encapsulation des domaines".
 */
public interface IRestitutionService {

    RestitutionResponseDto createRestitution(RestitutionRequestDto requestDto);

    RestitutionResponseDto getById(Long id);

    Page<RestitutionResponseDto> searchRestitutions(RestitutionSearchDto searchDto, Pageable pageable);

    RestitutionResponseDto updateRestitution(Long id, RestitutionRequestDto requestDto);

    void deleteRestitution(Long id);

    EncoursRestitution getOutstandingRestitutions(EncoursRestitutionSearchDto searchDto, Long banqueId, Pageable pageable);

    RestitutionResponseDto toggleStatus(Long id);

    byte[] exporterPdf(RestitutionSearchDto searchDto);

    byte[] exporterEncoursPdf(EncoursRestitutionSearchDto searchDto, Long banqueId);
}
