package staport.finance.gBanque.leasing.echeancesLeasing.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.leasing.contratLeasing.config.LeasingProperties;
import staport.finance.gBanque.leasing.echeancesLeasing.dto.EcheanceLeasingResponseDTO;
import staport.finance.gBanque.leasing.echeancesLeasing.entities.EcheanceLeasing;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Mapper pour convertir entre l'entité EcheanceLeasing et EcheanceLeasingResponseDTO.
 */
@Component
public class EcheanceLeasingMapper {


    private final LeasingProperties leasingProperties;
    private static final int SCALE = 2;

    public EcheanceLeasingMapper(LeasingProperties leasingProperties) {
        this.leasingProperties = leasingProperties;
    }

    public EcheanceLeasingResponseDTO toDto(EcheanceLeasing entity) {
        if (entity == null) {
            return null;
        }

        EcheanceLeasingResponseDTO dto = new EcheanceLeasingResponseDTO();
        dto.setId(entity.getId());
        dto.setContratLeasingId(entity.getContratLeasing().getId());
        dto.setNumeroContrat(entity.getContratLeasing().getNumeroContrat());
        dto.setNumeroEcheance(entity.getNumeroEcheance());
        dto.setDateEcheance(entity.getDateEcheance());
        dto.setMontantTTC(entity.getMontantTTC());

        // Calcul des montants HT et TVA comme défini dans les règles de gestion (RG-ECH-008)
        if (entity.getMontantTTC() != null) {
            // Ht = (TTC / (1 + Taux TVA))
            BigDecimal montantHT = entity.getMontantTTC().divide((BigDecimal.ONE.add(leasingProperties.getTauxTva())), SCALE, RoundingMode.HALF_UP);
            // TVA = TTC - Ht
            BigDecimal montantTVA = entity.getMontantTTC().subtract(montantHT).setScale(SCALE, RoundingMode.HALF_UP);
            dto.setMontantHT(montantHT);
            dto.setMontantTVA(montantTVA);
        }

        dto.setStatutPaiement(entity.getStatutPaiement());
        dto.setDateOperation(entity.getDateOperation()!=null ? entity.getDateOperation(): null);
        dto.setDateValeur(entity.getDateValeur()!=null ? entity.getDateValeur(): null);
        dto.setDateCreation(entity.getDateCreation());
        dto.setDateModification(entity.getDateModification());

        return dto;
    }
}