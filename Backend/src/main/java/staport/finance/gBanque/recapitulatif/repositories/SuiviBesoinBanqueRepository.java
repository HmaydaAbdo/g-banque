package staport.finance.gBanque.recapitulatif.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import staport.finance.gBanque.recapitulatif.entities.SuiviBesoinBanque;
import staport.finance.gBanque.recapitulatif.enums.PeriodeRecap;

import java.time.LocalDate;
import java.util.List;

public interface SuiviBesoinBanqueRepository extends JpaRepository<SuiviBesoinBanque, Long> {

    /** Historique d'un horizon depuis {@code depuis} : (banque, jour, besoin), du plus ancien au plus récent. */
    @Query("select s.banque.id, s.jour, s.besoin from SuiviBesoinBanque s " +
            "where s.periode = :periode and s.jour >= :depuis order by s.jour")
    List<Object[]> historique(@Param("periode") PeriodeRecap periode, @Param("depuis") LocalDate depuis);

    @Modifying
    @Query("delete from SuiviBesoinBanque s where s.periode = :periode and s.jour = :jour")
    void supprimerDuJour(@Param("periode") PeriodeRecap periode, @Param("jour") LocalDate jour);
}
