package staport.finance.gBanque.decomptes.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import staport.finance.gBanque.decomptes.dtos.request.DecompteRequestDto;
import staport.finance.gBanque.decomptes.dtos.request.DecompteSearchDto;
import staport.finance.gBanque.decomptes.dtos.request.EncoursDecompteSearchDto;
import staport.finance.gBanque.decomptes.dtos.response.DecompteResponseDto;
import staport.finance.gBanque.decomptes.dtos.response.EncoursDecompteDto;

/**
 * Contrat de service pour le module Décompte.
 *
 * <p>Le contrôleur, et tout autre module consommateur, injectent uniquement cette interface
 * (DIP) — {@code DecompteServiceImpl} n'est jamais référencée depuis l'extérieur du module
 * (architecture.md, "Encapsulation des domaines").
 */
public interface IDecompteService {

    DecompteResponseDto createDecompte(DecompteRequestDto requestDTO);

    DecompteResponseDto updateDecompte(Long id, DecompteRequestDto requestDTO);

    void deleteDecompte(Long id);

    DecompteResponseDto findById(Long id);

    Page<DecompteResponseDto> findAll(DecompteSearchDto searchDTO, Pageable pageable);

    DecompteResponseDto toggleStatus(Long id);

    byte[] exportToPdf(DecompteSearchDto searchDTO);

    EncoursDecompteDto getDecomptesEncoursForBanque(EncoursDecompteSearchDto searchDto, Long banqueId, Pageable pageable);

    /**
     * Exporte en PDF l'intégralité de l'encours de décomptes d'une banque (mêmes filtres que
     * {@link #getDecomptesEncoursForBanque}) — sans pagination : le fichier contient tous les
     * résultats filtrés, pas seulement la page actuellement affichée à l'écran.
     */
    byte[] exportEncoursPdf(EncoursDecompteSearchDto searchDto, Long banqueId);
}
