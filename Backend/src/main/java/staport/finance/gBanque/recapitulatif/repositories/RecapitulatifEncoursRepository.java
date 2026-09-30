package staport.finance.gBanque.recapitulatif.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import staport.finance.gBanque.recapitulatif.entities.RecapitulatifEncoursId;
import staport.finance.gBanque.recapitulatif.entities.RecapitulatifEncoursView;

import java.time.LocalDate;
import java.util.List;

public interface RecapitulatifEncoursRepository
        extends JpaRepository<RecapitulatifEncoursView, RecapitulatifEncoursId>,
        JpaSpecificationExecutor<RecapitulatifEncoursView>,
        RecapitulatifEncoursRepositoryCustom {

    /**
     * Somme par (sourceType, sens) pour une banque sur une plage de dates — alimente
     * {@code RecapGeneralDetailleServiceImpl.buildDecaissements/buildEncaissements}
     * (regroupement par catégorie fait ensuite en Java, sur un nombre de lignes borné par le
     * nombre de sources, pas par le nombre d'encours).
     */
    @Query("SELECT v.id.sourceType, v.sens, SUM(v.montant) FROM RecapitulatifEncoursView v " +
            "WHERE v.banqueId = :banqueId AND v.dateReference BETWEEN :from AND :until " +
            "GROUP BY v.id.sourceType, v.sens")
    List<Object[]> sumByBanqueAndSourceBetween(
            @Param("banqueId") Long banqueId, @Param("from") LocalDate from, @Param("until") LocalDate until);

    /**
     * Somme par sens (DECAISSEMENT/ENCAISSEMENT) pour une banque jusqu'à une date — alimente
     * {@code RecapGeneralDetailleServiceImpl.buildResume} (encoursMoins/encoursPlus).
     */
    @Query("SELECT v.sens, SUM(v.montant) FROM RecapitulatifEncoursView v " +
            "WHERE v.banqueId = :banqueId AND v.dateReference <= :until GROUP BY v.sens")
    List<Object[]> sumByBanqueAndSensUntil(@Param("banqueId") Long banqueId, @Param("until") LocalDate until);

    /**
     * Somme par (banque, sens) pour TOUTES les banques jusqu'à une date — une seule requête
     * pour les cartes de l'accueil (évite le N+1).
     */
    @Query("SELECT v.banqueId, v.sens, SUM(v.montant) FROM RecapitulatifEncoursView v " +
            "WHERE v.dateReference <= :until GROUP BY v.banqueId, v.sens")
    List<Object[]> sumAllBanquesBySensUntil(@Param("until") LocalDate until);
}
