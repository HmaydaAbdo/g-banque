package staport.finance.gBanque.Agios.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.Agios.entities.AgiosJournalier;
import staport.finance.gBanque.Agios.enums.TypeAgios;
import staport.finance.gBanque.marches.entities.Marche;
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

    /**
     * Marchés distincts effectivement présents dans le détail journalier d'une banque/type/période
     * donnée — alimente le filtre marché de l'écran de détail (types multi-marchés uniquement,
     * {@code marche IS NULL} pour la Facilité de Caisse donc jamais retourné ici).
     */
    @Query("""
        SELECT DISTINCT j.marche FROM AgiosJournalier j
        WHERE j.banque.id = :banqueId
        AND j.typeAgios = :type
        AND j.dateValeur BETWEEN :debut AND :fin
        AND j.marche IS NOT NULL
        ORDER BY j.marche.numero
    """)
    List<Marche> findDistinctMarchesByCriteria(@Param("banqueId") Long banqueId,
                                                @Param("type") TypeAgios type,
                                                @Param("debut") LocalDate debut,
                                                @Param("fin") LocalDate fin);
}