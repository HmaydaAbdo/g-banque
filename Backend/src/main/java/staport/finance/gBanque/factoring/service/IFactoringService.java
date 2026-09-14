package staport.finance.gBanque.factoring.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import staport.finance.gBanque.factoring.dtos.request.EncoursFactoringSearchDto;
import staport.finance.gBanque.factoring.dtos.request.FactoringRequestDto;
import staport.finance.gBanque.factoring.dtos.request.FactoringSearchDto;
import staport.finance.gBanque.factoring.dtos.response.EncoursFactoringDto;
import staport.finance.gBanque.factoring.dtos.response.FactoringResponseDto;

/**
 * Contrat de service pour le module Factoring.
 *
 * <p>Le contrôleur, et tout autre module consommateur, injectent uniquement cette interface
 * (DIP) — {@code FactoringServiceImpl} n'est jamais référencée depuis l'extérieur du module
 * (architecture.md, "Encapsulation des domaines").
 */
public interface IFactoringService {

    FactoringResponseDto createFactoring(FactoringRequestDto requestDTO) throws Exception;

    FactoringResponseDto updateFactoring(Long id, FactoringRequestDto requestDTO) throws Exception;

    void deleteFactoring(Long id);

    FactoringResponseDto findById(Long id);

    Page<FactoringResponseDto> findAll(FactoringSearchDto searchDTO, Pageable pageable);

    FactoringResponseDto toggleStatus(Long id);

    EncoursFactoringDto getRemboursementEncoursForBanque(EncoursFactoringSearchDto searchDto, Long banqueId, Pageable pageable);

    EncoursFactoringDto getDeblocageEncoursForBanque(EncoursFactoringSearchDto searchDto, Long banqueId, Pageable pageable);

    /**
     * Exporte en PDF l'intégralité des factorings correspondant aux critères fournis (mêmes
     * filtres que {@link #findAll}) — sans pagination.
     */
    byte[] exportPdf(FactoringSearchDto searchDTO);

    /**
     * Exporte en PDF l'encours de remboursement d'une banque (mêmes filtres que
     * {@link #getRemboursementEncoursForBanque}) — sans pagination.
     */
    byte[] exportRemboursementEncoursPdf(EncoursFactoringSearchDto searchDto, Long banqueId);

    /**
     * Exporte en PDF l'encours de déblocage d'une banque (mêmes filtres que
     * {@link #getDeblocageEncoursForBanque}) — sans pagination.
     */
    byte[] exportDeblocageEncoursPdf(EncoursFactoringSearchDto searchDto, Long banqueId);
}
