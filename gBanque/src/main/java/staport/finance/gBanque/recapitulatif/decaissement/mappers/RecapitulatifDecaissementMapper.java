package staport.finance.gBanque.recapitulatif.decaissement.mappers;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.cheques.entities.Cheque;
import staport.finance.gBanque.cmt.echeances.entities.EcheanceCMT;
import staport.finance.gBanque.lcn.entities.Lcn;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.entities.EcheanceLeasing;
import staport.finance.gBanque.miseADisposition.entities.MiseADisposition;
import staport.finance.gBanque.recapitulatif.decaissement.dtos.RecaputilatifDecaissementItem;
import staport.finance.gBanque.virements.entities.Virement;

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


    /**
     * Maps a Cheque entity to a RécaputilatifDecaissementItem DTO.
     *
     * @param cheque The Cheque entity to map.
     * @return The mapped RécaputilatifDecaissementItem DTO.
     */
    public RecaputilatifDecaissementItem mapChequeToDto(Cheque cheque) {
        if (cheque == null) {
            return null;
        }

        RecaputilatifDecaissementItem dto = new RecaputilatifDecaissementItem();
        dto.setDateEmission(cheque.getDateEmission());
        if ( dto.getDateEcheance() != null) {
            dto.setDateEcheance(cheque.getDateEcheance());
        }
        dto.setDebit(cheque.getMontant().setScale(SCALE, RoundingMode.HALF_UP));
        dto.setObservations("Prélèvement ");
        dto.setLibelle("Chèque N° " + cheque.getNumero() );

        return dto;
    }

    /**
     * Maps an Lcn entity to a RécaputilatifDecaissementItem DTO.
     *
     * @param lcn The Lcn entity to map.
     * @return The mapped RécaputilatifDecaissementItem DTO.
     */
    public RecaputilatifDecaissementItem mapLcnToDto(Lcn lcn) {
        if (lcn == null) {
            return null;
        }

        RecaputilatifDecaissementItem dto = new RecaputilatifDecaissementItem();
        dto.setDateEmission(lcn.getDateEmission());
        dto.setDateEcheance(lcn.getDateEcheance());
        dto.setDebit(lcn.getMontant().setScale(SCALE, RoundingMode.HALF_UP));
        dto.setObservations("LCN à l'échéance");
        dto.setLibelle("LCN N° " + lcn.getNumero() + " au bénéficiaire : " + lcn.getBeneficiaire());

        return dto;
    }

    /**
     * Maps a Virement entity to a RecaputilatifDecaissementItem DTO.
     *
     * @param virement The Virement entity to map.
     * @return The mapped RecaputilatifDecaissementItem DTO.
     */
    public RecaputilatifDecaissementItem mapVirementToDto(Virement virement) {
        if (virement == null) {
            return null;
        }

        RecaputilatifDecaissementItem dto = new RecaputilatifDecaissementItem();
        dto.setDateEmission(virement.getDateEmission());
        dto.setDebit(virement.getMontant().setScale(SCALE, RoundingMode.HALF_UP));
        dto.setObservations("Prélèvement");
        dto.setLibelle("Virement N° " + virement.getNumero() );

        return dto;
    }

    /**
     * Maps a MiseADisposition entity to a RecaputilatifDecaissementItem DTO.
     *
     * @param mad The MiseADisposition entity to map.
     * @return The mapped RecaputilatifDecaissementItem DTO.
     */
    public RecaputilatifDecaissementItem mapMiseADispositionToDto(MiseADisposition mad) {
        if (mad == null) {
            return null;
        }

        RecaputilatifDecaissementItem dto = new RecaputilatifDecaissementItem();
        dto.setDateEmission(mad.getDateEmission());
        dto.setDebit(mad.getMontant().setScale(SCALE, RoundingMode.HALF_UP));
        dto.setObservations("Prélèvement");
        dto.setLibelle("Mise à disposition N° " + mad.getNumero() );

        return dto;
    }
}