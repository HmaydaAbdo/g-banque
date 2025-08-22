package staport.finance.gBanque.lesEtatsDesEncours.recapitulatif.decaissement.mappers;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.cmt.echeances.entities.EcheanceCMT;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.entities.EcheanceLeasing;
import staport.finance.gBanque.lesEtatsDesEncours.recapitulatif.decaissement.dtos.RecaputilatifDecaissementItem;
import java.math.RoundingMode;

/**
 * Mapper component to convert EcheanceCMT and EcheanceLeasing entities
 * to the unified RécaputilatifDecaissementItem DTO.
 */
@Component
public class RecapitulatifDecaissementMapper {

    private static final int SCALE = 2;

    /**
     * Maps an EcheanceLeasing entity to a RécaputilatifDecaissementItem DTO.
     *
     * @param echeance The EcheanceLeasing entity to map.
     * @return The mapped RécaputilatifDecaissementItem DTO.
     */
    public RecaputilatifDecaissementItem mapLeasingToDto(EcheanceLeasing echeance) {
        if (echeance == null || echeance.getContratLeasing() == null) {
            return null;
        }

        RecaputilatifDecaissementItem dto = new RecaputilatifDecaissementItem();
        dto.setDateEmission(echeance.getDateEcheance());
        dto.setDebit(echeance.getMontantTTC().setScale(SCALE, RoundingMode.HALF_UP));
        dto.setObservations("Prélèvement");

        // Construct the libelle based on the contract details
        ContratLeasing contrat = echeance.getContratLeasing();
        String libelle;
        // Logic to check if it's the final residual value payment
        if (echeance.getNumeroEcheance() == (contrat.getPeriodeEnMois() + 1)) {
            libelle = contrat.getOrganismeLeasing().getNom() + ", V.R. pour le contrat N° " + contrat.getNumeroContrat();
        } else {
            libelle = contrat.getOrganismeLeasing().getNom() + ", contrat N° " + contrat.getNumeroContrat();
        }
        dto.setLibelle(libelle);

        return dto;
    }

    /**
     * Maps an EcheanceCMT entity to a RécaputilatifDecaissementItem DTO.
     *
     * @param echeance The EcheanceCMT entity to map.
     * @return The mapped RécaputilatifDecaissementItem DTO.
     */
    public RecaputilatifDecaissementItem mapCmtToDto(EcheanceCMT echeance) {
        if (echeance == null || echeance.getCreditMoyenTerme() == null) {
            return null;
        }

        RecaputilatifDecaissementItem dto = new RecaputilatifDecaissementItem();
        dto.setDateEmission(echeance.getDateEcheance());
        dto.setDebit(echeance.getMontant().setScale(SCALE, RoundingMode.HALF_UP));
        dto.setObservations("Prélèvement");
        dto.setLibelle("Échéance CMT");

        return dto;
    }
}