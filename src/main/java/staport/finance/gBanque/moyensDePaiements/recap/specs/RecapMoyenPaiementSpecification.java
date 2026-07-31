package staport.finance.gBanque.moyensDePaiements.recap.specs;

import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.moyensDePaiements.recap.dtos.request.RecapMoyenPaiementSearchDto;
import staport.finance.gBanque.moyensDePaiements.recap.entities.RecapMoyenPaiementView;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class RecapMoyenPaiementSpecification {

    private RecapMoyenPaiementSpecification() {
        // Classe utilitaire — pas d'instanciation
    }

    public static Specification<RecapMoyenPaiementView> byType(Type type) {
        return (root, query, cb) ->
                type == null ? cb.conjunction() : cb.equal(root.get("type"), type);
    }

    public static Specification<RecapMoyenPaiementView> byBanqueId(Long banqueId) {
        return (root, query, cb) ->
                banqueId == null ? cb.conjunction() : cb.equal(root.get("banqueId"), banqueId);
    }

    public static Specification<RecapMoyenPaiementView> bySources(List<TransactionSource> sources) {
        return (root, query, cb) ->
                (sources == null || sources.isEmpty())
                        ? cb.conjunction()
                        : root.get("id").get("sourceType").in(sources);
    }

    public static Specification<RecapMoyenPaiementView> byStatuts(List<StatutMoyenPaiement> statuts) {
        return (root, query, cb) ->
                (statuts == null || statuts.isEmpty())
                        ? cb.conjunction()
                        : root.get("statut").in(statuts);
    }

    public static Specification<RecapMoyenPaiementView> byLibelle(String libelle) {
        return (root, query, cb) ->
                (libelle == null || libelle.isBlank())
                        ? cb.conjunction()
                        : cb.like(cb.lower(root.get("libelle")), "%" + libelle.toLowerCase() + "%");
    }

    public static Specification<RecapMoyenPaiementView> byNumero(String numero) {
        return (root, query, cb) ->
                (numero == null || numero.isBlank())
                        ? cb.conjunction()
                        : cb.like(cb.lower(root.get("numero")), "%" + numero.toLowerCase() + "%");
    }

    public static Specification<RecapMoyenPaiementView> byMontantRange(BigDecimal from, BigDecimal to) {
        return (root, query, cb) -> {
            if (from == null && to == null) return cb.conjunction();
            if (from != null && to != null) return cb.between(root.get("montant"), from, to);
            if (from != null) return cb.greaterThanOrEqualTo(root.get("montant"), from);
            return cb.lessThanOrEqualTo(root.get("montant"), to);
        };
    }

    public static Specification<RecapMoyenPaiementView> byDateOperationRange(LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            if (from == null && to == null) return cb.conjunction();
            if (from != null && to != null) return cb.between(root.get("dateOperation"), from, to);
            if (from != null) return cb.greaterThanOrEqualTo(root.get("dateOperation"), from);
            return cb.lessThanOrEqualTo(root.get("dateOperation"), to);
        };
    }

    public static Specification<RecapMoyenPaiementView> byDateValeurRange(LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            if (from == null && to == null) return cb.conjunction();
            if (from != null && to != null) return cb.between(root.get("dateValeur"), from, to);
            if (from != null) return cb.greaterThanOrEqualTo(root.get("dateValeur"), from);
            return cb.lessThanOrEqualTo(root.get("dateValeur"), to);
        };
    }

    /**
     * Combine le sens fixé par l'endpoint (encaissement/décaissement) avec les filtres
     * dynamiques optionnels de {@link RecapMoyenPaiementSearchDto}.
     */
    public static Specification<RecapMoyenPaiementView> searchByCriteria(
            RecapMoyenPaiementSearchDto searchDto, Type type) {

        Specification<RecapMoyenPaiementView> spec = Specification.where(byType(type));

        if (searchDto != null) {
            spec = spec
                    .and(byBanqueId(searchDto.banqueId()))
                    .and(bySources(searchDto.sources()))
                    .and(byStatuts(searchDto.statuts()))
                    .and(byLibelle(searchDto.libelle()))
                    .and(byNumero(searchDto.numero()))
                    .and(byMontantRange(searchDto.montantFrom(), searchDto.montantTo()))
                    .and(byDateOperationRange(searchDto.dateOperationFrom(), searchDto.dateOperationTo()))
                    .and(byDateValeurRange(searchDto.dateValeurFrom(), searchDto.dateValeurTo()));
        }

        return spec;
    }
}
