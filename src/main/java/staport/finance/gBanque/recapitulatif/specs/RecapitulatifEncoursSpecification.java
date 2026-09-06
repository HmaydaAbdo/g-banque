package staport.finance.gBanque.recapitulatif.specs;

import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.recapitulatif.dtos.request.RecapitulatifSearchDto;
import staport.finance.gBanque.recapitulatif.entities.RecapitulatifEncoursView;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class RecapitulatifEncoursSpecification {

    private RecapitulatifEncoursSpecification() {
        // Classe utilitaire — pas d'instanciation
    }

    public static Specification<RecapitulatifEncoursView> byBanqueId(Long banqueId) {
        return (root, query, cb) ->
                banqueId == null ? cb.conjunction() : cb.equal(root.get("banqueId"), banqueId);
    }

    public static Specification<RecapitulatifEncoursView> bySens(Type sens) {
        return (root, query, cb) ->
                sens == null ? cb.conjunction() : cb.equal(root.get("sens"), sens);
    }

    public static Specification<RecapitulatifEncoursView> outstandingFrom(LocalDate from) {
        return (root, query, cb) ->
                from == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("dateReference"), from);
    }

    public static Specification<RecapitulatifEncoursView> outstandingUntil(LocalDate until) {
        return (root, query, cb) ->
                until == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("dateReference"), until);
    }

    public static Specification<RecapitulatifEncoursView> bySources(List<TransactionSource> sources) {
        return (root, query, cb) ->
                (sources == null || sources.isEmpty())
                        ? cb.conjunction()
                        : root.get("id").get("sourceType").in(sources);
    }

    public static Specification<RecapitulatifEncoursView> byLibelle(String libelle) {
        return (root, query, cb) ->
                (libelle == null || libelle.isBlank())
                        ? cb.conjunction()
                        : cb.like(cb.lower(root.get("libelle")), "%" + libelle.toLowerCase() + "%");
    }

    public static Specification<RecapitulatifEncoursView> byObservation(String observation) {
        return (root, query, cb) ->
                (observation == null || observation.isBlank())
                        ? cb.conjunction()
                        : cb.like(cb.lower(root.get("observation")), "%" + observation.toLowerCase() + "%");
    }

    /**
     * Filtre optionnel sur le montant exact (égalité stricte, pas une plage) — {@code null} ne
     * filtre pas.
     */
    public static Specification<RecapitulatifEncoursView> byMontant(BigDecimal montant) {
        return (root, query, cb) ->
                montant == null ? cb.conjunction() : cb.equal(root.get("montant"), montant);
    }

    /**
     * Combine les critères fixes (banque, sens, encours entre deux dates) avec les filtres
     * dynamiques optionnels de {@link RecapitulatifSearchDto} (sources, libellé, observation,
     * plage de montant).
     *
     * <p>Le sens est reçu ici sous sa forme legacy {@link Type} — celle portée par la vue SQL —
     * et non sous la forme {@code SensOperation} du DTO : la traduction est faite en amont par le
     * Service, pour que cette classe ne dépende que du modèle de persistance qu'elle interroge.
     * {@code null} signifie « aucun filtre de sens » : les deux sens sont alors retournés.</p>
     */
    public static Specification<RecapitulatifEncoursView> searchByCriteria(
            RecapitulatifSearchDto searchDto, Long banqueId, Type sens, LocalDate from, LocalDate until) {

        Specification<RecapitulatifEncoursView> spec = Specification
                .where(byBanqueId(banqueId))
                .and(bySens(sens))
                .and(outstandingFrom(from))
                .and(outstandingUntil(until));

        if (searchDto != null) {
            spec = spec.and(bySources(searchDto.transactionSources()))
                    .and(byLibelle(searchDto.libelle()))
                    .and(byObservation(searchDto.observation()))
                    .and(byMontant(searchDto.montant()));
        }

        return spec;
    }
}
