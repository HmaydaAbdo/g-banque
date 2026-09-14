package staport.finance.gBanque.moyensDePaiements.virements.service;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.moyensDePaiements.virements.entities.Virement;
import staport.finance.gBanque.moyensDePaiements.virements.repositories.VirementRepository;

import java.util.List;
import java.util.Optional;

/**
 * Façade cross-domaine pour le module {@code moyensDePaiements.batch} (synchronisation
 * FunPaiement) — le batch ne doit jamais injecter {@link VirementRepository} directement
 * (encapsulation des domaines, architecture.md).
 *
 * <p>Écriture en lot ({@code saveAll}) volontairement distincte des méthodes DTO de
 * {@link VirementService} : les entités construites par {@code VirementPaymentProcessor} viennent
 * directement du flux FunPaiement (upsert bas niveau, pas de validation/audit applicatif —
 * l'audit du batch suit son propre mécanisme, {@code BatchAuditContext}).
 */
@Component
public class VirementBatchGateway {

    private final VirementRepository virementRepository;

    public VirementBatchGateway(VirementRepository virementRepository) {
        this.virementRepository = virementRepository;
    }

    public Optional<Virement> findByIdPaiement(String idPaiement) {
        return virementRepository.findByIdPaiement(idPaiement);
    }

    public List<Virement> saveAll(Iterable<Virement> virements) {
        return virementRepository.saveAll(virements);
    }
}
