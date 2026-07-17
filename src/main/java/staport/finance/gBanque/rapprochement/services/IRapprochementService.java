package staport.finance.gBanque.rapprochement.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import staport.finance.gBanque.rapprochement.dtos.RapprochementActifsDto;
import staport.finance.gBanque.rapprochement.dtos.RapprochementResponseDto;
import staport.finance.gBanque.rapprochement.dtos.RapprochementSearchDto;
import staport.finance.gBanque.rapprochement.entities.Rapprochement;
import staport.finance.gBanque.reports.enums.ReportType;
import staport.finance.gBanque.transactions.dtos.TransactionSearchDto;

import java.util.List;
import java.util.Optional;

/**
 * Façade publique du domaine Rapprochement.
 *
 * <p>Toute communication inter-domaines avec Rapprochement passe exclusivement par cette
 * interface (jamais {@code RapprochementRepository} injecté directement — voir
 * {@code architecture.md}, section "Encapsulation des domaines"). Consommée par
 * {@code RapprochementController} et {@code BankAccueilAggregatorService} (module Bank).
 *
 * <p>{@code createInitialRapprochement}/{@code onBanqueCreated} restent volontairement hors de
 * cette interface : appelés uniquement en interne (écoute d'événement Spring), aucun autre domaine
 * n'y accède directement.
 */
public interface IRapprochementService {

    RapprochementActifsDto getActifs(Long banqueId);

    RapprochementResponseDto cloturerProvisoire(Long banqueId);

    RapprochementResponseDto cloturerDefinitivement(Long rapprochementId);

    RapprochementResponseDto getRapprochementById(Long id, TransactionSearchDto searchDto, Pageable pageable);

    Page<RapprochementResponseDto> searchRapprochements(RapprochementSearchDto searchDto, Pageable pageable);

    byte[] generateRapprochementPdf(Long rapprochementId, ReportType reportType);

    /** Tous les rapprochements actuellement EN_COURS, toutes banques confondues. */
    List<Rapprochement> findAllEnCours();

    /** Le rapprochement EN_COURS d'une banque donnée, s'il existe. */
    Optional<Rapprochement> findEnCoursByBanqueId(Long banqueId);
}
