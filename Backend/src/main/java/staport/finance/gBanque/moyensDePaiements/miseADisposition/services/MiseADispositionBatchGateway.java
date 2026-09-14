package staport.finance.gBanque.moyensDePaiements.miseADisposition.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.entities.MiseADisposition;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.repositories.MiseADispositionRepository;

import java.util.List;
import java.util.Optional;

/**
 * Façade cross-domaine pour le module {@code moyensDePaiements.batch} (synchronisation
 * FunPaiement) — le batch ne doit jamais injecter {@link MiseADispositionRepository} directement
 * (encapsulation des domaines, architecture.md).
 *
 * <p>Écriture en lot ({@code saveAll}) volontairement distincte des méthodes DTO de
 * {@link MiseADispositionService} : les entités construites par
 * {@code MiseADispositionPaymentProcessor} viennent directement du flux FunPaiement (upsert bas
 * niveau, pas de validation/audit applicatif — l'audit du batch suit son propre mécanisme,
 * {@code BatchAuditContext}).
 */
@Component
public class MiseADispositionBatchGateway {

    private final MiseADispositionRepository miseADispositionRepository;

    public MiseADispositionBatchGateway(MiseADispositionRepository miseADispositionRepository) {
        this.miseADispositionRepository = miseADispositionRepository;
    }

    public Optional<MiseADisposition> findByIdPaiement(String idPaiement) {
        return miseADispositionRepository.findByIdPaiement(idPaiement);
    }

    public List<MiseADisposition> saveAll(Iterable<MiseADisposition> misesADisposition) {
        return miseADispositionRepository.saveAll(misesADisposition);
    }
}
