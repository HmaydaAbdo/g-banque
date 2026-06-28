package staport.finance.gBanque.leasing.echeancesLeasing.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;
import staport.finance.gBanque.leasing.contratLeasing.config.LeasingProperties;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.dto.EcheanceLeasingResponseDTO;
import staport.finance.gBanque.leasing.echeancesLeasing.entities.EcheanceLeasing;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Mapper MapStruct pour EcheanceLeasing → EcheanceLeasingResponseDTO.
 * Classe abstraite nécessaire pour injecter LeasingProperties (taux TVA)
 * et calculer montantHT, montantTVA et libellé via des méthodes protégées.
 */
@Mapper(componentModel = "spring")
public abstract class EcheanceLeasingMapper {

    @Autowired
    protected LeasingProperties leasingProperties;

    private static final int SCALE = 2;

    // ── toDto ─────────────────────────────────────────────────────────────────

    @Mapping(target = "contratLeasingId", source = "contratLeasing.id")
    @Mapping(target = "numeroContrat",    source = "contratLeasing.numeroContrat")
    @Mapping(target = "montantHT",        expression = "java(computeMontantHT(entity))")
    @Mapping(target = "montantTVA",       expression = "java(computeMontantTVA(entity))")
    @Mapping(target = "libelle",          expression = "java(computeLibelle(entity))")
    @Mapping(target = "observation",      constant = "Prélèvement leasing")
    public abstract EcheanceLeasingResponseDTO toDto(EcheanceLeasing entity);

    // ── Méthodes de calcul ────────────────────────────────────────────────────

    protected BigDecimal computeMontantHT(EcheanceLeasing entity) {
        if (entity.getMontantTTC() == null) return null;
        return entity.getMontantTTC()
                .divide(BigDecimal.ONE.add(leasingProperties.getTauxTva()), SCALE, RoundingMode.HALF_UP);
    }

    protected BigDecimal computeMontantTVA(EcheanceLeasing entity) {
        if (entity.getMontantTTC() == null) return null;
        BigDecimal montantHT = computeMontantHT(entity);
        return entity.getMontantTTC().subtract(montantHT).setScale(SCALE, RoundingMode.HALF_UP);
    }

    protected String computeLibelle(EcheanceLeasing entity) {
        ContratLeasing contrat = entity.getContratLeasing();
        if (entity.getNumeroEcheance().equals(contrat.getPeriodeEnMois() + 1)) {
            return contrat.getOrganismeLeasing().getNom()
                    + ", V.R. pour le contrat N° " + contrat.getNumeroContrat();
        }
        return contrat.getOrganismeLeasing().getNom()
                + ", contrat N° " + contrat.getNumeroContrat();
    }
}
