package staport.finance.gBanque.Agios.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.Agios.entities.AgiosJournalier;
import staport.finance.gBanque.Agios.enums.TypeAgios;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AgiosJournalierRepository extends JpaRepository<AgiosJournalier, Long>, JpaSpecificationExecutor<AgiosJournalier> {

    // ── FC et types sans marché (marche IS NULL) ─────────────────────────────

    Optional<AgiosJournalier> findByDateValeurAndBanqueIdAndTypeAgiosAndMarcheIsNull(
            LocalDate dateValeur, Long banqueId, TypeAgios typeAgios);

    // ── Préfinancement et types avec marché (marche IS NOT NULL) ─────────────

    Optional<AgiosJournalier> findByDateValeurAndBanqueIdAndTypeAgiosAndMarcheId(
            LocalDate dateValeur, Long banqueId, TypeAgios typeAgios, Long marcheId);

    // ── Consultation ──────────────────────────────────────────────────────────

    List<AgiosJournalier> findByBanqueIdAndTypeAgiosAndDateValeurBetweenOrderByDateValeurAsc(
            Long banqueId, TypeAgios typeAgios, LocalDate debut, LocalDate fin);

    List<AgiosJournalier> findByTrimestreId(Long trimestreId);
}