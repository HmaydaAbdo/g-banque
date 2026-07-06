package staport.finance.gBanque.LigneFonctionnelle.generalLignes.services;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.entities.LigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.repositories.LigneFonctionnelleRepository;

import java.util.Optional;

/**
 * Façade de lecture cross-domaine pour LigneFonctionnelle (générale).
 *
 * <p>Les domaines Préfinancement, Factoring, DroitConstate et Lcn ont besoin de résoudre la
 * ligne fonctionnelle générale active pour rattacher leurs opérations, mais ne doivent jamais
 * injecter {@link LigneFonctionnelleRepository} directement (encapsulation des domaines,
 * architecture.md). Ce composant expose uniquement les lectures nécessaires — jamais d'écriture,
 * jamais la logique métier de création/mise à jour d'une ligne (qui reste dans
 * {@code LigneFonctionnelleService}).
 *
 * <p>N'injecte aucun Service — évite tout cycle de beans avec {@code ILigneFonctionnelleService},
 * qui injecte lui-même des lecteurs équivalents des 4 domaines ci-dessus.
 */
@Component
public class LigneFonctionnelleReader {

    private final LigneFonctionnelleRepository ligneFonctionnelleRepository;

    public LigneFonctionnelleReader(LigneFonctionnelleRepository ligneFonctionnelleRepository) {
        this.ligneFonctionnelleRepository = ligneFonctionnelleRepository;
    }

    /**
     * Ligne active (générale ou non) qui inclut le marché donné dans sa collection de marchés,
     * pour la nature spécifiée. Verrou PESSIMISTIC_WRITE porté par la requête sous-jacente —
     * participe à la transaction de l'appelant (pas de {@code REQUIRES_NEW} ici).
     */
    @Transactional(readOnly = true)
    public Optional<LigneFonctionnelle> findActiveLineForMarche(Long marcheId, Long natureId) {
        return ligneFonctionnelleRepository.findActiveLineForMarche(marcheId, natureId);
    }

    /**
     * Ligne active pour une banque et une nature donnée (utilisé par Lcn pour l'association
     * automatique Escompte Commercial).
     */
    @Transactional(readOnly = true)
    public Optional<LigneFonctionnelle> findByBanqueIdAndNatureLigneFonctionnelleIdAndActifTrue(Long banqueId, Long natureId) {
        return ligneFonctionnelleRepository.findByBanqueIdAndNatureLigneFonctionnelleIdAndActifTrue(banqueId, natureId);
    }
}
