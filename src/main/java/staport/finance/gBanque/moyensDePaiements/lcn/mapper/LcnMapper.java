package staport.finance.gBanque.moyensDePaiements.lcn.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.moyensDePaiements.lcn.dtos.LcnRequestDto;
import staport.finance.gBanque.moyensDePaiements.lcn.dtos.LcnResponseDTO;
import staport.finance.gBanque.moyensDePaiements.lcn.entities.Lcn;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;

/**
 * Mapper MapStruct pour le module LCN — conversion uniquement, aucune logique métier
 * (architecture.md, section Mapper).
 *
 * <p>{@code libelle}/{@code observation} du DTO sont des libellés d'affichage calculés
 * (différents des champs {@code libelle}/{@code observation} de l'entité, alimentés par la
 * validation/rapprochement) — cette règle d'affichage est appliquée par {@code LcnService}
 * après le mapping, pas ici.
 */
@Mapper(componentModel = "spring")
public interface LcnMapper {

    @Mapping(target = "banqueName",  source = "banque.nom")
    @Mapping(target = "banqueId",    source = "banque.id")
    @Mapping(target = "libelle",     ignore = true)
    @Mapping(target = "observation", ignore = true)
    LcnResponseDTO toDto(Lcn lcn);

    /**
     * Crée une nouvelle entité LCN à partir du DTO de requête.
     * La banque et, le cas échéant, la ligne fonctionnelle escompte sont résolues et injectées
     * par le service ; le statut initial est ENCOURS.
     */
    default Lcn toEntity(LcnRequestDto dto, Banque banque) {
        if (dto == null || banque == null) {
            return null;
        }
        Lcn lcn = new Lcn();
        lcn.setDateEmission(dto.getDateEmission());
        lcn.setDateEcheance(dto.getDateEcheance());
        lcn.setBeneficiaire(dto.getBeneficiaire());
        lcn.setMontant(dto.getMontant());
        lcn.setBanque(banque);
        lcn.setStatut(StatutMoyenPaiement.ENCOURS);
        lcn.setNumero(dto.getNumero());
        lcn.setType(dto.getType());
        lcn.setEmetteur(dto.getEmetteur());
        lcn.setNatureLcn(dto.getNatureLcn());
        return lcn;
    }
}
