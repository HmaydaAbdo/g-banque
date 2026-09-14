package staport.finance.gBanque.moyensDePaiements.lcn.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.moyensDePaiements.lcn.entities.Lcn;
import staport.finance.gBanque.moyensDePaiements.lcn.repositories.LcnRepository;

import java.util.List;
import java.util.Optional;

/**
 * Façade cross-domaine pour le module {@code moyensDePaiements.batch} (synchronisation
 * FunPaiement) — le batch ne doit jamais injecter {@link LcnRepository} directement
 * (encapsulation des domaines, architecture.md).
 *
 * <p>Écriture en lot ({@code saveAll}) volontairement distincte des méthodes DTO de
 * {@link LcnService} : les entités construites par {@code LcnPaymentProcessor} viennent
 * directement du flux FunPaiement (upsert bas niveau, pas de validation/audit applicatif —
 * l'audit du batch suit son propre mécanisme, {@code BatchAuditContext}).
 */
@Component
public class LcnBatchGateway {

    private final LcnRepository lcnRepository;

    public LcnBatchGateway(LcnRepository lcnRepository) {
        this.lcnRepository = lcnRepository;
    }

    public Optional<Lcn> findByIdPaiement(String idPaiement) {
        return lcnRepository.findByIdPaiement(idPaiement);
    }

    public List<Lcn> saveAll(Iterable<Lcn> lcns) {
        return lcnRepository.saveAll(lcns);
    }
}
