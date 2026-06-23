package staport.finance.gBanque.Bank.events;

/**
 * Publié par {@link staport.finance.gBanque.Bank.services.BanqueServiceImpl#createBanque}
 * après la création d'une banque. Permet au domaine Rapprochement de créer le premier
 * rapprochement EN_COURS sans que {@code BanqueServiceImpl} ait besoin d'injecter
 * {@code RapprochementService} — {@code RapprochementService} dépend déjà de
 * {@code IBanqueService}, une dépendance dans l'autre sens créerait un cycle Spring
 * (Backend/CLAUDE.md, "Encapsulation des domaines").
 *
 * <p>Publié via {@code ApplicationEventPublisher} (bean du framework, pas du domaine
 * Bank) — aucune dépendance directe entre les deux Services.
 */
public record BanqueCreatedEvent(Long banqueId) {
}
