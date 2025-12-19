package staport.finance.gBanque.leasing.echeancesLeasing.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.leasing.contratLeasing.config.LeasingProperties;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.dto.EcheanceLeasingResponseDTO;
import staport.finance.gBanque.leasing.echeancesLeasing.entities.EcheanceLeasing;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Mapper pour convertir entre l'entité EcheanceLeasing et EcheanceLeasingResponseDTO.
 */
@Component
public class EcheanceLeasingMapper {

    private static final Logger log = LoggerFactory.getLogger(EcheanceLeasingMapper.class);
    private final LeasingProperties leasingProperties;
    private static final int SCALE = 2;

    public EcheanceLeasingMapper(LeasingProperties leasingProperties) {
        this.leasingProperties = leasingProperties;
    }

    public EcheanceLeasingResponseDTO toDto(EcheanceLeasing entity) {
        log.debug("Début de la conversion de l'entité EcheanceLeasing vers le DTO");

        if (entity == null) {
            log.warn("L'entité EcheanceLeasing reçue pour la conversion est null");
            return null;
        }

        log.debug("Conversion de l'échéance numéro {} du contrat ID : {}", entity.getNumeroEcheance(), entity.getContratLeasing().getId());

        EcheanceLeasingResponseDTO dto = new EcheanceLeasingResponseDTO();
        dto.setId(entity.getId());
        dto.setContratLeasingId(entity.getContratLeasing().getId());
        dto.setNumeroContrat(entity.getContratLeasing().getNumeroContrat());
        dto.setNumeroEcheance(entity.getNumeroEcheance());
        dto.setDateEcheance(entity.getDateEcheance());
        dto.setMontantTTC(entity.getMontantTTC());

        // Calcul des montants HT et TVA comme défini dans les règles de gestion
        if (entity.getMontantTTC() != null) {
            log.debug("Calcul des montants HT et TVA pour le montant TTC : {}", entity.getMontantTTC());

            // Ht = (TTC / (1 + Taux TVA))
            BigDecimal montantHT = entity.getMontantTTC().divide((BigDecimal.ONE.add(leasingProperties.getTauxTva())), SCALE, RoundingMode.HALF_UP);
            // TVA = TTC - Ht
            BigDecimal montantTVA = entity.getMontantTTC().subtract(montantHT).setScale(SCALE, RoundingMode.HALF_UP);
            dto.setMontantHT(montantHT);
            dto.setMontantTVA(montantTVA);

            log.debug("Montants calculés - HT : {}, TVA : {}", montantHT, montantTVA);
        }

        // 2. Construire le libellé
        String libelle;
        ContratLeasing contrat = entity.getContratLeasing();
        // La dernière échéance est celle dont le numéro est égal à PeriodeEnMois + 1
        if (entity.getNumeroEcheance().equals(contrat.getPeriodeEnMois() + 1)) {
            libelle = contrat.getOrganismeLeasing().getNom() + ", V.R. pour le contrat N° " + contrat.getNumeroContrat();
            log.debug("Libellé généré pour la dernière échéance (V.R.) : {}", libelle);
        } else {
            libelle = contrat.getOrganismeLeasing().getNom() + ", contrat N° " + contrat.getNumeroContrat();
            log.debug("Libellé généré pour l'échéance standard : {}", libelle);
        }
        dto.setLibelle(libelle);
        dto.setObservation("Prélèvement leasing");
        dto.setStatutPaiement(entity.getStatutPaiement());
        dto.setDateOperation(entity.getDateOperation()!=null ? entity.getDateOperation(): null);
        dto.setDateValeur(entity.getDateValeur()!=null ? entity.getDateValeur(): null);
        dto.setDateCreation(entity.getDateCreation());
        dto.setDateModification(entity.getDateModification());

        log.debug("Conversion de l'entité EcheanceLeasing vers le DTO réussie pour l'échéance ID : {}", entity.getId());

        return dto;
    }
}