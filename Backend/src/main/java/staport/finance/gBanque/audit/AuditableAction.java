package staport.finance.gBanque.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marque explicitement une méthode de Service comme devant être auditée
 * (alimente la table {@code Log} via {@code GenericAuditAspect}).
 *
 * <p>Remplace toute convention de nommage (ex. "methodes commençant par
 * create/update/delete") : seules les méthodes annotées sont auditées. Une
 * méthode sensible (ex. {@code setSystemAdmin}, {@code assignDepartmentRole})
 * qui ne commence par aucun de ces verbes ne passe plus sous le radar
 * silencieusement — son absence d'annotation est visible à la revue de code.
 *
 * <p>Voir Backend/CLAUDE.md, section "Audit", pour le mécanisme complet.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AuditableAction {

    AuditOperation operation();

    /** Classe de l'entité métier concernée — sert à dériver {@code Log.entityName}. */
    Class<?> entity();

    /**
     * Nom de l'accesseur sans argument (record ou getter) à invoquer sur la valeur de
     * retour pour extraire l'identifiant à auditer, lorsque celui-ci ne correspond ni à
     * {@code result.id()}/{@code result.getId()} ni à un paramètre {@code Long} de la
     * méthode (comportement par défaut de {@code GenericAuditAspect}).
     *
     * <p>Exemple : une méthode qui crée une {@code Transaction} mais dont l'entité auditée
     * est l'échéance source (champ {@code echeanceId} du DTO de retour) déclare
     * {@code idAccessor = "echeanceId"}.
     *
     * <p>Laissé vide par défaut — n'affecte aucune des méthodes déjà annotées sans cet
     * attribut, qui conservent l'extraction par défaut.
     */
    String idAccessor() default "";
}
