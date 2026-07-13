package staport.finance.gBanque.prefinancement.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.prefinancement.dtos.request.PrefinancementRequestDTO;
import staport.finance.gBanque.prefinancement.dtos.response.PrefinancementResponseDTO;
import staport.finance.gBanque.prefinancement.dtos.response.SuiviePrefiLigneResponseDto;
import staport.finance.gBanque.prefinancement.entities.Prefinancement;
import staport.finance.gBanque.prefinancement.enums.Type;

/**
 * Mapper MapStruct pour le module Préfinancement (conventions/architecture.md).
 *
 * <p>{@code ligneFonctionnelleSpecifique}/{@code ligneFonctionnelle} sont résolues et settées par
 * le Service après recherche de la ligne active — jamais par le mapper (le mapper ne connaît
 * aucun Repository).
 */
@Mapper(componentModel = "spring")
public interface PrefinancementMapper {

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
    Prefinancement toEntity(PrefinancementRequestDTO dto, Marche marche);

    @Mapping(target = "marcheId", source = "marche.id")
    @Mapping(target = "marcheName", source = "marche.nom")
    @Mapping(target = "marcheNumero", source = "marche.numero")
    @Mapping(target = "banqueId", source = "marche.banque.id")
    @Mapping(target = "banqueName", source = "marche.banque.nom")
    @Mapping(target = "observation", expression = "java(\"Marche N° \" + prefinancement.getMarche().getNumero())")
    @Mapping(target = "libelle", expression = "java(buildResponseLibelle(prefinancement))")
    PrefinancementResponseDTO toDto(Prefinancement prefinancement);

    @Mapping(target = "banqueName", source = "marche.banque.nom")
    @Mapping(target = "marcheName", source = "marche.nom")
    @Mapping(target = "marcheNumero", source = "marche.numero")
    @Mapping(target = "libelle", expression = "java(buildSuivieLibelle(prefinancement))")
    @Mapping(target = "debit", expression = "java(prefinancement.getType() == Type.REMBOURSEMENT ? prefinancement.getMontant() : null)")
    @Mapping(target = "credit", expression = "java(prefinancement.getType() == Type.DEBLOCAGE ? prefinancement.getMontant() : null)")
    SuiviePrefiLigneResponseDto toSuiviePrefiLigneResponseDto(Prefinancement prefinancement);

    default String buildResponseLibelle(Prefinancement prefinancement) {
        if (prefinancement.getMarche() == null) {
            return null;
        }
        String marcheNom = prefinancement.getMarche().getNom();
        return prefinancement.getType() == Type.DEBLOCAGE
                ? "Déblocage  préfinancement  marché " + marcheNom
                : "Remboursement  préfinancement  marché " + marcheNom;
    }

    default String buildSuivieLibelle(Prefinancement prefinancement) {
        if (prefinancement.getMarche() == null) {
            return null;
        }
        String marcheNom = prefinancement.getMarche().getNom();
        return prefinancement.getType() == Type.DEBLOCAGE
                ? "Déblocage du préfinancement du marché " + marcheNom
                : "Remboursement du préfinancement du marché " + marcheNom;
    }
}
