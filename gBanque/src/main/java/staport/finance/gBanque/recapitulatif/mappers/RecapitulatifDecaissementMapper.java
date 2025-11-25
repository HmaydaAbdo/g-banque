package staport.finance.gBanque.recapitulatif.mappers;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Agios.entities.Agios;
import staport.finance.gBanque.cheques.entities.Cheque;
import staport.finance.gBanque.cmt.echeances.entities.EcheanceCMT;
import staport.finance.gBanque.commision.entities.Commission;
import staport.finance.gBanque.droitConstate.entities.DroitConstate;
import staport.finance.gBanque.factoring.entities.Factoring;
import staport.finance.gBanque.fraisBancaires.entities.FraisBancaire;
import staport.finance.gBanque.interets.entities.Interet;
import staport.finance.gBanque.lcn.entities.Lcn;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.entities.EcheanceLeasing;
import staport.finance.gBanque.miseADisposition.entities.MiseADisposition;
import staport.finance.gBanque.prefinancement.entities.Prefinancement;
import staport.finance.gBanque.prelevements.entitie.Prelevement;
import staport.finance.gBanque.recapitulatif.dtos.RecaputilatifItem;
import staport.finance.gBanque.transactions.enums.TransactionSource;
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
    public RecaputilatifItem mapLeasingToDto(EcheanceLeasing echeance) {
        if (echeance == null || echeance.getContratLeasing() == null) {
            return null;
        }

        RecaputilatifItem dto = new RecaputilatifItem();
        dto.setDateEmission(echeance.getDateEcheance());
        dto.setDebit(echeance.getMontantTTC().setScale(SCALE, RoundingMode.HALF_UP));
        dto.setObservations("Prélèvement leasing");

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
        dto.setSource(TransactionSource.LEASING);
        return dto;
    }

    /**
     * Maps an EcheanceCMT entity to a RécaputilatifDecaissementItem DTO.
     *
     * @param echeance The EcheanceCMT entity to map.
     * @return The mapped RécaputilatifDecaissementItem DTO.
     */
    public RecaputilatifItem mapCmtToDto(EcheanceCMT echeance) {
        if (echeance == null || echeance.getCreditMoyenTerme() == null) {
            return null;
        }

        RecaputilatifItem dto = new RecaputilatifItem();
        dto.setDateEmission(echeance.getDateEcheance());
        dto.setDebit(echeance.getMontant().setScale(SCALE, RoundingMode.HALF_UP));
        dto.setObservations("Prélèvement CMT");
        dto.setLibelle("Échéance CMT");
        dto.setSource(TransactionSource.CMT);
        return dto;
    }


    /**
     * Maps a Cheque entity to a RécaputilatifDecaissementItem DTO.
     *
     * @param cheque The Cheque entity to map.
     * @return The mapped RécaputilatifDecaissementItem DTO.
     */
    public RecaputilatifItem mapChequeToDto(Cheque cheque) {
        if (cheque == null) {
            return null;
        }

        RecaputilatifItem dto = new RecaputilatifItem();
        dto.setDateEmission(cheque.getDateEmission());
        if ( dto.getDateEcheance() != null) {
            dto.setDateEcheance(cheque.getDateEcheance());
        }
        dto.setDebit(cheque.getMontant().setScale(SCALE, RoundingMode.HALF_UP));
        dto.setObservations("Chèque N° " + cheque.getNumero());
        dto.setLibelle("Bénéficiaire : " + cheque.getBeneficiaire() );
        dto.setSource(TransactionSource.CHEQUE);
        return dto;
    }

    /**
     * Maps an Lcn entity to a RécaputilatifDecaissementItem DTO.
     *
     * @param lcn The Lcn entity to map.
     * @return The mapped RécaputilatifDecaissementItem DTO.
     */
    public RecaputilatifItem mapLcnToDto(Lcn lcn) {
        if (lcn == null) {
            return null;
        }

        RecaputilatifItem dto = new RecaputilatifItem();
        dto.setDateEmission(lcn.getDateEmission());
        dto.setDateEcheance(lcn.getDateEcheance());
        dto.setDebit(lcn.getMontant().setScale(SCALE, RoundingMode.HALF_UP));
        dto.setObservations("LCN N° " + lcn.getNumero());
        dto.setLibelle( "Bénéficiaire : " + lcn.getBeneficiaire());
        dto.setSource(TransactionSource.LCN);
        return dto;
    }

    /**
     * Maps a Virement entity to a RecaputilatifDecaissementItem DTO.
     *
     * @param virement The Virement entity to map.
     * @return The mapped RecaputilatifDecaissementItem DTO.
     */
    public RecaputilatifItem mapVirementToDto(Virement virement) {
        if (virement == null) {
            return null;
        }

        RecaputilatifItem dto = new RecaputilatifItem();
        dto.setDateEmission(virement.getDateEmission());
        dto.setDebit(virement.getMontant().setScale(SCALE, RoundingMode.HALF_UP));
        dto.setObservations("Virement N° " + virement.getNumero());
        dto.setLibelle("Bénéficiaire " + virement.getBeneficiaire() );
        dto.setSource(TransactionSource.Virement);
        return dto;
    }

    /**
     * Maps a MiseADisposition entity to a RecaputilatifDecaissementItem DTO.
     *
     * @param mad The MiseADisposition entity to map.
     * @return The mapped RecaputilatifDecaissementItem DTO.
     */
    public RecaputilatifItem mapMiseADispositionToDto(MiseADisposition mad) {
        if (mad == null) {
            return null;
        }

        RecaputilatifItem dto = new RecaputilatifItem();
        dto.setDateEmission(mad.getDateEmission());
        dto.setDebit(mad.getMontant().setScale(SCALE, RoundingMode.HALF_UP));
        dto.setObservations("Mise à disposition N° " + mad.getNumero());
        dto.setLibelle("Bénéficiaire " + mad.getBeneficiaire() );
        dto.setSource(TransactionSource.MiseADisposition);
        return dto;
    }

    /**
     * Maps a Préfinancement entity to a RecaputilatifDecaissementItem DTO.
     *
     * @param prefinancement The Préfinancement entity to map.
     * @return The mapped RecaputilatifDecaissementItem DTO.
     */
    public RecaputilatifItem mapPrefinancementToDto(Prefinancement prefinancement) {

        if (prefinancement == null) {
            return null;
        }

        RecaputilatifItem dto = new RecaputilatifItem();
        dto.setDateEmission(prefinancement.getDateEmission());
        dto.setDebit(prefinancement.getMontant().setScale(SCALE, RoundingMode.HALF_UP));
        dto.setObservations("Remboursement du préfinancement" );
        dto.setLibelle("Marché N° : " + prefinancement.getMarche().getNumero() );
        dto.setSource(TransactionSource.PREFINANCEMENT);
        return dto;
    }

    /**
     * Maps a DroitConstate entity to a RecaputilatifDecaissementItem DTO.
     *
     * @param droitConstate The DroitConstate entity to map.
     * @return The mapped RecaputilatifDecaissementItem DTO.
     */
    public RecaputilatifItem mapDroitConstateToDto(DroitConstate droitConstate) {

        if (droitConstate == null) {
            return null;
        }

        RecaputilatifItem dto = new RecaputilatifItem();
        dto.setDateEmission(droitConstate.getDateEmission());
        dto.setDebit(droitConstate.getMontant().setScale(SCALE, RoundingMode.HALF_UP));
        dto.setObservations("Remboursement du droit constaté" );
        dto.setLibelle("Marché N° : " + droitConstate.getMarche().getNumero() );
        dto.setSource(TransactionSource.DROIT_CONSTATE);
        return dto;
    }


    /**
     * Maps a Factoring entity to a RecaputilatifDecaissementItem DTO.
     *
     * @param factoring The Factoring entity to map.
     * @return The mapped RecaputilatifDecaissementItem DTO.
     */
    public RecaputilatifItem mapFactoringToDto(Factoring factoring) {

        if (factoring == null) {
            return null;
        }

        RecaputilatifItem dto = new RecaputilatifItem();
        dto.setDateEmission(factoring.getDateEmission());
        dto.setDebit(factoring.getMontant().setScale(SCALE, RoundingMode.HALF_UP));
        dto.setObservations("Remboursement du factoring" );
        dto.setLibelle("Marché N° : " + factoring.getMarche().getNumero() );
        dto.setSource(TransactionSource.FACTORING);
        return dto;
    }


    /**
     * Maps a Commission entity to a RecaputilatifItem DTO.
     *
     * @param commission The Commission entity to map.
     * @return The mapped RecaputilatifItem DTO.
     */
    public RecaputilatifItem mapCommissionToDto(Commission commission) {

        if (commission == null) {
            return null;
        }

        RecaputilatifItem dto = new RecaputilatifItem();
        dto.setDateEmission(commission.getDateEmission());
        dto.setDebit(commission.getMontant().setScale(SCALE, RoundingMode.HALF_UP));
        dto.setObservations(commission.getObservation() );
        dto.setLibelle(commission.getLibelle());
        dto.setSource(TransactionSource.COMMISSION);
        return dto;
    }


    public RecaputilatifItem mapAgiosToDto(Agios agios) {

        if (agios == null) {
            return null;
        }

        RecaputilatifItem dto = new RecaputilatifItem();
        dto.setDateEmission(agios.getDateEmission());
        dto.setDebit(agios.getMontant().setScale(SCALE, RoundingMode.HALF_UP));
        dto.setObservations(agios.getObservation() );
        dto.setLibelle(agios.getLibelle());
        dto.setSource(TransactionSource.AGIOS);
        return dto;
    }

    public RecaputilatifItem mapPrelevementToDto(Prelevement prelevement) {

        if (prelevement == null) {
            return null;
        }

        RecaputilatifItem dto = new RecaputilatifItem();
        dto.setDateEmission(prelevement.getDateEmission());
        dto.setDebit(prelevement.getMontant().setScale(SCALE, RoundingMode.HALF_UP));
        dto.setObservations(prelevement.getObservation() );
        dto.setLibelle(prelevement.getLibelle());
        dto.setSource(TransactionSource.PRELEVEMENT);
        return dto;
    }


    public RecaputilatifItem mapFraisBancaireToDto(FraisBancaire fraisBancaire) {

        if (fraisBancaire == null) {
            return null;
        }

        RecaputilatifItem dto = new RecaputilatifItem();
        dto.setDateEmission(fraisBancaire.getDateEmission());
        dto.setDebit(fraisBancaire.getMontant().setScale(SCALE, RoundingMode.HALF_UP));
        dto.setObservations(fraisBancaire.getObservation() );
        dto.setLibelle(fraisBancaire.getLibelle());
        dto.setSource(TransactionSource.FRAIS_BANCAIRE);
        return dto;
    }

    public RecaputilatifItem mapInteretToDto(Interet interet) {

        if (interet == null) {
            return null;
        }

        RecaputilatifItem dto = new RecaputilatifItem();
        dto.setDateEmission(interet.getDateEmission());
        dto.setDebit(interet.getMontant().setScale(SCALE, RoundingMode.HALF_UP));
        dto.setObservations(interet.getObservation() );
        dto.setLibelle(interet.getLibelle());
        dto.setSource(TransactionSource.INTERETS);
        return dto;
    }



}