package staport.finance.gBanque.cautions.specifications;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.cautions.dtos.request.CautionSearchCriteria;
import staport.finance.gBanque.cautions.entities.Caution;

import java.util.ArrayList;
import java.util.List;

/**
 * Spécifications JPA pour la recherche dynamique de cautions.
 *
 * <p>Chaque critère est optionnel — un champ {@code null} est simplement ignoré.
 * Les prédicats sont combinés avec {@code AND}.
 *
 * <p>Filtres disponibles :
 * <ul>
 *   <li>{@code numeroCaution} — LIKE insensible à la casse (%valeur%)</li>
 *   <li>{@code organisme}     — LIKE insensible à la casse (%valeur%)</li>
 *   <li>{@code banqueId}      — égalité exacte sur l'ID banque</li>
 *   <li>{@code natureId}      — égalité exacte sur l'ID nature</li>
 *   <li>{@code marcheId}      — égalité exacte sur l'ID marché</li>
 *   <li>{@code dateDePrise}      — filtre par plage (dateDePriseDebut/Fin)</li>
 *   <li>{@code dateDeMainLevee}  — filtre par plage (dateDeMainLeveeDebut/Fin)</li>
 *   <li>{@code montant}       — égalité exacte sur le montant</li>
 *   <li>{@code actif}         — filtre booléen exact</li>
 * </ul>
 */
public final class CautionSpecification {

    private CautionSpecification() {
        // Classe utilitaire — pas d'instanciation
    }

    /**
     * Construit une {@link Specification} combinée à partir des critères fournis.
     * Retourne tous les enregistrements si tous les critères sont null.
     */
    public static Specification<Caution> parCriteres(CautionSearchCriteria criteres) {
        return (root, query, cb) -> {

            // Évite le doublon de lignes lors de l'utilisation avec EntityGraph (DISTINCT)
            query.distinct(true);

            List<Predicate> predicates = new ArrayList<>();

            // ── Numéro caution (LIKE insensible à la casse) ───────────────────
            if (criteres.numeroCaution() != null && !criteres.numeroCaution().isBlank()) {
                predicates.add(
                    cb.like(
                        cb.lower(root.get("numeroCaution")),
                        "%" + criteres.numeroCaution().toLowerCase() + "%"
                    )
                );
            }

            // ── Organisme (LIKE insensible à la casse) ────────────────────────
            if (criteres.organisme() != null && !criteres.organisme().isBlank()) {
                predicates.add(
                    cb.like(
                        cb.lower(root.get("organisme")),
                        "%" + criteres.organisme().toLowerCase() + "%"
                    )
                );
            }

            // ── Banque ID ─────────────────────────────────────────────────────
            if (criteres.banqueId() != null) {
                predicates.add(
                    cb.equal(root.get("banque").get("id"), criteres.banqueId())
                );
            }

            // ── Nature ID ─────────────────────────────────────────────────────
            if (criteres.natureId() != null) {
                predicates.add(
                    cb.equal(root.get("nature").get("id"), criteres.natureId())
                );
            }

            // ── Marché ID ─────────────────────────────────────────────────────
            if (criteres.marcheId() != null) {
                predicates.add(
                    cb.equal(root.get("marche").get("id"), criteres.marcheId())
                );
            }

            // ── Date de prise (plage) ──────────────────────────────────────────
            if (criteres.dateDePriseDebut() != null && criteres.dateDePriseFin() != null) {
                predicates.add(cb.between(root.get("dateDePrise"), criteres.dateDePriseDebut(), criteres.dateDePriseFin()));
            } else if (criteres.dateDePriseDebut() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dateDePrise"), criteres.dateDePriseDebut()));
            } else if (criteres.dateDePriseFin() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dateDePrise"), criteres.dateDePriseFin()));
            }

            // ── Date de main levée (plage) ─────────────────────────────────────
            if (criteres.dateDeMainLeveeDebut() != null && criteres.dateDeMainLeveeFin() != null) {
                predicates.add(cb.between(root.get("dateDeMainLevee"), criteres.dateDeMainLeveeDebut(), criteres.dateDeMainLeveeFin()));
            } else if (criteres.dateDeMainLeveeDebut() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dateDeMainLevee"), criteres.dateDeMainLeveeDebut()));
            } else if (criteres.dateDeMainLeveeFin() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dateDeMainLevee"), criteres.dateDeMainLeveeFin()));
            }

            // ── Montant (égalité exacte) ──────────────────────────────────────
            if (criteres.montant() != null) {
                predicates.add(
                    cb.equal(root.get("montant"), criteres.montant())
                );
            }

            // ── Statut actif ──────────────────────────────────────────────────
            if (criteres.actif() != null) {
                predicates.add(
                    cb.equal(root.get("actif"), criteres.actif())
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
