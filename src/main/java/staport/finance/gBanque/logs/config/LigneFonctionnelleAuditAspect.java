package staport.finance.gBanque.logs.config;

/**
 * SUPPRIMÉ (contenu neutralisé) — sweep LigneFonctionnelle générale du {@code 2026-07-06}.
 *
 * Cet aspect dédié à une seule entité violait {@code conventions/Audit.md}
 * ("Un seul aspect d'audit est utilisé : GenericAuditAspect... Ne jamais créer un
 * aspect dédié à une entité ou à un module") et comparait les champs manuellement,
 * également interdit ("Ne jamais comparer les champs manuellement dans un aspect.
 * Utiliser AuditReader via EnversDiffService.").
 *
 * De plus, ses {@code @Pointcut} référençaient un FQCN obsolète
 * ({@code staport.finance.gBanque.LigneFonctionnelle.services.LigneFonctionnelleService},
 * qui n'existe plus — le service vit désormais dans le sous-package
 * {@code generalLignes.services}) et une méthode inexistante
 * ({@code deactivateLigneFonctionnelleByNatureAndBank}, remplacée depuis par
 * {@code deactivateLigneFonctionnelle(Long ligneId)}). Cet aspect était donc déjà
 * totalement mort (pointcuts qui ne correspondent plus à rien) avant même d'être
 * neutralisé.
 *
 * Remplacé par {@code @Audited} sur {@code LigneFonctionnelle} +
 * {@code @AuditableAction} sur les méthodes de création/modification/suppression de
 * {@code LigneFonctionnelleService} (mécanisme {@code GenericAuditAspect}, cf. javadoc
 * de cette classe — "Remplace les anciens aspects dédiés par entité au fur et à mesure
 * de la migration module par module").
 *
 * Le fichier n'a pas pu être supprimé depuis cet environnement (sandbox sans accès
 * disque complet — même limitation que documentée dans {@code TODO.md} pour les sweeps
 * CMT et NatureLigneFonctionnelle). Ce contenu neutralisé (ni {@code @Aspect} ni
 * {@code @Component}, donc jamais chargé par Spring) doit être supprimé manuellement
 * dès que possible :
 *
 * <pre>
 * git rm Backend/src/main/java/staport/finance/gBanque/logs/config/LigneFonctionnelleAuditAspect.java
 * </pre>
 */
final class LigneFonctionnelleAuditAspect {

    private LigneFonctionnelleAuditAspect() {
    }
}
