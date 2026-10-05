package staport.finance.gBanque.leasing.contratLeasing.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.leasing.contratLeasing.dto.ContratLeasingRequestDTO;
import staport.finance.gBanque.leasing.contratLeasing.dto.ContratLeasingResponseDTO;
import staport.finance.gBanque.leasing.contratLeasing.dto.ContratLeasingSearchDTO;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;

/**
 * Interface pour le service de gestion des contrats de leasing.
 */
public interface ContratLeasingService {

    ContratLeasingResponseDTO createContratLeasing(ContratLeasingRequestDTO requestDTO) throws ValidationException, ResourceNotFoundException;

    ContratLeasingResponseDTO getContratLeasingById(Long id) throws ResourceNotFoundException;

    ContratLeasingResponseDTO updateContratLeasing(Long id, ContratLeasingRequestDTO requestDTO) throws ResourceNotFoundException, ValidationException;

    void deleteContratLeasing(Long id) throws ResourceNotFoundException;

    Page<ContratLeasingResponseDTO> getActiveContrats(ContratLeasingSearchDTO searchDTO, Pageable pageable);

    Page<ContratLeasingResponseDTO> getPendingLienReleaseContrats(ContratLeasingSearchDTO searchDTO, Pageable pageable);

    Page<ContratLeasingResponseDTO> getInactiveContrats(ContratLeasingSearchDTO searchDTO, Pageable pageable);

    ContratLeasingResponseDTO markAsInactive(Long id) throws ResourceNotFoundException, ValidationException;

    /**
     * Exporte en PDF l'intégralité des contrats actifs correspondant aux filtres fournis
     * (mêmes critères que {@link #getActiveContrats}) — sans pagination.
     *
     * @param colonnes clés des colonnes à inclure, dans l'ordre canonique du module (voir
     *                 {@code ContratLeasingServiceImpl.LEASING_EXPORT_COLUMNS}) ; {@code null}, vide ou sans
     *                 clé reconnue ⇒ toutes les colonnes
     */
    byte[] exportActiveContratsPdf(ContratLeasingSearchDTO searchDTO, List<String> colonnes);

    /** Idem {@link #exportActiveContratsPdf}, au format Excel (.xlsx). */
    byte[] exportActiveContratsExcel(ContratLeasingSearchDTO searchDTO, List<String> colonnes);

    /**
     * Exporte en PDF l'intégralité des contrats en attente de main levée correspondant
     * aux filtres fournis (mêmes critères que {@link #getPendingLienReleaseContrats}).
     *
     * @param colonnes clés des colonnes à inclure, dans l'ordre canonique du module (voir
     *                 {@code ContratLeasingServiceImpl.LEASING_EXPORT_COLUMNS}) ; {@code null}, vide ou sans
     *                 clé reconnue ⇒ toutes les colonnes
     */
    byte[] exportPendingLienReleaseContratsPdf(ContratLeasingSearchDTO searchDTO, List<String> colonnes);

    /** Idem {@link #exportPendingLienReleaseContratsPdf}, au format Excel (.xlsx). */
    byte[] exportPendingLienReleaseContratsExcel(ContratLeasingSearchDTO searchDTO, List<String> colonnes);

    /**
     * Exporte en PDF l'intégralité des contrats clôturés correspondant aux filtres fournis
     * (mêmes critères que {@link #getInactiveContrats}).
     *
     * @param colonnes clés des colonnes à inclure, dans l'ordre canonique du module (voir
     *                 {@code ContratLeasingServiceImpl.LEASING_EXPORT_COLUMNS}) ; {@code null}, vide ou sans
     *                 clé reconnue ⇒ toutes les colonnes
     */
    byte[] exportInactiveContratsPdf(ContratLeasingSearchDTO searchDTO, List<String> colonnes);

    /** Idem {@link #exportInactiveContratsPdf}, au format Excel (.xlsx). */
    byte[] exportInactiveContratsExcel(ContratLeasingSearchDTO searchDTO, List<String> colonnes);

    /**
     * Retourne l'entité ContratLeasing ou lève ResourceNotFoundException.
     * Façade pour les domaines qui ont besoin de l'entité (ex. LeasingValidationService)
     * sans accéder directement à ContratLeasingRepository (CLAUDE.md — Bounded Context).
     */
    ContratLeasing findOrThrow(Long id);
}
