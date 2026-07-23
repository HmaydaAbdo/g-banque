package staport.finance.gBanque.impayees.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import staport.finance.gBanque.impayees.dtos.EncoursImpayeeDto;
import staport.finance.gBanque.impayees.dtos.EncoursImpayeeSearchDto;
import staport.finance.gBanque.impayees.dtos.ImpayeeRequestDto;
import staport.finance.gBanque.impayees.dtos.ImpayeeResponseDto;
import staport.finance.gBanque.impayees.dtos.ImpayeeSearchDto;

/**
 * Contrat de service pour le module Impayées.
 *
 * <p>Le contrôleur, et tout autre module consommateur, injectent uniquement cette interface
 * (DIP). La classe concrète {@link ImpayeeServiceImpl} n'est jamais référencée depuis
 * l'extérieur du module — voir {@code architecture.md}, "Encapsulation des domaines".
 */
public interface IImpayeeService {

    ImpayeeResponseDto createImpayee(ImpayeeRequestDto requestDto);

    ImpayeeResponseDto getById(Long id);

    Page<ImpayeeResponseDto> searchImpayees(ImpayeeSearchDto searchDto, Pageable pageable);

    ImpayeeResponseDto updateImpayee(Long id, ImpayeeRequestDto requestDto);

    void deleteImpayee(Long id);

    ImpayeeResponseDto toggleStatus(Long id);

    EncoursImpayeeDto getOutstandingImpayees(EncoursImpayeeSearchDto searchDto, Long banqueId, Pageable pageable);

    /**
     * Exporte en PDF l'intégralité des impayées correspondant aux critères fournis (mêmes filtres
     * que {@link #searchImpayees}) — sans pagination : le fichier contient tous les résultats
     * filtrés, pas seulement la page actuellement affichée à l'écran.
     */
    byte[] exporterPdf(ImpayeeSearchDto searchDto);

    /**
     * Exporte en PDF l'intégralité de l'encours d'impayées d'une banque (mêmes critères que
     * {@link #getOutstandingImpayees}, {@code type} inclus) — sans pagination : le fichier
     * contient tous les résultats filtrés, pas seulement la page actuellement affichée à l'écran.
     */
    byte[] exporterEncoursPdf(EncoursImpayeeSearchDto searchDto, Long banqueId);
}
