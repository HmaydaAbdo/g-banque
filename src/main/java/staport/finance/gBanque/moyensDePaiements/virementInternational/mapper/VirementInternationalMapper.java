package staport.finance.gBanque.moyensDePaiements.virementInternational.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;
import staport.finance.gBanque.moyensDePaiements.virementInternational.dtos.VirementInternationalRequestDto;
import staport.finance.gBanque.moyensDePaiements.virementInternational.dtos.VirementInternationalResponseDto;
import staport.finance.gBanque.moyensDePaiements.virementInternational.entities.VirementInternational;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;

/**
 * Mapper MapStruct du module Virement International.
 *
 * <p>{@code toEntity}/{@code updateEntity} restent des méthodes par défaut : la résolution de la
 * banque est une opération métier (via {@code IBanqueService}) qui reste dans le Service — le
 * mapper ne fait que construire/mettre à jour l'entité à partir d'une {@link Banque} déjà résolue
 * (architecture.md — un Mapper ne fait pas d'accès base de données).
 */
@Mapper(componentModel = "spring")
public interface VirementInternationalMapper {

    @Mapping(target = "banqueId", source = "banque.id")
    @Mapping(target = "banqueNom", source = "banque.nom")
    VirementInternationalResponseDto toDto(VirementInternational virementInternational);

    /**
     * Construit une nouvelle entité à l'état ENCOURS. Libellé et observation sont générés
     * automatiquement selon le type (ENCAISSEMENT/DECAISSEMENT) — comportement identique à
     * l'ancien mapper manuel.
     */
    default VirementInternational toEntity(VirementInternationalRequestDto dto, Banque banque) {
        VirementInternational virement = new VirementInternational();
        virement.setBanque(banque);
        virement.setBeneficiaire(dto.getBeneficiaire());
        virement.setEmetteur(dto.getEmetteur());
        virement.setMontant(dto.getMontant());
        virement.setDateEmission(dto.getDateEmission());
        virement.setType(dto.getType());

        String libelleEtObservation = genererLibelleEtObservation(dto.getType(), dto.getEmetteur(), dto.getBeneficiaire(), dto.getMontant());
        virement.setLibelle(libelleEtObservation);
        virement.setObservation(libelleEtObservation);
        virement.setStatut(StatutMoyenPaiement.ENCOURS);

        return virement;
    }

    /**
     * Met à jour une entité existante. {@code banque} est la banque déjà résolue par le Service
     * (nouvelle banque si {@code dto.getBanqueId()} a changé, sinon la banque actuelle).
     */
    default VirementInternational updateEntity(VirementInternational virement, VirementInternationalRequestDto dto, Banque banque) {
        virement.setBanque(banque);
        virement.setBeneficiaire(dto.getBeneficiaire());
        virement.setEmetteur(dto.getEmetteur());
        virement.setMontant(dto.getMontant());
        virement.setDateEmission(dto.getDateEmission());
        virement.setType(dto.getType());

        String libelleEtObservation = genererLibelleEtObservation(dto.getType(), dto.getEmetteur(), dto.getBeneficiaire(), dto.getMontant());
        virement.setLibelle(libelleEtObservation);
        virement.setObservation(libelleEtObservation);

        return virement;
    }

    private String genererLibelleEtObservation(Type type, String emetteur, String beneficiaire, java.math.BigDecimal montant) {
        return type == Type.ENCAISSEMENT
                ? "Encaissement de " + emetteur + " - Montant: " + montant
                : "Décaissement vers " + beneficiaire + " - Montant: " + montant;
    }
}
