package staport.finance.gBanque.leasing.echeancesLeasing.services.interfaces;

import staport.finance.gBanque.leasing.echeancesLeasing.dto.AvanceLeasingResponseDTO;
import staport.finance.gBanque.leasing.echeancesLeasing.dto.AvanceMoyenPaiementRequestDTO;
import staport.finance.gBanque.leasing.echeancesLeasing.dto.MoyenPaiementAvanceDTO;

import java.util.List;

/**
 * Avance d'un contrat de leasing (échéance n°1) et rattachement du chèque / de la LCN / du virement qui la règle.
 * Le rattachement se fait uniquement depuis le contrat ; l'échéance est ensuite représentée par ce moyen de paiement.
 */
public interface IAvanceLeasingService {

    /** Avance du contrat et son moyen de paiement ; refusé si le contrat n'a pas d'avance. */
    AvanceLeasingResponseDTO getAvance(Long contratId);

    /** Décaissements non annulés du montant exact de l'avance et pas encore rattachés à une avance. */
    List<MoyenPaiementAvanceDTO> getMoyensPaiementDisponibles(Long contratId);

    /** Rattache un chèque, une LCN ou un virement à l'avance du contrat. */
    AvanceLeasingResponseDTO rattacherMoyenPaiement(Long contratId, AvanceMoyenPaiementRequestDTO dto);

    /** Détache le moyen de paiement de l'avance — refusé s'il est déjà RÉGLÉ. */
    AvanceLeasingResponseDTO detacherMoyenPaiement(Long contratId);
}
