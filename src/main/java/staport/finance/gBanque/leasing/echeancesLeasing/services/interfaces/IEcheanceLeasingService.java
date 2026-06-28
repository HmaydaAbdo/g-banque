package staport.finance.gBanque.leasing.echeancesLeasing.services.interfaces;

import org.springframework.data.domain.Pageable;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.dto.EcheanceLeasingResponseDTO;
import staport.finance.gBanque.leasing.echeancesLeasing.dto.EcheanceLeasingSearchDTO;
import staport.finance.gBanque.leasing.echeancesLeasing.dto.EcheanceLeasingUpdateRequestDTO;
import staport.finance.gBanque.leasing.echeancesLeasing.dto.EncoursLeasingRequestDTO;
import staport.finance.gBanque.leasing.echeancesLeasing.dto.EncoursLeasingResponseDTO;
import staport.finance.gBanque.leasing.echeancesLeasing.dto.TableauAmortissementDTO;

public interface IEcheanceLeasingService {

    void generateEcheances(ContratLeasing contrat);

    void regenerateEcheances(Long contratId);

    TableauAmortissementDTO getTableauAmortissementForContrat(Long contratId, EcheanceLeasingSearchDTO searchDTO, Pageable pageable);

    EcheanceLeasingResponseDTO toggleStatus(Long echeanceId);

    EcheanceLeasingResponseDTO updateEcheance(Long id, EcheanceLeasingUpdateRequestDTO dto);

    EncoursLeasingResponseDTO getEncoursLeasingForBanque(EncoursLeasingRequestDTO requestDTO, Pageable pageable);

    boolean hasPaidEcheances(Long contratId);

    void deleteEcheancesAndCommissionByContrat(ContratLeasing contrat);
}
