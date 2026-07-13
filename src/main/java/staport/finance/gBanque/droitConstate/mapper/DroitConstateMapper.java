package staport.finance.gBanque.droitConstate.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.droitConstate.dtos.request.DroitConstateRequestDto;
import staport.finance.gBanque.droitConstate.dtos.response.DroitConstateResponseDto;
import staport.finance.gBanque.droitConstate.dtos.response.SuivieDcLigneResponseDto;
import staport.finance.gBanque.droitConstate.entities.DroitConstate;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.prefinancement.enums.Type;

/**
 * Mapper MapStruct pour le module Droit Constaté (conventions/architecture.md).
 *
 * <p>{@code ligneFonctionnelleSpecifique}/{@code ligneFonctionnelle} sont résolues et settées par
 * le Service après recherche de la ligne active — jamais par le mapper (le mapper ne connaît
 * aucun Repository). {@code banque} est un champ direct de l'entité (indépendant de
 * {@code marche.banque}), résolue par le Service via {@code IBanqueService} et passée déjà
 * chargée.
 */
@Mapper(componentModel = "spring")
public interface DroitConstateMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "marche", source = "marche")
    @Mapping(target = "banque", source = "banque")
    @Mapping(target = "montant", source = "dto.montant")
    @Mapping(target = "dateEmission", source = "dto.dateEmission")
    @Mapping(target = "type", source = "dto.type")
    @Mapping(target = "ligneFonctionnelleSpecifique", ignore = true)
    @Mapping(target = "ligneFonctionnelle", ignore = true)
    @Mapping(target = "status", constant = "PREVU")
    @Mapping(target = "dateOperation", ignore = true)
    @Mapping(target = "dateValeur", ignore = true)
    @Mapping(target = "libelle", ignore = true)
    @Mapping(target = "observation", source = "marche.numero")
    DroitConstate toEntity(DroitConstateRequestDto dto, Marche marche, Banque banque);

    @Mapping(target = "marcheId", source = "marche.id")
    @Mapping(target = "marcheName", source = "marche.nom")
    @Mapping(target = "marcheNumero", source = "marche.numero")
    @Mapping(target = "banqueId", source = "banque.id")
    @Mapping(target = "banqueName", source = "banque.nom")
    @Mapping(target = "observation", expression = "java(\"Marche N° \" + droitConstate.getMarche().getNumero())")
    @Mapping(target = "libelle", expression = "java(buildResponseLibelle(droitConstate))")
    DroitConstateResponseDto toDto(DroitConstate droitConstate);

    @Mapping(target = "banqueName", source = "banque.nom")
    @Mapping(target = "marcheName", source = "marche.nom")
    @Mapping(target = "marcheNumero", source = "marche.numero")
    @Mapping(target = "libelle", expression = "java(buildSuivieLibelle(droitConstate))")
    @Mapping(target = "debit", expression = "java(droitConstate.getType() == Type.REMBOURSEMENT ? droitConstate.getMontant() : null)")
    @Mapping(target = "credit", expression = "java(droitConstate.getType() == Type.DEBLOCAGE ? droitConstate.getMontant() : null)")
    SuivieDcLigneResponseDto toSuivieDcLigneResponseDto(DroitConstate droitConstate);

    default String buildResponseLibelle(DroitConstate droitConstate) {
        String marcheNom = droitConstate.getMarche() != null ? droitConstate.getMarche().getNom() : null;
        return droitConstate.getType() == Type.DEBLOCAGE
                ? "Déblocage  droit constaté  marché " + marcheNom
                : "Remboursement  droit constaté  marché " + marcheNom;
    }

    default String buildSuivieLibelle(DroitConstate droitConstate) {
        String marcheNom = droitConstate.getMarche() != null ? droitConstate.getMarche().getNom() : null;
        return droitConstate.getType() == Type.DEBLOCAGE
                ? "Déblocage du Droit constaté du marché " + marcheNom
                : "Remboursement du Droit constaté du marché " + marcheNom;
    }
}
