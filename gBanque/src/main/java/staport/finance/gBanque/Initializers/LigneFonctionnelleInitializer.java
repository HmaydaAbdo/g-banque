package staport.finance.gBanque.Initializers;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.repositories.NatureLigneFonctionnelleRepository;
import staport.finance.gBanque.LigneFonctionnelle.entities.LigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.repositories.LigneFonctionnelleRepository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Configuration
public class LigneFonctionnelleInitializer {

    private static final Logger log = LoggerFactory.getLogger(LigneFonctionnelleInitializer.class);
    private static final Random random = new Random();

    // Define the start and end years for initialization
    private static final int START_YEAR = 2015;
    private static final int END_YEAR = 2025; // Including the current year

    @Bean
    @Order(4)
    CommandLineRunner ligneFonctionnelleDataInitializer(
            LigneFonctionnelleRepository ligneFonctionnelleRepository,
            BanqueRepository banqueRepository,
            NatureLigneFonctionnelleRepository natureLigneFonctionnelleRepository
    ) {
        return args -> {
            log.info("Initializing LigneFonctionnelle data yearly ({} to {})...", START_YEAR, END_YEAR);

            List<Banque> banks = banqueRepository.findAll();
            List<NatureLigneFonctionnelle> natureLignes = natureLigneFonctionnelleRepository.findAll();

            if (banks.isEmpty()) {
                log.warn("No Banks found. Cannot initialize LigneFonctionnelle data. Please ensure BanqueInitializer runs first.");
                return;
            }
            if (natureLignes.isEmpty()) {
                log.warn("No NatureLigneFonctionnelle found. Cannot initialize LigneFonctionnelle data. Please ensure NatureLigneFonctionnelleInitializer runs first.");
                return;
            }

            int newLignesCount = 0;
            for (Banque bank : banks) {
                for (NatureLigneFonctionnelle natureLigne : natureLignes) {
                    log.debug("Processing LigneFonctionnelle for Bank: {} and Nature: {}", bank.getNom(), natureLigne.getLabel());

                    // Track the previously active line for this bank-nature combo
                    Optional<LigneFonctionnelle> currentActiveLine = Optional.empty();

                    for (int year = START_YEAR; year <= END_YEAR; year++) {
                        log.debug("  Processing year: {}", year);

                        // 1. Deactivate the old line (if any) that was active for the previous year
                        // We do this by finding the currently active one and setting it to false.
                        // This logic handles the case where an active line might already exist from a previous run or another initializer.
                        // Also, when moving from year to year, the 'currentActiveLine' variable holds the one from the previous iteration.
                        if (currentActiveLine.isPresent()) {
                            LigneFonctionnelle lineToDeactivate = currentActiveLine.get();
                            lineToDeactivate.setActif(false);
                            ligneFonctionnelleRepository.save(lineToDeactivate);
                            log.debug("    Deactivated old LigneFonctionnelle ID: {}", lineToDeactivate.getId());
                        }

                        // 2. Create and activate a new line for the current year
                        OffsetDateTime dateDepart = OffsetDateTime.of(year, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(1));
                        // Set dateFin to end of the current year or later to ensure validity for the year
                        OffsetDateTime dateFin = OffsetDateTime.of(year, 12, 31, 23, 59, 59, 999999999, ZoneOffset.ofHours(1));

                        // Ensure dateFin is always after dateDepart
                        if (dateFin.isBefore(dateDepart)) {
                            dateFin = dateDepart.plusYears(1); // Fallback to 1 year duration
                        }

                        BigDecimal plafond = BigDecimal.valueOf(100000 + random.nextInt(9900000)).setScale(2, BigDecimal.ROUND_HALF_UP); // Between 100,000 and 10,000,000
                        BigDecimal taux = BigDecimal.valueOf(0.01 + (0.09 * random.nextDouble()))
                                .setScale(4, BigDecimal.ROUND_HALF_UP); // Between 0.01 and 0.10

                        LigneFonctionnelle newLine = new LigneFonctionnelle();
                        newLine.setBanque(bank);
                        newLine.setNatureLigneFonctionnelle(natureLigne);
                        newLine.setPlafond(plafond);
                        newLine.setTaux(taux);
                        newLine.setDateDepart(dateDepart);
                        newLine.setDateFin(dateFin);
                        newLine.setActif(true); // This one is explicitly active

                        // Check for true duplicates based on all defining fields before saving
                        // This helps idempotency in case of re-runs, though yearly approach minimizes exact date overlaps
                        Optional<LigneFonctionnelle> existingExactMatch = ligneFonctionnelleRepository.findByBanqueAndNatureLigneFonctionnelleAndDateDepartAndDateFin(
                                newLine.getBanque(),
                                newLine.getNatureLigneFonctionnelle(),
                                newLine.getDateDepart(),
                                newLine.getDateFin()
                        );

                        if (existingExactMatch.isEmpty()) {
                            LigneFonctionnelle savedLine = ligneFonctionnelleRepository.save(newLine);
                            currentActiveLine = Optional.of(savedLine); // This is now the 'active' line for the next iteration
                            newLignesCount++;
                            log.debug("    Saved new active LigneFonctionnelle: ID={}, Year={}", savedLine.getId(), year);
                        } else {
                            // If an exact duplicate exists, we assume it's the one we should consider "active" for this year
                            // This might happen if the initializer ran before for this specific year/bank/nature combination
                            currentActiveLine = existingExactMatch;
                            // Ensure it's active in the DB if it's supposed to be
                            if (!currentActiveLine.get().isActif()) {
                                currentActiveLine.get().setActif(true);
                                ligneFonctionnelleRepository.save(currentActiveLine.get());
                                log.debug("    Found existing but inactive line for year {}. Activated it. ID: {}", year, currentActiveLine.get().getId());
                            } else {
                                log.debug("    Found existing active line for year {}. Skipping creation. ID: {}", year, existingExactMatch.get().getId());
                            }
                        }
                    }
                }
            }

            if (newLignesCount > 0) {
                log.info("Successfully saved {} new LigneFonctionnelle entities for yearly progression.", newLignesCount);
            } else {
                log.info("No new LigneFonctionnelle entities to save for yearly progression, or all existed.");
            }

            log.info("LigneFonctionnelle yearly data initialization complete.");
        };
    }


}
