package staport.finance.gBanque.recapitulatif.mappers;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.impayees.entities.Impayee;
import staport.finance.gBanque.moyensDePaiements.cheques.entities.Cheque;
import staport.finance.gBanque.decomptes.entities.Decompte;
import staport.finance.gBanque.droitConstate.entities.DroitConstate;
import staport.finance.gBanque.factoring.entities.Factoring;
import staport.finance.gBanque.moyensDePaiements.lcn.dtos.NatureLcn;
import staport.finance.gBanque.moyensDePaiements.lcn.entities.Lcn;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.entities.MiseADisposition;
import staport.finance.gBanque.moyensDePaiements.virementInternational.entities.VirementInternational;
import staport.finance.gBanque.prefinancement.entities.Prefinancement;
import staport.finance.gBanque.recapitulatif.dtos.RecaputilatifItem;
import staport.finance.gBanque.restitution.entities.Restitution;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.moyensDePaiements.virements.entities.Virement;

import java.math.RoundingMode;

@Component
public class RecapitulatifEncaissementMapper {
    private static final int SCALE = 2;

    /**
     * Maps a Préfinancement entity to a RecaputilatifItem DTO.
     *
     * @param prefinancement The Préfinancement entity to map.
     * @return The mapped RecaputilatifItem DTO.
     */
    public RecaputilatifItem mapPrefinancementToDto(Prefinancement prefinancement) {

        if (prefinancement == null) {
            return null;
        }

        RecaputilatifItem dto = new RecaputilatifItem();
        dto.setId(prefinancement.getId());
        dto.setDateEmission(prefinancement.getDateEmission());
        dto.setCredit(prefinancement.getMontant().setScale(SCALE, RoundingMode.HALF_UP));
        dto.setObservations("Déblocage du préfinancement" );
        dto.setLibelle("Marché N° : " + prefinancement.getMarche().getNumero() );
        dto.setSource(TransactionSource.PREFINANCEMENT);
        return dto;
    }

    /**
     * Maps a Droit Constate entity to a RecaputilatifItem DTO.
     *
     * @param droitConstate The DroitConstate entity to map.
     * @return The mapped RecaputilatifItem DTO.
     */
    public RecaputilatifItem mapDroitConstateToDto(DroitConstate droitConstate) {

        if (droitConstate == null) {
            return null;
        }

        RecaputilatifItem dto = new RecaputilatifItem();
        dto.setId(droitConstate.getId());
        dto.setDateEmission(droitConstate.getDateEmission());
        dto.setCredit(droitConstate.getMontant().setScale(SCALE, RoundingMode.HALF_UP));
        dto.setObservations("Déblocage du droit constaté" );
        dto.setLibelle("Marché N° : " + droitConstate.getMarche().getNumero() );
        dto.setSource(TransactionSource.DROIT_CONSTATE);
        return dto;
    }


    /**
     * Maps a factoring  entity to a RecaputilatifItem DTO.
     *
     * @param factoring The Factoring entity to map.
     * @return The mapped RecaputilatifItem DTO.
     */
    public RecaputilatifItem mapFactoringToDto(Factoring factoring) {

        if (factoring == null) {
            return null;
        }

        RecaputilatifItem dto = new RecaputilatifItem();
        dto.setId(factoring.getId());
        dto.setDateEmission(factoring.getDateEmission());
        dto.setCredit(factoring.getMontant().setScale(SCALE, RoundingMode.HALF_UP));
        dto.setObservations("Déblocage du factoring" );
        dto.setLibelle("Marché N° : " + factoring.getMarche().getNumero() );
        dto.setSource(TransactionSource.FACTORING);
        return dto;
    }


    /**
     * Maps a Décompte entity to a RecaputilatifItem DTO.
     *
     * @param decompte The Decompte entity to map.
     * @return The mapped RecaputilatifItem DTO.
     */
    public RecaputilatifItem mapDecompteToDto(Decompte decompte) {

        if (decompte == null) {
            return null;
        }

        RecaputilatifItem dto = new RecaputilatifItem();
        dto.setId(decompte.getId());
        dto.setDateEmission(decompte.getDateEmission());
        dto.setCredit(decompte.getMontant().setScale(SCALE, RoundingMode.HALF_UP));
        dto.setObservations(decompte.getObservation());
        dto.setLibelle("Marché N° : " + decompte.getMarche().getNumero() );
        dto.setSource(TransactionSource.DECOMPTES);
        return dto;
    }


    public RecaputilatifItem mapVirementToDto(Virement virement) {
        if (virement == null) {
            return null;
        }

        RecaputilatifItem dto = new RecaputilatifItem();
        dto.setId(virement.getId());
        dto.setDateEmission(virement.getDateEmission());
        dto.setCredit(virement.getMontant().setScale(SCALE, RoundingMode.HALF_UP));
        dto.setObservations( virement.getEmetteur());
        dto.setLibelle( virement.getBeneficiaire() );
        dto.setSource(TransactionSource.Virement);
        return dto;
    }


    public RecaputilatifItem mapChequeToDto(Cheque cheque) {
        if (cheque == null) {
            return null;
        }

        RecaputilatifItem dto = new RecaputilatifItem();
        dto.setId(cheque.getId());
        dto.setDateEmission(cheque.getDateEmission());
        dto.setCredit(cheque.getMontant().setScale(SCALE, RoundingMode.HALF_UP));
        dto.setObservations("emetteur  " + cheque.getEmetteur());
        dto.setLibelle("Bénéficiaire " + cheque.getBeneficiaire() );
        dto.setSource(TransactionSource.CHEQUE);
        return dto;
    }

    public RecaputilatifItem mapLcnToDto(Lcn lcn) {
        if (lcn == null) {
            return null;
        }

        RecaputilatifItem dto = new RecaputilatifItem();
        dto.setId(lcn.getId());
        dto.setDateEmission(lcn.getDateEmission());
        dto.setCredit(lcn.getMontant().setScale(SCALE, RoundingMode.HALF_UP));
        dto.setObservations( lcn.getEmetteur());
        dto.setLibelle( lcn.getBeneficiaire() );
        TransactionSource source =
                (lcn.getNatureLcn() == NatureLcn.ENCAISSEMENT)
                        ? TransactionSource.LCN_ENCAISSEMENT
                        : TransactionSource.LCN_ESCOMPTE;
        dto.setSource(source);
        return dto;
    }

    public RecaputilatifItem mapRestitutionToDto(Restitution restitution) {
        if (restitution == null) {
            return null;
        }

        RecaputilatifItem dto = new RecaputilatifItem();
        dto.setId(restitution.getId());
        dto.setDateEmission(restitution.getDateEmission());
        dto.setCredit(restitution.getMontant().setScale(SCALE, RoundingMode.HALF_UP));
        dto.setObservations(restitution.getObservation());
        dto.setLibelle(restitution.getLibelle());
        dto.setSource(TransactionSource.RESTITUTION);
        return dto;
    }

    public RecaputilatifItem mapImpayeeToDto(Impayee impayee) {
        if (impayee == null) {
            return null;
        }

        RecaputilatifItem dto = new RecaputilatifItem();
        dto.setId(impayee.getId());
        dto.setDateEmission(impayee.getDateEmission());
        dto.setCredit(impayee.getMontant().setScale(SCALE, RoundingMode.HALF_UP));
        dto.setObservations(impayee.getObservation());
        dto.setLibelle(impayee.getLibelle());
        dto.setSource(TransactionSource.IMPAYEE);
        return dto;
    }



    public RecaputilatifItem mapVirementInternationalToDto(VirementInternational virement){
        if (virement == null) {
            return null;
        }

        RecaputilatifItem dto = new RecaputilatifItem();
        dto.setId(virement.getId());
        dto.setDateEmission(virement.getDateEmission());
        dto.setCredit(virement.getMontant().setScale(SCALE, RoundingMode.HALF_UP));
        dto.setObservations(virement.getObservation());
        dto.setLibelle(virement.getLibelle());
        dto.setSource(TransactionSource.VIREMENT_INTERNATIONAL);
        return dto;
    }

}
