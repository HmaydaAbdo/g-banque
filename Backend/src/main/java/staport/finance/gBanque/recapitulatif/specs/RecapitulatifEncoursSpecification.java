package staport.finance.gBanque.recapitulatif.specs;

import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.impot_taxes.enums.Motif;
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

    public static Specification<RecapitulatifEncoursView> byNumero(String numero) {
        return (root, query, cb) ->
                (numero == null || numero.isBlank())
                        ? cb.conjunction()
                        : cb.like(cb.lower(root.get("numero")), "%" + numero.toLowerCase() + "%");
    }

    /**
     * Un seul terme testé sur {@code libelle}, {@code observation} OU {@code
     * rechercheComplementaire} (sous-chaîne, insensible à la casse) — fusion du 21/09/2026 de deux
     * filtres auparavant combinés en AND sur des colonnes distinctes, l'utilisateur ne sachant
     * souvent pas laquelle des deux contient le terme cherché. {@code rechercheComplementaire}
     * (colonne invisible, voir {@code v_recapitulatif_encours.sql}) restaure des critères des
     * anciens écrans par source perdus par la vue selon le sens (émetteur, organisme Leasing).
     */
    public static Specification<RecapitulatifEncoursView> byLibelleOuObservation(String terme) {
        return (root, query, cb) -> {
            if (terme == null || terme.isBlank()) {
                return cb.conjunction();
            }
            String pattern = "%" + terme.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("libelle")), pattern),
                    cb.like(cb.lower(root.get("observation")), pattern),
                    cb.like(cb.lower(root.get("rechercheComplementaire")), pattern));
        };
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
     * Filtre optionnel exact sur le motif (type) d'une Commission — ne s'applique qu'aux lignes
     * COMMISSION (une ligne d'une autre source n'est jamais exclue par ce critère), restaurant le
     * filtre « Motif » de l'ancien écran dédié (21/09/2026).
     */
    public static Specification<RecapitulatifEncoursView> byCommissionOperation(TransactionSource operation) {
        return (root, query, cb) ->
                operation == null
                        ? cb.conjunction()
                        : cb.or(
                                cb.notEqual(root.get("id").get("sourceType"), TransactionSource.COMMISSION),
                                cb.equal(root.get("commissionOperation"), operation.name()));
    }

    /**
     * Filtre optionnel exact sur le motif d'un Impôt/Taxe — ne s'applique qu'aux lignes
     * IMPOT_TAXE (une ligne d'une autre source n'est jamais exclue par ce critère), restaurant le
     * filtre « Motif » de l'ancien écran dédié (21/09/2026).
     */
    public static Specification<RecapitulatifEncoursView> byImpotEtTaxeMotif(Motif motif) {
        return (root, query, cb) ->
                motif == null
                        ? cb.conjunction()
                        : cb.or(
                                cb.notEqual(root.get("id").get("sourceType"), TransactionSource.IMPOT_TAXE),
                                cb.equal(root.get("impotTaxeMotif"), motif.name()));
    }

    /**
     * Combine les critères fixes (banque, sens, encours entre deux dates) avec les filtres
     * dynamiques optionnels de {@link RecapitulatifSearchDto} (sources, libellé/observation,
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
                    .and(byLibelleOuObservation(searchDto.libelleOuObservation()))
                    .and(byNumero(searchDto.numero()))
                    .and(byMontant(searchDto.montant()))
                    .and(byCommissionOperation(searchDto.commissionOperation()))
                    .and(byImpotEtTaxeMotif(searchDto.impotEtTaxeMotif()));
        }

        return spec;
    }
}
