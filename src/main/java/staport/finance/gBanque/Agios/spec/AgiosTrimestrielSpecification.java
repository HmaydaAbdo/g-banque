package staport.finance.gBanque.Agios.spec;


import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.Agios.entities.AgiosTrimestriel;
import staport.finance.gBanque.Agios.enums.StatutTrimestre;
import staport.finance.gBanque.Agios.enums.TypeAgios;

public class AgiosTrimestrielSpecification {

    private AgiosTrimestrielSpecification() {}

    public static Specification<AgiosTrimestriel> banqueId(Long banqueId) {
        return (root, query, cb) -> banqueId == null ? null :
                cb.equal(root.get("banque").get("id"), banqueId);
    }

    public static Specification<AgiosTrimestriel> typeAgios(TypeAgios type) {
        return (root, query, cb) -> type == null ? null :
                cb.equal(root.get("typeAgios"), type);
    }

    public static Specification<AgiosTrimestriel> annee(Integer annee) {
        return (root, query, cb) -> annee == null ? null :
                cb.equal(root.get("annee"), annee);
    }

    public static Specification<AgiosTrimestriel> trimestre(Integer trimestre) {
        return (root, query, cb) -> trimestre == null ? null :
                cb.equal(root.get("trimestre"), trimestre);
    }

    public static Specification<AgiosTrimestriel> statut(StatutTrimestre statut) {
        return (root, query, cb) -> statut == null ? null :
                cb.equal(root.get("statut"), statut);
    }

    public static Specification<AgiosTrimestriel> banqueNomContains(String search) {
        return (root, query, cb) -> (search == null || search.isBlank()) ? null :
                cb.like(cb.lower(root.get("banque").get("nom")),
                        "%" + search.toLowerCase() + "%");
    }

    /** Convention repo : {@code XSpecification.searchByCriteria(...)} (architecture.md). */
    public static Specification<AgiosTrimestriel> searchByCriteria(Long banqueId, TypeAgios type,
                                                            Integer annee, Integer trimestre,
                                                            StatutTrimestre statut, String search) {
        return Specification.where(banqueId(banqueId))
                .and(typeAgios(type))
                .and(annee(annee))
                .and(trimestre(trimestre))
                .and(statut(statut))
                .and(banqueNomContains(search));
    }
}