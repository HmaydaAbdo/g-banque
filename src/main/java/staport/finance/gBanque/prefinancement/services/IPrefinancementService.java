package staport.finance.gBanque.prefinancement.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import staport.finance.gBanque.prefinancement.dtos.request.EncoursPrefinancementSearchDto;
import staport.finance.gBanque.prefinancement.dtos.request.PrefinancementRequestDTO;
import staport.finance.gBanque.prefinancement.dtos.request.PrefinancementSearchDTO;
import staport.finance.gBanque.prefinancement.dtos.response.EncoursPrefinancementDto;
import staport.finance.gBanque.prefinancement.dtos.response.PrefinancementResponseDTO;

/**
 * Contrat de service pour le module Préfinancement.
 *
 * <p>Le contrôleur, et tout autre module consommateur, injectent uniquement cette interface
 * (DIP) — {@code PrefinancementServiceImpl} n'est jamais référencée depuis l'extérieur du module
 * (architecture.md, "Encapsulation des domaines").
 */
public interface IPrefinancementService {

    PrefinancementResponseDTO createPrefinancement(PrefinancementRequestDTO requestDTO) throws Exception;

    PrefinancementResponseDTO updatePrefinancement(Long id, PrefinancementRequestDTO requestDTO) throws Exception;

    void deletePrefinancement(Long id);

    PrefinancementResponseDTO findById(Long id);

    Page<PrefinancementResponseDTO> findAll(PrefinancementSearchDTO searchDTO, Pageable pageable);

    PrefinancementResponseDTO toggleStatus(Long id);

    EncoursPrefinancementDto getRemboursementEncoursForBanque(EncoursPrefinancementSearchDto searchDto, Long banqueId, Pageable pageable);

    EncoursPrefinancementDto getDeblocageEncoursForBanque(EncoursPrefinancementSearchDto searchDto, Long banqueId, Pageable pageable);

    /**
     * Exporte en PDF l'intégralité des préfinancements correspondant aux critères fournis
     * (mêmes filtres que {@link #findAll}) — sans pagination.
     */
    byte[] exportPdf(PrefinancementSearchDTO searchDTO);

    /**
     * Exporte en PDF l'encours de remboursement d'une banque (mêmes filtres que
     * {@link #getRemboursementEncoursForBanque}) — sans pagination.
     */
    byte[] exportRemboursementEncoursPdf(EncoursPrefinancementSearchDto searchDto, Long banqueId);

    /**
     * Exporte en PDF l'encours de déblocage d'une banque (mêmes filtres que
     * {@link #getDeblocageEncoursForBanque}) — sans pagination.
     */
    byte[] exportDeblocageEncoursPdf(EncoursPrefinancementSearchDto searchDto, Long banqueId);
}
