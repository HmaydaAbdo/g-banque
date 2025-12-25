package staport.finance.gBanque.marches.specs;


import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.marches.entities.Marche;

public final class MarcheSpecification {

    /**
     * Private constructor to prevent instantiation.
     * All methods are static.
     */
    private MarcheSpecification() {
    }

    // --- Specifications for each field ---

    /**
     * Creates a Specification to filter by 'nom' (name).
     * The search is case-insensitive and uses 'like' for partial matches.
     *
     * @param nom The name to search for.
     * @return A Specification for the name.
     */
    public static Specification<Marche> hasNom(String nom) {
        return (root, query, cb) -> {
            if (nom == null || nom.trim().isEmpty()) {
                return cb.conjunction(); // Return an "always true" predicate
            }
            return cb.like(cb.lower(root.get("nom")), "%" + nom.toLowerCase() + "%");
        };
    }

    /**
     * Creates a Specification to filter by 'numero' (number) using LIKE.
     *
     * @param numero The contract number (supports partial match).
     * @return A Specification for the number.
     */
    public static Specification<Marche> hasNumero(String numero) {
        return (root, query, cb) -> {
            if (numero == null || numero.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("numero")), "%" + numero.toLowerCase() + "%");
        };
    }




    /**
     * Creates a Specification to filter by 'maitreDouvrage' (client).
     * The search is case-insensitive and uses 'like' for partial matches.
     *
     * @param maitreDouvrage The client name to search for.
     * @return A Specification for the client.
     */
    public static Specification<Marche> hasMaitreDouvrage(String maitreDouvrage) {
        return (root, query, cb) -> {
            if (maitreDouvrage == null || maitreDouvrage.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("maitreDouvrage")), "%" + maitreDouvrage.toLowerCase() + "%");
        };
    }


    /**
     * Creates a Specification to filter by banqueId.
     *
     * @param banqueId The id of the bank.
     * @return A Specification for banqueId.
     */
    public static Specification<Marche> hasBanqueId(Long banqueId) {
        return (root, query, cb) -> {
            if (banqueId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("banque").get("id"), banqueId);
        };
    }
}