package staport.finance.gBanque.factoring.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import staport.finance.gBanque.factoring.dtos.request.FactoringRequestDto;
import staport.finance.gBanque.factoring.dtos.response.FactoringResponseDto;
import staport.finance.gBanque.factoring.dtos.response.SuivieFactoringLineResponseDto;
import staport.finance.gBanque.factoring.entities.Factoring;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.prefinancement.enums.Type;

/**
 * Mapper MapStruct pour le module Factoring (conventions/architecture.md).
 *
 * <p>{@code ligneFonctionnelleSpecifique}/{@code ligneFonctionnelle} sont résolues et settées par
 * le Service après recherche de la ligne active — jamais par le mapper (le mapper ne connaît
 * aucun Repository).
 */
@Mapper(componentModel = "spring")
public interface FactoringMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "marche", source = "marche")
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
    Factoring toEntity(FactoringRequestDto dto, Marche marche);

    @Mapping(target = "marcheId", source = "marche.id")
    @Mapping(target = "marcheName", source = "marche.nom")
    @Mapping(target = "marcheNumero", source = "marche.numero")
    @Mapping(target = "banqueId", source = "marche.banque.id")
    @Mapping(target = "banqueName", source = "marche.banque.nom")
    @Mapping(target = "observation", expression = "java(\"Marche N° \" + factoring.getMarche().getNumero())")
    @Mapping(target = "libelle", expression = "java(buildResponseLibelle(factoring))")
    FactoringResponseDto toDto(Factoring factoring);

    @Mapping(target = "banqueName", source = "marche.banque.nom")
    @Mapping(target = "marcheName", source = "marche.nom")
    @Mapping(target = "marcheNumero", source = "marche.numero")
    @Mapping(target = "libelle", expression = "java(buildSuivieLibelle(factoring))")
    @Mapping(target = "debit", expression = "java(factoring.getType() == Type.REMBOURSEMENT ? factoring.getMontant() : null)")
    @Mapping(target = "credit", expression = "java(factoring.getType() == Type.DEBLOCAGE ? factoring.getMontant() : null)")
    SuivieFactoringLineResponseDto toSuivieFactoringLigneResponseDto(Factoring factoring);

    default String buildResponseLibelle(Factoring factoring) {
        if (factoring.getMarche() == null) {
            return null;
        }
        String marcheNom = factoring.getMarche().getNom();
        return factoring.getType() == Type.DEBLOCAGE
                ? "Déblocage factoring marché " + marcheNom
                : "Remboursement factoring marché " + marcheNom;
    }

    default String buildSuivieLibelle(Factoring factoring) {
        if (factoring.getMarche() == null) {
            return null;
        }
        String marcheNom = factoring.getMarche().getNom();
        return factoring.getType() == Type.DEBLOCAGE
                ? "Déblocage du factoring pour le marché " + marcheNom
                : "Remboursement du factoring pour le marché " + marcheNom;
    }
}
