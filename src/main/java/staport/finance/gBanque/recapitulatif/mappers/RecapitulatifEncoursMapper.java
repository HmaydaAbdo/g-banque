package staport.finance.gBanque.recapitulatif.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.recapitulatif.dtos.response.RecaputilatifItem;
import staport.finance.gBanque.recapitulatif.entities.RecapitulatifEncoursView;

/**
 * Remplace {@code RecapitulatifDecaissementMapper}/{@code RecapitulatifEncaissementMapper}
 * (17 méthodes manuelles à la main, une par entité source) par un mapper MapStruct unique :
 * la vue {@code v_recapitulatif_encours} a déjà uniformisé les 19 sources en un seul jeu de
 * colonnes, il ne reste qu'une projection directe.
 *
 * <p>{@code debit}/{@code credit} restent dérivés de {@code montant} + {@code sens} (et non des
 * colonnes séparées) pour préserver exactement le contrat JSON consommé par le frontend
 * (RecaputilatifItem.ts), qui distingue encore les deux colonnes dans ses tableaux.</p>
 */
@Mapper(componentModel = "spring", imports = Type.class)
public interface RecapitulatifEncoursMapper {

    @Mapping(target = "id", source = "entityId")
    @Mapping(target = "dateEcheance", source = "dateEcheanceAffichage")
    @Mapping(target = "observations", source = "observation")
    @Mapping(target = "source", source = "sourceType")
    @Mapping(target = "debit", expression = "java(view.getSens() == Type.DECAISSEMENT ? view.getMontant() : null)")
    @Mapping(target = "credit", expression = "java(view.getSens() == Type.ENCAISSEMENT ? view.getMontant() : null)")
    RecaputilatifItem toDto(RecapitulatifEncoursView view);
}
