package staport.finance.gBanque.Bank.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.Bank.entities.HistoriqueSoldeBanque;

import java.util.List;
import java.util.Optional;

@Repository
public interface HistoriqueSoldeBanqueRepository extends JpaRepository<HistoriqueSoldeBanque, Long> {

    /**
     * Récupère l'historique d'une banque trié par année et mois décroissants
     */
    @Query("SELECT h FROM HistoriqueSoldeBanque h WHERE h.banque.id = :banqueId ORDER BY h.annee DESC, h.mois DESC")
    List<HistoriqueSoldeBanque> findByBanqueIdOrderByAnneeDescMoisDesc(@Param("banqueId") Long banqueId);

    /**
     * Vérifie si un historique existe pour une banque, un mois et une année donnés
     */
    boolean existsByBanqueIdAndMoisAndAnnee(Long banqueId, Integer mois, Integer annee);

    /**
     * Récupère un historique spécifique pour une banque, un mois et une année
     */
    Optional<HistoriqueSoldeBanque> findByBanqueIdAndMoisAndAnnee(Long banqueId, Integer mois, Integer annee);

    /**
     * Récupère l'historique d'une année spécifique pour une banque
     */
    @Query("SELECT h FROM HistoriqueSoldeBanque h WHERE h.banque.id = :banqueId AND h.annee = :annee ORDER BY h.mois DESC")
    List<HistoriqueSoldeBanque> findByBanqueIdAndAnnee(@Param("banqueId") Long banqueId, @Param("annee") Integer annee);
}