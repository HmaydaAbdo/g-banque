package staport.finance.gBanque.recapitulatif.mappers;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.decomptes.entities.Decompte;
import staport.finance.gBanque.droitConstate.entities.DroitConstate;
import staport.finance.gBanque.factoring.entities.Factoring;
import staport.finance.gBanque.prefinancement.entities.Prefinancement;
import staport.finance.gBanque.recapitulatif.dtos.RecaputilatifItem;
import staport.finance.gBanque.transactions.enums.TransactionSource;

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
        dto.setDateEmission(decompte.getDateEmission());
        dto.setCredit(decompte.getMontant().setScale(SCALE, RoundingMode.HALF_UP));
        dto.setObservations(decompte.getObservation());
        dto.setLibelle("Marché N° : " + decompte.getMarche().getNumero() );
        dto.setSource(TransactionSource.DECOMPTES);
        return dto;
    }


}
