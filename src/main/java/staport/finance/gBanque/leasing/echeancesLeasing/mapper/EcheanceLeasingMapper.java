package staport.finance.gBanque.leasing.echeancesLeasing.mapper;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
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
 * et calculer montantHT, montantTVA et libellé via @AfterMapping.
 */
@Mapper(componentModel = "spring")
public abstract class EcheanceLeasingMapper {

    @Autowired
    protected LeasingProperties leasingProperties;

    private static final int SCALE = 2;

    // ── toDto ─────────────────────────────────────────────────────────────────

    @Mapping(target = "contratLeasingId", source = "contratLeasing.id")
    @Mapping(target = "numeroContrat",    source = "contratLeasing.numeroContrat")
    @Mapping(target = "montantHT",        ignore = true)
    @Mapping(target = "montantTVA",       ignore = true)
    @Mapping(target = "libelle",          ignore = true)
    @Mapping(target = "observation",      ignore = true)
    public abstract EcheanceLeasingResponseDTO toDto(EcheanceLeasing entity);

    // ── @AfterMapping — calcul HT/TVA, libellé, observation ──────────────────

    /**
     * Calcule les montants HT et TVA depuis le montant TTC,
     * construit le libellé (standard ou valeur résiduelle)
     * et fixe l'observation.
     */
    @AfterMapping
    protected void computeCalculatedFields(EcheanceLeasing entity,
                                            @MappingTarget EcheanceLeasingResponseDTO dto) {
        // Montants HT et TVA
        if (entity.getMontantTTC() != null) {
            BigDecimal montantHT = entity.getMontantTTC()
                    .divide(BigDecimal.ONE.add(leasingProperties.getTauxTva()), SCALE, RoundingMode.HALF_UP);
            BigDecimal montantTVA = entity.getMontantTTC().subtract(montantHT)
                    .setScale(SCALE, RoundingMode.HALF_UP);
            dto.setMontantHT(montantHT);
            dto.setMontantTVA(montantTVA);
        }

        // Libellé : valeur résiduelle si dernière échéance (numéro = périodeEnMois + 1)
        ContratLeasing contrat = entity.getContratLeasing();
        String libelle;
        if (entity.getNumeroEcheance().equals(contrat.getPeriodeEnMois() + 1)) {
            libelle = contrat.getOrganismeLeasing().getNom()
                    + ", V.R. pour le contrat N° " + contrat.getNumeroContrat();
        } else {
            libelle = contrat.getOrganismeLeasing().getNom()
                    + ", contrat N° " + contrat.getNumeroContrat();
        }
        dto.setLibelle(libelle);
        dto.setObservation("Prélèvement leasing");
    }
}
