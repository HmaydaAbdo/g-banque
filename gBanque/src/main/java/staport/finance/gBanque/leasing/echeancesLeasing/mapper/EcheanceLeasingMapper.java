package staport.finance.gBanque.leasing.echeancesLeasing.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.leasing.echeancesLeasing.dto.EcheanceLeasingResponseDTO;
import staport.finance.gBanque.leasing.echeancesLeasing.entities.EcheanceLeasing;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Mapper pour convertir entre l'entité EcheanceLeasing et EcheanceLeasingResponseDTO.
 */
@Component
public class EcheanceLeasingMapper {

    private static final BigDecimal TVA_RATE_DIVISOR = new BigDecimal("1.20"); // Pour calculer HT à partir de TTC
    private static final int SCALE = 2; // Deux décimales pour les montants

    public EcheanceLeasingResponseDTO toDto(EcheanceLeasing entity) {
        if (entity == null) {
            return null;
        }

        EcheanceLeasingResponseDTO dto = new EcheanceLeasingResponseDTO();
        dto.setId(entity.getId());
        // Prévention de la LazyInitializationException si le contrat n'est pas chargé
        // Assurez-vous que le contrat est chargé (e.g., via fetch = FetchType.EAGER ou une requête join fetch)
        // si vous avez besoin de son ID ou de son numéro de contrat.
        // Dans le contexte du tableau d'amortissement, le service s'assure que le contrat est accessible.
        if (entity.getContratLeasing() != null) {
            dto.setContratLeasingId(entity.getContratLeasing().getId());
            dto.setNumeroContrat(entity.getContratLeasing().getNumeroContrat());
            // Si vous avez besoin du numéro de contrat de l'échéance individuelle (peu courant),
            // assurez-vous qu'il est chargé ou utilisez une jointure appropriée dans le repository
            // dto.setNumeroContrat(entity.getContratLeasing().getNumeroContrat());
        } else {
            dto.setContratLeasingId(null);
        }

        dto.setNumeroEcheance(entity.getNumeroEcheance());
        dto.setDateEcheance(entity.getDateEcheance());
        dto.setMontantTTC(entity.getMontantTTC());

        // Calcul des montants HT et TVA comme défini dans les règles de gestion (RG-ECH-008)
        if (entity.getMontantTTC() != null) {
            BigDecimal montantHT = entity.getMontantTTC().divide(TVA_RATE_DIVISOR, SCALE, RoundingMode.HALF_UP);
            BigDecimal montantTVA = entity.getMontantTTC().subtract(montantHT).setScale(SCALE, RoundingMode.HALF_UP);
            dto.setMontantHT(montantHT);
            dto.setMontantTVA(montantTVA);
        }

        dto.setStatutPaiement(entity.getStatutPaiement());
        dto.setDateCreation(entity.getDateCreation());
        dto.setDateModification(entity.getDateModification());

        return dto;
    }
}