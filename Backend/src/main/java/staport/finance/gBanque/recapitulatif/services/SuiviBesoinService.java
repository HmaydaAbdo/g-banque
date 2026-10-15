package staport.finance.gBanque.recapitulatif.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.recapitulatif.dtos.response.PlanBanqueDto;
import staport.finance.gBanque.recapitulatif.entities.SuiviBesoinBanque;
import staport.finance.gBanque.recapitulatif.enums.PeriodeRecap;
import staport.finance.gBanque.recapitulatif.repositories.SuiviBesoinBanqueRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Suivi de l'ancienneté du besoin de financement de chaque banque : enregistrement quotidien du besoin,
 * puis calcul de « en besoin depuis le JJ/MM » à partir de cet historique.
 */
@Service
public class SuiviBesoinService {

    /** Au-delà d'un an d'historique, l'ancienneté n'a plus de sens pour la trésorerie. */
    private static final int JOURS_HISTORIQUE = 366;

    private final SuiviBesoinBanqueRepository repository;
    private final BanqueRepository banqueRepository;

    public SuiviBesoinService(SuiviBesoinBanqueRepository repository, BanqueRepository banqueRepository) {
        this.repository = repository;
        this.banqueRepository = banqueRepository;
    }

    /** Enregistre le besoin du jour de chaque banque du plan ; rejouable le même jour (remplace les lignes du jour). */
    @Transactional
    public void enregistrer(LocalDate jour, PeriodeRecap periode, List<PlanBanqueDto> banques) {
        repository.supprimerDuJour(periode, jour);
        repository.flush();
        repository.saveAll(banques.stream()
                .map(b -> new SuiviBesoinBanque(jour, banqueRepository.getReferenceById(b.banqueId()), periode, b.besoinFinancement()))
                .toList());
    }

    /**
     * Jour à partir duquel chaque banque actuellement en besoin l'est sans interruption : le premier jour
     * enregistré après son dernier jour sans besoin, ou {@code aujourdhui} si l'historique ne dit rien de plus.
     * Une banque sans besoin n'a pas d'entrée dans le résultat.
     */
    @Transactional(readOnly = true)
    public Map<Long, LocalDate> enBesoinDepuis(PeriodeRecap periode, LocalDate aujourdhui, List<PlanBanqueDto> banques) {
        Map<Long, LocalDate> premierBesoinApresLeDernierJourCouvert = new HashMap<>();
        for (Object[] ligne : repository.historique(periode, aujourdhui.minusDays(JOURS_HISTORIQUE))) {
            Long banqueId = (Long) ligne[0];
            if (((BigDecimal) ligne[2]).signum() == 0) {
                premierBesoinApresLeDernierJourCouvert.remove(banqueId);
            } else {
                premierBesoinApresLeDernierJourCouvert.putIfAbsent(banqueId, (LocalDate) ligne[1]);
            }
        }
        Map<Long, LocalDate> depuis = new HashMap<>();
        for (PlanBanqueDto b : banques) {
            if (b.besoinFinancement().signum() > 0) {
                depuis.put(b.banqueId(), premierBesoinApresLeDernierJourCouvert.getOrDefault(b.banqueId(), aujourdhui));
            }
        }
        return depuis;
    }
}
