package staport.finance.gBanque.prefinancement.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import staport.finance.gBanque.prefinancement.dtos.request.PrefinancementRequestDTO;
import staport.finance.gBanque.prefinancement.dtos.request.PrefinancementSearchDTO;
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

    /**
     * Exporte en PDF l'intégralité des préfinancements correspondant aux critères fournis
     * (mêmes filtres que {@link #findAll}) — sans pagination.
     */
    byte[] exportPdf(PrefinancementSearchDTO searchDTO);
}
