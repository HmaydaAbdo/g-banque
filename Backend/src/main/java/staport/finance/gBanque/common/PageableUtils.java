package staport.finance.gBanque.common;

/**
 * Borne la taille de page demandée par le client avant de construire un
 * {@code PageRequest} manuellement à partir d'un {@code @RequestParam} brut.
 *
 * <p>Pour les endpoints qui injectent directement {@code Pageable}/{@code @PageableDefault}
 * (résolus par Spring Data), la borne globale {@code spring.data.web.pageable.max-page-size}
 * (application.yml) s'applique déjà automatiquement — cette classe ne concerne que les
 * endpoints qui contournent ce mécanisme en construisant {@code PageRequest.of(...)} à la main.
 */
public final class PageableUtils {

    /** Doit rester cohérent avec {@code spring.data.web.pageable.max-page-size}. */
    public static final int MAX_PAGE_SIZE = 100;

    private PageableUtils() {
    }

    /** Ramène {@code size} dans la plage [1, {@link #MAX_PAGE_SIZE}]. */
    public static int clampSize(int size) {
        if (size < 1) {
            return 1;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}
