package staport.finance.gBanque.recapitulatif.repositories;

import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.recapitulatif.dtos.response.SourceRecapDto;
import staport.finance.gBanque.recapitulatif.entities.RecapitulatifEncoursView;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Agrégation (SUM) sur {@link RecapitulatifEncoursView} respectant une {@link Specification}
 * arbitraire — nécessaire car {@code JpaSpecificationExecutor} ne fournit pas d'agrégat natif,
 * et les filtres dynamiques de {@code RecapitulatifSearchDto} (sources, libellé/observation,
 * sens) doivent être reflétés dans les totaux, pas seulement dans la page retournée.
 */
public interface RecapitulatifEncoursRepositoryCustom {

    /**
     * Somme des montants groupée par sens, pour la {@link Specification} donnée.
     *
     * <p>Une seule requête couvre les deux totaux affichés côte à côte par l'écran Récapitulatif
     * depuis la fusion des deux écrans (06/09/2026), y compris quand aucun filtre de sens n'est
     * appliqué et que les deux sens sont donc présents dans le résultat.</p>
     *
     * <p>Un sens absent du résultat — aucune ligne correspondante, ou sens exclu par la
     * Specification elle-même lorsque l'utilisateur filtre sur un sens précis — est simplement
     * absent de la Map : au consommateur d'appliquer {@code getOrDefault(..., BigDecimal.ZERO)}.
     * C'est ce qui fait que le total du sens non demandé vaut zéro par construction, sans
     * traitement conditionnel dans le Service.</p>
     */
    Map<Type, BigDecimal> sumMontantParSens(Specification<RecapitulatifEncoursView> spec);

    /**
     * Indicateurs par source pour la {@link Specification} donnée : nombre d'encours, total des
     * décaissements, total des encaissements et nombre d'encours dont la date de référence est
     * <= {@code today}. Une seule requête GROUP BY ; les sources sans encours sont absentes.
     */
    List<SourceRecapDto> aggregateBySource(Specification<RecapitulatifEncoursView> spec, LocalDate today);
}
