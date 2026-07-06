package staport.finance.gBanque.logs.config;

/**
 * SUPPRIMÉ (contenu neutralisé) — sweep NatureLigneFonctionnelle du {@code 2026-07-04}.
 *
 * Cet aspect dédié à une seule entité violait {@code conventions/Audit.md}
 * ("Un seul aspect d'audit est utilisé : GenericAuditAspect... Ne jamais créer un
 * aspect dédié à une entité ou à un module") et comparait les champs manuellement
 * ({@code compareAndLogFields}), également interdit ("Ne jamais comparer les champs
 * manuellement dans un aspect. Utiliser AuditReader via EnversDiffService.").
 *
 * Remplacé par {@code @Audited} sur {@code NatureLigneFonctionnelle} +
 * {@code @AuditableAction} sur les méthodes de {@code NatureLigneFonctionnelleService}
 * (mécanisme {@code GenericAuditAspect}, cf. javadoc de cette classe — "Remplace les
 * anciens aspects dédiés par entité au fur et à mesure de la migration module par
 * module").
 *
 * Le fichier n'a pas pu être supprimé depuis cet environnement (sandbox sans accès
 * disque complet — même limitation que documentée dans {@code TODO.md} pour le sweep
 * CMT). Ce contenu neutralisé (ni {@code @Aspect} ni {@code @Component}, donc jamais
 * chargé par Spring) doit être supprimé manuellement dès que possible :
 *
 * <pre>
 * git rm Backend/src/main/java/staport/finance/gBanque/logs/config/NatureLigneFonctionnelleAuditAspect.java
 * </pre>
 */
final class NatureLigneFonctionnelleAuditAspect {

    private NatureLigneFonctionnelleAuditAspect() {
    }
}
