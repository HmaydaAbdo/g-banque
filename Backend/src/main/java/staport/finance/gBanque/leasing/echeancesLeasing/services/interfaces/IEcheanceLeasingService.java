package staport.finance.gBanque.leasing.echeancesLeasing.services.interfaces;

import org.springframework.data.domain.Pageable;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.dto.EcheanceLeasingResponseDTO;
import staport.finance.gBanque.leasing.echeancesLeasing.dto.EcheanceLeasingSearchDTO;
import staport.finance.gBanque.leasing.echeancesLeasing.dto.EcheanceLeasingUpdateRequestDTO;
import staport.finance.gBanque.leasing.echeancesLeasing.dto.MontantsContratLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.dto.TableauAmortissementDTO;

import java.util.Collection;
import java.util.Map;

public interface IEcheanceLeasingService {

    void generateEcheances(ContratLeasing contrat);

    /**
     * Régénère les échéances et commissions d'un contrat de leasing existant.
     * Prend l'entité directement (et non un simple ID) pour éviter que ce service
     * n'ait besoin d'injecter le Repository du module contratLeasing — l'appelant
     * (ContratLeasingServiceImpl) possède déjà l'entité au moment de l'appel.
     */
    void regenerateEcheances(ContratLeasing contrat);

    /**
     * @param contratId     ID du contrat (déjà validé par l'appelant — voir ContratLeasingService.getContratLeasingById).
     * @param numeroContrat Numéro du contrat, résolu par l'appelant (module contratLeasing) et transmis
     *                       pour éviter à ce service de dépendre du Repository d'un autre domaine.
     */
    TableauAmortissementDTO getTableauAmortissementForContrat(Long contratId, String numeroContrat,
                                                               EcheanceLeasingSearchDTO searchDTO, Pageable pageable);

    /**
     * Exporte en PDF le tableau d'amortissement complet d'un contrat (mêmes critères que
     * {@link #getTableauAmortissementForContrat}, sans pagination), avec les totaux
     * Endettement / Payé / Restant.
     */
    byte[] exportTableauAmortissementPdf(ContratLeasing contrat, EcheanceLeasingSearchDTO searchDTO);

    EcheanceLeasingResponseDTO toggleStatus(Long echeanceId);

    EcheanceLeasingResponseDTO updateEcheance(Long id, EcheanceLeasingUpdateRequestDTO dto);

    /**
     * Récupère une échéance de leasing par son identifiant.
     * Utilisé par l'écran de récapitulatif unifié (validation) pour résoudre le contrat de
     * leasing (contratLeasingId, numéro de contrat, montants HT/TVA) avant l'ouverture du
     * formulaire de validation, sans dupliquer cette logique côté frontend.
     *
     * @param id L'identifiant de l'échéance de leasing
     * @return Le DTO de l'échéance correspondante
     */
    EcheanceLeasingResponseDTO getEcheanceLeasingById(Long id);

    boolean hasPaidEcheances(Long contratId);

    /**
     * Endettement et déjà payé de chaque contrat demandé, en une seule requête (utilisé par les exports).
     * Un contrat sans échéance est absent de la map.
     */
    Map<Long, MontantsContratLeasing> getMontantsByContratIds(Collection<Long> contratIds);

    void deleteEcheancesAndCommissionByContrat(ContratLeasing contrat);
}
