package staport.finance.gBanque.moyensDePaiements.cheques.services;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.moyensDePaiements.cheques.entities.Cheque;
import staport.finance.gBanque.moyensDePaiements.cheques.repositories.ChequeRepository;

import java.util.List;
import java.util.Optional;

/**
 * Façade cross-domaine pour le module {@code moyensDePaiements.batch} (synchronisation
 * FunPaiement) — le batch ne doit jamais injecter {@link ChequeRepository} directement
 * (encapsulation des domaines, architecture.md).
 *
 * <p>Écriture en lot ({@code saveAll}) volontairement distincte des méthodes DTO de
 * {@link ChequeService} : les entités construites par {@code ChequePaymentProcessor} viennent
 * directement du flux FunPaiement (upsert bas niveau, pas de validation/audit applicatif —
 * l'audit du batch suit son propre mécanisme, {@code BatchAuditContext}).
 */
@Component
public class ChequeBatchGateway {

    private final ChequeRepository chequeRepository;

    public ChequeBatchGateway(ChequeRepository chequeRepository) {
        this.chequeRepository = chequeRepository;
    }

    public Optional<Cheque> findByIdPaiement(String idPaiement) {
        return chequeRepository.findByIdPaiement(idPaiement);
    }

    public List<Cheque> saveAll(Iterable<Cheque> cheques) {
        return chequeRepository.saveAll(cheques);
    }
}
