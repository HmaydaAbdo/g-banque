package staport.finance.gBanque.moyensDePaiements.virements.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.moyensDePaiements.virements.dtos.VirementRequestDto;
import staport.finance.gBanque.moyensDePaiements.virements.dtos.VirementResponseDto;
import staport.finance.gBanque.moyensDePaiements.virements.entities.Virement;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;

/**
 * Mapper MapStruct pour le module Virement — conversion uniquement, aucune logique métier
 * (architecture.md, section Mapper).
 *
 * <p>{@code libelle}/{@code observation} du DTO sont des libellés d'affichage calculés, et
 * {@code banqueBeneficiaire} dépend d'un choix entre banque référencée et nom libre — ces trois
 * champs sont appliqués par {@code VirementService} après le mapping, pas ici.
 *
 * <p>{@code observation} de l'entité est en revanche renseigné dès {@link #toEntity} avec la même
 * convention (" VIREMENT N° ...") que celle utilisée pour l'affichage, afin que le champ ne soit
 * jamais {@code null} lors d'une validation en lot (voir {@code BatchValidationService}).
 */
@Mapper(componentModel = "spring")
public interface VirementMapper {

    @Mapping(target = "banqueEmettriceNom",  source = "banqueEmettrice.nom")
    @Mapping(target = "banqueEmettriceId",   source = "banqueEmettrice.id")
    @Mapping(target = "banqueBeneficiaireId", source = "banqueBeneficiare.id")
    @Mapping(target = "banqueBeneficiaire",  ignore = true)
    @Mapping(target = "libelle",             ignore = true)
    @Mapping(target = "observation",         ignore = true)
    VirementResponseDto toDto(Virement virement);

    /**
     * Crée une nouvelle entité virement d'encaissement à partir du DTO de requête.
     * La banque bénéficiaire est résolue et injectée par le service ; le statut initial est ENCOURS.
     */
    default Virement toEntity(VirementRequestDto dto, Banque banqueBeneficiaire) {
        if (dto == null || banqueBeneficiaire == null) {
            return null;
        }
        Virement virement = new Virement();
        virement.setBanqueBeneficiare(banqueBeneficiaire);
        virement.setDateEmission(dto.getDateReception());
        virement.setBeneficiaire(dto.getBeneficiaire());
        virement.setMontant(dto.getMontant());
        virement.setEmetteur(dto.getEmetteur());
        virement.setType(dto.getType());
        virement.setStatut(StatutMoyenPaiement.ENCOURS);
        virement.setObservation(" VIREMENT N° " + virement.getNumero());
        return virement;
    }
}
