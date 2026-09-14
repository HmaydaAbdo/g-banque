package staport.finance.gBanque.audit;

import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Rend une notion liée lisible dans le détail d'audit : {@code "7 — BMCE"} plutôt
 * que {@code "7"}.
 *
 * <p>L'identifiant est <strong>toujours</strong> conservé en tête. C'est la seule
 * valeur exacte, stable et toujours disponible : le libellé peut avoir changé, être
 * vide, ou concerner une entité supprimée depuis. Un détail d'audit doit rester
 * vérifiable — le libellé n'est qu'un confort de lecture posé par-dessus.
 *
 * <p><strong>Convention de résolution</strong> (voir {@code conventions/Audit.md}) :
 * le premier accesseur trouvé dans {@link #LABEL_ACCESSORS} fait office de libellé,
 * dans cet ordre. À défaut, tout accesseur {@code getNumeroXxx} renvoyant une chaîne
 * est utilisé (couvre {@code Caution.numeroCaution}). Si rien ne correspond
 * (ex. {@code Rapprochement}, qui ne porte aucun champ textuel), seul l'identifiant
 * est rendu.
 *
 * <p>La résolution passe par les <em>getters</em> et non par les champs : sur un proxy
 * Hibernate, les champs déclarés sont ceux de la sous-classe générée et sont vides,
 * alors que les getters sont surchargés et initialisent le proxy correctement.
 */
@Component
public class AuditLabelResolver {

    private static final List<String> LABEL_ACCESSORS = List.of(
            "getNom",
            "getLibelle",
            "getDesignation",
            "getIntitule",
            "getReference",
            "getNumero",
            "getCode",
            "getUsername"
    );

    private static final String NUMERO_ACCESSOR_PREFIX = "getNumero";

    private static final String SEPARATOR = " — ";

    /**
     * @return {@code "<id>"} ou {@code "<id> — <libellé>"}, ou {@code null} si la
     *         notion liée n'expose aucun identifiant exploitable.
     */
    public String describe(Object relatedEntity) {
        String id = readId(relatedEntity);
        if (id == null) {
            return null;
        }
        String label = readLabel(relatedEntity);
        return (label == null || label.isBlank()) ? id : id + SEPARATOR + label;
    }

    /**
     * Lit l'identifiant sans initialiser le proxy : Hibernate sert
     * {@code getId()} depuis la clé étrangère déjà chargée, sans requête.
     */
    private String readId(Object relatedEntity) {
        Object id = invokeIfPresent(relatedEntity, "getId");
        if (id == null) {
            id = invokeIfPresent(relatedEntity, "id");
        }
        return id == null ? null : String.valueOf(id);
    }

    private String readLabel(Object relatedEntity) {
        for (String accessor : LABEL_ACCESSORS) {
            Object value = invokeIfPresent(relatedEntity, accessor);
            if (value instanceof String text && !text.isBlank()) {
                return text;
            }
        }
        return readNumeroLabel(relatedEntity);
    }

    /**
     * Repli pour les notions dont le numéro porte le nom de l'entité
     * ({@code getNumeroCaution}, etc.) plutôt que le simple {@code getNumero}.
     */
    private String readNumeroLabel(Object relatedEntity) {
        for (Method method : relatedEntity.getClass().getMethods()) {
            if (!method.getName().startsWith(NUMERO_ACCESSOR_PREFIX)
                    || method.getParameterCount() != 0
                    || !String.class.equals(method.getReturnType())) {
                continue;
            }
            Object value = invoke(method, relatedEntity);
            if (value instanceof String text && !text.isBlank()) {
                return text;
            }
        }
        return null;
    }

    private Object invokeIfPresent(Object target, String accessorName) {
        try {
            return invoke(target.getClass().getMethod(accessorName), target);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * Toute erreur de lecture est absorbée : le détail d'audit doit rester
     * consultable même si une notion liée est illisible (entité non auditée,
     * cible supprimée, proxy non initialisable hors session).
     */
    private Object invoke(Method method, Object target) {
        try {
            return method.invoke(target);
        } catch (Exception e) {
            return null;
        }
    }
}
