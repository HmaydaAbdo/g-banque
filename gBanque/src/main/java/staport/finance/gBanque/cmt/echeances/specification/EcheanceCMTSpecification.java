package staport.finance.gBanque.cmt.echeances.specification;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.cmt.echeances.dtos.EcheanceCMTSearchDto;
import staport.finance.gBanque.cmt.echeances.dtos.PeriodeEncoursCMT;
import staport.finance.gBanque.cmt.echeances.entities.EcheanceCMT;
import staport.finance.gBanque.cmt.echeances.enums.StatutEcheanceCMT;
import staport.finance.gBanque.cmt.entities.CreditMoyenTerme;


import java.time.OffsetDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe de spécification pour construire dynamiquement des requêtes
 * de recherche pour l'entité EcheanceCMT.
 * Elle utilise le Criteria API de JPA.
 */
public class EcheanceCMTSpecification {

    public static Specification<EcheanceCMT> withSearchCriteria(EcheanceCMTSearchDto searchDto) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Prédicat pour l'ID du crédit parent
            if (searchDto.getCreditMoyenTermeId() != null) {
                predicates.add(builder.equal(root.get("creditMoyenTerme").get("id"), searchDto.getCreditMoyenTermeId()));
            }

            // Prédicat pour le numéro d'échéance
            if (searchDto.getNumeroEcheance() != null) {
                predicates.add(builder.equal(root.get("numeroEcheance"), searchDto.getNumeroEcheance()));
            }

            // Prédicat pour le statut de paiement
            if (searchDto.getStatutPaiement() != null) {
                predicates.add(builder.equal(root.get("statutPaiement"), searchDto.getStatutPaiement()));
            }

            // Prédicats pour le mois et l'année en utilisant une plage de dates pour plus de robustesse.
            if (searchDto.getYear() != null && searchDto.getMonth() != null) {
                OffsetDateTime startOfMonth = OffsetDateTime.now()
                        .withYear(searchDto.getYear())
                        .withMonth(searchDto.getMonth())
                        .with(TemporalAdjusters.firstDayOfMonth())
                        .withHour(0).withMinute(0).withSecond(0).withNano(0);

                OffsetDateTime endOfMonth = OffsetDateTime.now()
                        .withYear(searchDto.getYear())
                        .withMonth(searchDto.getMonth())
                        .with(TemporalAdjusters.lastDayOfMonth())
                        .withHour(23).withMinute(59).withSecond(59).withNano(999999999);

                predicates.add(builder.between(root.get("dateEcheance"), startOfMonth, endOfMonth));
            }


            // Combine tous les prédicats avec un opérateur AND
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Crée une Specification pour filtrer les échéances par l'ID de la banque
     * associée au crédit à moyen terme parent.
     *
     * @param banqueId L'ID de la banque pour le filtrage.
     * @return Une Specification qui filtre par l'ID de la banque.
     */
    public static Specification<EcheanceCMT> byCreditBanqueId(Long banqueId) {
        return (root, query, criteriaBuilder) -> {
            // Effectue une jointure entre EcheanceCMT et CreditMoyenTerme
            Join<EcheanceCMT, CreditMoyenTerme> creditJoin = root.join("creditMoyenTerme");
            // Accède à l'entité Banque via le CreditMoyenTerme joint
            return criteriaBuilder.equal(creditJoin.get("banque").get("id"), banqueId);
        };
    }

    /**
     * Crée une Specification pour filtrer les échéances non payées jusqu'à
     * une période donnée (fin du jour ou fin du mois).
     *
     * @param periode L'énumération de la période ("JOUR" ou "MOIS").
     * @param now L'instant présent utilisé pour les calculs.
     * @return Une Specification pour les échéances d'encours.
     */
    public static Specification<EcheanceCMT> outstandingUntil(PeriodeEncoursCMT periode, OffsetDateTime now) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            // Filtre les échéances dont le statut est "PREVU" (non payé)
            predicates.add(criteriaBuilder.equal(root.get("statutPaiement"), StatutEcheanceCMT.PREVU));

            OffsetDateTime dateButoir;
            if (PeriodeEncoursCMT.JOUR.equals(periode)) {
                // Calcule la date butoir à la fin de la journée actuelle
                dateButoir = now.withHour(23).withMinute(59).withSecond(59).withNano(999999999);
            } else if (PeriodeEncoursCMT.MOIS.equals(periode)) {
                // Calcule la date butoir à la fin du dernier jour du mois
                dateButoir = now.with(TemporalAdjusters.lastDayOfMonth()).withHour(23).withMinute(59).withSecond(59).withNano(999999999);
            } else {
                throw new IllegalArgumentException("La période d'encours spécifiée est invalide.");
            }

            // Ajoute la condition de date
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("dateEcheance"), dateButoir));

            // Combine tous les prédicats avec un opérateur AND
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

}
