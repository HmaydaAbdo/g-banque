package staport.finance.gBanque.provision.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import staport.finance.gBanque.provision.dtos.EncoursProvisionDto;
import staport.finance.gBanque.provision.dtos.EncoursProvisionSearchDto;
import staport.finance.gBanque.provision.dtos.ProvisionRequestDto;
import staport.finance.gBanque.provision.dtos.ProvisionResponseDto;
import staport.finance.gBanque.provision.dtos.ProvisionSearchDto;

/**
 * Contrat de service pour le module Provision.
 *
 * <p>Le contrôleur, et tout autre module consommateur, injectent uniquement cette interface (DIP).
 * La classe concrète {@link ProvisionServiceImpl} n'est jamais référencée depuis l'extérieur du
 * module — voir {@code architecture.md}, "Encapsulation des domaines".
 */
public interface IProvisionService {

    ProvisionResponseDto createProvision(ProvisionRequestDto requestDto);

    ProvisionResponseDto updateProvision(Long id, ProvisionRequestDto requestDto);

    Page<ProvisionResponseDto> searchProvision(ProvisionSearchDto searchDto, Pageable pageable);

    ProvisionResponseDto getById(Long id);

    void deleteProvision(Long id);

    ProvisionResponseDto toggleStatus(Long id);

    EncoursProvisionDto getOutstandingProvision(EncoursProvisionSearchDto searchDto, Long banqueId, Pageable pageable);

    byte[] exporterPdf(ProvisionSearchDto searchDto);

    byte[] exporterEncoursPdf(EncoursProvisionSearchDto searchDto, Long banqueId);
}
