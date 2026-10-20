package staport.finance.gBanque.leasing.echeancesLeasing.services.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.dto.AvanceLeasingResponseDTO;
import staport.finance.gBanque.leasing.echeancesLeasing.dto.AvanceMoyenPaiementRequestDTO;
import staport.finance.gBanque.leasing.echeancesLeasing.dto.MoyenPaiementAvanceDTO;
import staport.finance.gBanque.moyensDePaiements.lecture.TypeMoyenPaiement;

/**
 * Avance d'un contrat de leasing (échéance n°1) : elle est définie par le chèque, la LCN ou le virement qui la règle,
 * choisi parmi ceux qui existent déjà (en cours ou réglés). Le montant de l'avance est celui du moyen de paiement.
 * L'échéance est ensuite représentée par ce moyen de paiement : une seule transaction existe, celle du moyen de
 * paiement, et son libellé et son observation mentionnent l'avance.
 */
public interface IAvanceLeasingService {

    /** Avance du contrat et son moyen de paiement ; refusé si le contrat n'a pas d'avance. */
    AvanceLeasingResponseDTO getAvance(Long contratId);

    /**
     * Décaissements non annulés du type donné (réglés ou en cours) qui ne règlent pas déjà l'avance d'un autre
     * contrat, par pages — alimente la liste déroulante du wizard.
     *
     * @param contratId contrat en cours de modification dont le propre moyen de paiement reste proposé ({@code null} à la création)
     */
    Page<MoyenPaiementAvanceDTO> getMoyensPaiementDisponibles(TypeMoyenPaiement type, String recherche,
                                                              Long contratId, Pageable pageable);

    /**
     * Contrôle qu'un chèque, une LCN ou un virement peut régler une avance (existant, décaissement, non annulé, pas déjà
     * rattaché à une avance) et le renvoie : son montant devient celui de l'avance.
     */
    MoyenPaiementAvanceDTO resoudreMoyen(AvanceMoyenPaiementRequestDTO dto);

    /** Rattache le moyen de paiement à l'échéance n°1 du contrat (déjà générée) et y inscrit la mention d'avance. */
    void rattacher(Long contratId, AvanceMoyenPaiementRequestDTO dto);

    /** Détache le moyen de paiement de l'avance du contrat, s'il y en a un, et retire la mention d'avance. */
    void liberer(ContratLeasing contrat);
}
