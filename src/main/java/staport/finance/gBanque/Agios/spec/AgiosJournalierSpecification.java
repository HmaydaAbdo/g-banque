package staport.finance.gBanque.Agios.spec;

import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.Agios.entities.AgiosJournalier;
import staport.finance.gBanque.Agios.enums.TypeAgios;

import java.time.LocalDate;

public class AgiosJournalierSpecification {

    private AgiosJournalierSpecification() {}

    public static Specification<AgiosJournalier> banqueId(Long banqueId) {
        return (root, query, cb) -> banqueId == null ? null :
                cb.equal(root.get("banque").get("id"), banqueId);
    }

    public static Specification<AgiosJournalier> typeAgios(TypeAgios type) {
        return (root, query, cb) -> type == null ? null :
                cb.equal(root.get("typeAgios"), type);
    }

    public static Specification<AgiosJournalier> dateValeurBetween(LocalDate debut, LocalDate fin) {
        return (root, query, cb) -> {
            if (debut == null || fin == null) return null;
            return cb.between(root.get("dateValeur"), debut, fin);
        };
    }

    public static Specification<AgiosJournalier> buildSpec(Long banqueId, TypeAgios type,
                                                           LocalDate debut, LocalDate fin) {
        return Specification.where(banqueId(banqueId))
                .and(typeAgios(type))
                .and(dateValeurBetween(debut, fin));
    }
}