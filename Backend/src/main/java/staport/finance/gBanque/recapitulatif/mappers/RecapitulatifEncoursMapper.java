package staport.finance.gBanque.recapitulatif.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.recapitulatif.dtos.response.RecaputilatifItem;
import staport.finance.gBanque.recapitulatif.entities.RecapitulatifEncoursView;
import staport.finance.gBanque.transactions.enums.SensOperation;

/**
 * Remplace {@code RecapitulatifDecaissementMapper}/{@code RecapitulatifEncaissementMapper}
 * (17 méthodes manuelles à la main, une par entité source) par un mapper MapStruct unique :
 * la vue {@code v_recapitulatif_encours} a déjà uniformisé les 19 sources en un seul jeu de
 * colonnes, il ne reste qu'une projection directe.
 *
 * <p>{@code debit}/{@code credit} restent dérivés de {@code montant} + {@code sens} (et non des
 * colonnes séparées) pour préserver exactement le contrat JSON consommé par le frontend
 * (RecaputilatifItem.ts), qui distingue encore les deux colonnes dans ses tableaux.</p>
 *
 * <p>{@code sens} traduit l'enum legacy {@link Type} porté par la vue vers {@link SensOperation},
 * le type public du projet pour cette notion. Les deux enums déclarent les mêmes constantes
 * ({@code ENCAISSEMENT}, {@code DECAISSEMENT}) : la correspondance par nom suffit donc. La
 * traduction est écrite explicitement plutôt que laissée à la conversion automatique d'enums de
 * MapStruct, pour que le contrat exposé reste lisible directement dans ce fichier.</p>
 */
@Mapper(componentModel = "spring", imports = {Type.class, SensOperation.class})
public interface RecapitulatifEncoursMapper {

    @Mapping(target = "id", source = "entityId")
    @Mapping(target = "dateEcheance", source = "dateEcheanceAffichage")
    @Mapping(target = "observations", source = "observation")
    @Mapping(target = "source", source = "sourceType")
    @Mapping(target = "sens", expression = "java(SensOperation.valueOf(view.getSens().name()))")
    @Mapping(target = "debit", expression = "java(view.getSens() == Type.DECAISSEMENT ? view.getMontant() : null)")
    @Mapping(target = "credit", expression = "java(view.getSens() == Type.ENCAISSEMENT ? view.getMontant() : null)")
    RecaputilatifItem toDto(RecapitulatifEncoursView view);
}
