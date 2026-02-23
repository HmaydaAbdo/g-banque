package staport.finance.gBanque.Agios.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Agios.dtos.AgiosDetailAggregateDto;
import staport.finance.gBanque.Agios.dtos.AgiosJournalierSearchDto;
import staport.finance.gBanque.Agios.dtos.AgiosRequestDto;
import staport.finance.gBanque.Agios.dtos.AgiosTrimestrielSearchDto;
import staport.finance.gBanque.Agios.entities.AgiosJournalier;
import staport.finance.gBanque.Agios.entities.AgiosTrimestriel;
import staport.finance.gBanque.Agios.enums.StatutTrimestre;
import staport.finance.gBanque.Agios.enums.TypeAgios;
import staport.finance.gBanque.Agios.mapper.AgiosMapper;
import staport.finance.gBanque.Agios.repository.AgiosJournalierRepository;
import staport.finance.gBanque.Agios.repository.AgiosTrimestrielRepository;
import staport.finance.gBanque.Agios.service.calculateurs.AgiosCalculateur;
import staport.finance.gBanque.Agios.service.calculateurs.AgiosCalculateur.ResultatJournalier;
import staport.finance.gBanque.Agios.spec.AgiosJournalierSpecification;
import staport.finance.gBanque.Agios.spec.AgiosTrimestrielSpecification;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.repositories.TransactionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AgiosOrchestrateurService {

    private static final Logger log = LoggerFactory.getLogger(AgiosOrchestrateurService.class);
    private static final BigDecimal TAUX_TVA = new BigDecimal("10");
    private static final int SCALE = 4;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final List<AgiosCalculateur> calculateurs;
    private final TransactionRepository transactionRepository;
    private final BanqueRepository banqueRepository;
    private final AgiosJournalierRepository agiosJournalierRepository;
    private final AgiosTrimestrielRepository agiosTrimestrielRepository;
    private final AgiosService agiosService;

    public AgiosOrchestrateurService(List<AgiosCalculateur> calculateurs,
                                     TransactionRepository transactionRepository,
                                     BanqueRepository banqueRepository,
                                     AgiosJournalierRepository agiosJournalierRepository,
                                     AgiosTrimestrielRepository agiosTrimestrielRepository,
                                     AgiosService agiosService) {
        this.calculateurs = calculateurs;
        this.transactionRepository = transactionRepository;
        this.banqueRepository = banqueRepository;
        this.agiosJournalierRepository = agiosJournalierRepository;
        this.agiosTrimestrielRepository = agiosTrimestrielRepository;
        this.agiosService = agiosService;

        log.info("Orchestrateur agios initialisé avec {} calculateur(s) : {}",
                calculateurs.size(),
                calculateurs.stream().map(c -> c.getType().name()).collect(Collectors.joining(", ")));
    }

    // ═══════════════════════════════════════════════════════════════
    //  SCHEDULER — Chaque nuit à 02h00
    // ═══════════════════════════════════════════════════════════════

    @Scheduled(cron = "0 0 2 * * *")
    public void calculQuotidienAutomatique() {
        log.info("══════ Calcul quotidien des agios — {} type(s) actif(s) ══════", calculateurs.size());

        LocalDate aujourdhui = LocalDate.now();
        List<Banque> banques = banqueRepository.findAll();

        for (AgiosCalculateur calculateur : calculateurs) {
            log.info("── Type : {} ──", calculateur.getType().getLibelle());

            int succes = 0, nonEligibles = 0, erreurs = 0;

            for (Banque banque : banques) {
                try {
                    if (!calculateur.estEligible(banque)) {
                        nonEligibles++;
                        continue;
                    }

                    recalculerTrimestre(banque, calculateur, aujourdhui);
                    succes++;

                } catch (Exception e) {
                    erreurs++;
                    log.error("{} — Banque {} (ID:{}) : {}",
                            calculateur.getType(), banque.getNom(), banque.getId(), e.getMessage(), e);
                }
            }

            log.info("{} : {} OK, {} non éligibles, {} erreurs",
                    calculateur.getType(), succes, nonEligibles, erreurs);
        }

        log.info("══════ Fin du calcul quotidien ══════");
    }

    // ═══════════════════════════════════════════════════════════════
    //  RECALCUL COMPLET D'UN TRIMESTRE
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public void recalculerTrimestre(Banque banque, AgiosCalculateur calculateur, LocalDate dateRef) {
        TypeAgios type = calculateur.getType();
        LocalDate debutTrim = debutDuTrimestre(dateRef);
        LocalDate finTrim = finDuTrimestre(dateRef);
        // On limite la date de fin au maximum à hier si le trimestre n'est pas encore terminé.
        LocalDate finCalcul = finTrim.isAfter(LocalDate.now()) ? LocalDate.now().minusDays(1) : finTrim;

        if (finCalcul.isBefore(debutTrim)) return;

        AgiosTrimestriel trimestre = obtenirOuCreerTrimestre(banque, type, dateRef);

        if (!trimestre.isOuvert()) {
            log.debug("{} T{}/{} banque {} : clôturé, ignoré",
                    type, trimestre.getTrimestre(), trimestre.getAnnee(), banque.getNom());
            return;
        }

        // Reconstituer le solde initial
        BigDecimal soldeInitial = reconstituerSoldeAvantDate(banque.getId(), debutTrim, banque.getSoldeActuel());

        // Charger les transactions du trimestre
        List<Transaction> transactions = chargerTransactions(banque.getId(), debutTrim, finCalcul);

        Map<LocalDate, List<Transaction>> txParJour = transactions.stream()
                .filter(t -> t.getDateValeur() != null)
                .collect(Collectors.groupingBy(t -> t.getDateValeur().toLocalDate()));

        // Calculer jour par jour
        BigDecimal soldeCourant = soldeInitial;
        BigDecimal totalHT = BigDecimal.ZERO, totalTVA = BigDecimal.ZERO, totalTTC = BigDecimal.ZERO;
        int nbJoursCalcules = 0;
        long nbJours = ChronoUnit.DAYS.between(debutTrim, finCalcul) + 1;

        for (long i = 0; i < nbJours; i++) {
            LocalDate jour = debutTrim.plusDays(i);

            // Appliquer les mouvements du jour
            for (Transaction tx : txParJour.getOrDefault(jour, Collections.emptyList())) {
                BigDecimal credit = tx.getCredit() != null ? tx.getCredit() : BigDecimal.ZERO;
                BigDecimal debit = tx.getDebit() != null ? tx.getDebit() : BigDecimal.ZERO;
                soldeCourant = soldeCourant.add(credit).subtract(debit);
            }

            // Déléguer le calcul au calculateur spécifique
            ResultatJournalier resultat = calculateur.calculerJour(banque, jour, soldeCourant);

            // Calculer TVA et TTC (commun à tous les types)
            BigDecimal tvaJour = resultat.agiosHT()
                    .multiply(TAUX_TVA)
                    .divide(BigDecimal.valueOf(100), SCALE, ROUNDING);

            BigDecimal ttcJour = resultat.agiosHT().add(tvaJour);

            // Persister le jour
            enregistrerJournalier(jour, banque, type, trimestre, soldeCourant, resultat, tvaJour, ttcJour);

            totalHT = totalHT.add(resultat.agiosHT());
            totalTVA = totalTVA.add(tvaJour);
            totalTTC = totalTTC.add(ttcJour);
            if (resultat.baseCalcul().compareTo(BigDecimal.ZERO) > 0) nbJoursCalcules++;
        }

        // Mettre à jour les totaux du trimestre
        trimestre.setTotalAgiosHT(totalHT.setScale(2, ROUNDING));
        trimestre.setTotalTVA(totalTVA.setScale(2, ROUNDING));
        trimestre.setTotalAgiosTTC(totalTTC.setScale(2, ROUNDING));
        trimestre.setNbJoursCalcules(nbJoursCalcules);
        trimestre.setNbJoursTotal((int) nbJours);
        trimestre.setDateDernierCalcul(OffsetDateTime.now());
        agiosTrimestrielRepository.save(trimestre);

        log.info("{} — {} T{}/{} : HT={} TVA={} TTC={} | {}/{} jours",
                type, banque.getNom(), trimestre.getTrimestre(), trimestre.getAnnee(),
                trimestre.getTotalAgiosHT(), trimestre.getTotalTVA(), trimestre.getTotalAgiosTTC(),
                nbJoursCalcules, nbJours);
    }

    // ═══════════════════════════════════════════════════════════════
    //  CLÔTURE TRIMESTRIELLE
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public AgiosTrimestriel cloturerTrimestre(Long banqueId, TypeAgios type, int trimNum, int annee) {
        log.info("Clôture {} T{}/{} banque ID:{}", type, trimNum, annee, banqueId);

        AgiosTrimestriel trimestre = agiosTrimestrielRepository
                .findByTrimestreAndAnneeAndBanqueIdAndTypeAgios(trimNum, annee, banqueId, type)
                .orElseThrow(() -> new ResourceNotFoundException(
                        type + " T" + trimNum + "/" + annee + " non trouvé pour banque " + banqueId));

        if (!trimestre.isOuvert()) {
            throw new IllegalStateException(type + " T" + trimNum + "/" + annee + " déjà clôturé");
        }


        // Générer l'agios définitif si montant > 0
        if (trimestre.getTotalAgiosTTC().compareTo(BigDecimal.ZERO) > 0) {
            AgiosRequestDto req = new AgiosRequestDto();
            req.setLibelle(String.format("Agios %s T%d/%d", type.getLibelle(), trimNum, annee));
            req.setObservation(String.format(
                    "Clôture trimestrielle | HT: %s | TVA: %s | Jours: %d/%d",
                    trimestre.getTotalAgiosHT(), trimestre.getTotalTVA(),
                    trimestre.getNbJoursCalcules(), trimestre.getNbJoursTotal()));
            req.setMontant(trimestre.getTotalAgiosTTC());
            req.setBanqueId(banqueId);
            req.setDateEmission(OffsetDateTime.now());

            var agiosCree = agiosService.createAgios(req);
            trimestre.setAgiosDefinitifId(agiosCree.getId());
        }

        trimestre.setStatut(StatutTrimestre.CLOTURE);
        trimestre.setDateCloture(OffsetDateTime.now());
        agiosTrimestrielRepository.save(trimestre);

        log.info("{} T{}/{} clôturé — TTC={}", type, trimNum, annee, trimestre.getTotalAgiosTTC());
        return trimestre;
    }

    // ═══════════════════════════════════════════════════════════════
    //  CONSULTATION
    // ═══════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public Page<AgiosTrimestriel> searchTrimestres(AgiosTrimestrielSearchDto dto, Pageable pageable) {
        Specification<AgiosTrimestriel> spec = AgiosTrimestrielSpecification.buildSpec(
                dto.getBanqueId(), dto.getType(), dto.getAnnee(),
                dto.getTrimestre(), dto.getStatut(), dto.getSearch());
        return agiosTrimestrielRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AgiosJournalier> searchJournaliers(AgiosJournalierSearchDto dto, Pageable pageable) {
        LocalDate debut = debutDuTrimestrePourNumero(dto.getTrimestre(), dto.getAnnee());
        LocalDate fin = finDuTrimestrePourNumero(dto.getTrimestre(), dto.getAnnee());
        Specification<AgiosJournalier> spec = AgiosJournalierSpecification.buildSpec(
                dto.getBanqueId(), dto.getType(), debut, fin, dto.getDateLimite());
        return agiosJournalierRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public AgiosDetailAggregateDto getAggregates(AgiosJournalierSearchDto dto) {
        LocalDate debut = debutDuTrimestrePourNumero(dto.getTrimestre(), dto.getAnnee());
        LocalDate fin = finDuTrimestrePourNumero(dto.getTrimestre(), dto.getAnnee());
        Specification<AgiosJournalier> spec = AgiosJournalierSpecification.buildSpec(
                dto.getBanqueId(), dto.getType(), debut, fin, dto.getDateLimite());

        List<AgiosJournalier> jours = agiosJournalierRepository.findAll(spec);

        BigDecimal totalHT = BigDecimal.ZERO;
        BigDecimal totalTVA = BigDecimal.ZERO;
        BigDecimal totalTTC = BigDecimal.ZERO;
        int nbJoursDecouvert = 0;

        for (AgiosJournalier j : jours) {
            totalHT = totalHT.add(j.getAgiosHT() != null ? j.getAgiosHT() : BigDecimal.ZERO);
            totalTVA = totalTVA.add(j.getTva() != null ? j.getTva() : BigDecimal.ZERO);
            totalTTC = totalTTC.add(j.getAgiosTTC() != null ? j.getAgiosTTC() : BigDecimal.ZERO);
            if (j.getBaseCalcul() != null && j.getBaseCalcul().compareTo(BigDecimal.ZERO) > 0) {
                nbJoursDecouvert++;
            }
        }

        return new AgiosDetailAggregateDto(
                totalHT.setScale(2, RoundingMode.HALF_UP),
                totalTVA.setScale(2, RoundingMode.HALF_UP),
                totalTTC.setScale(2, RoundingMode.HALF_UP),
                nbJoursDecouvert,
                jours.size()
        );
    }

    public List<TypeAgios> getTypesActifs() {
        return calculateurs.stream().map(AgiosCalculateur::getType).collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════
    //  MÉTHODES PRIVÉES
    // ═══════════════════════════════════════════════════════════════

    private void enregistrerJournalier(LocalDate jour, Banque banque, TypeAgios type,
                                       AgiosTrimestriel trimestre, BigDecimal solde,
                                       ResultatJournalier resultat, BigDecimal tva, BigDecimal ttc) {

        AgiosJournalier aj = agiosJournalierRepository
                .findByDateValeurAndBanqueIdAndTypeAgios(jour, banque.getId(), type)
                .orElse(new AgiosJournalier());

        aj.setDateValeur(jour);
        aj.setBanque(banque);
        aj.setTypeAgios(type);
        aj.setTrimestre(trimestre);
        aj.setSoldeJournalier(solde);
        aj.setBaseCalcul(resultat.baseCalcul());
        aj.setTauxApplique(resultat.tauxApplique());
        aj.setAgiosHT(resultat.agiosHT());
        aj.setTva(tva);
        aj.setAgiosTTC(ttc);
        aj.setDateCalcul(OffsetDateTime.now());
        aj.setNbRecalculs(aj.getNbRecalculs() + 1);
        aj.setMetadata(resultat.metadata());

        agiosJournalierRepository.save(aj);
    }

    private AgiosTrimestriel obtenirOuCreerTrimestre(Banque banque, TypeAgios type, LocalDate dateRef) {
        int trimNum = trimestreDeLaDate(dateRef);
        int annee = dateRef.getYear();

        return agiosTrimestrielRepository
                .findByTrimestreAndAnneeAndBanqueIdAndTypeAgios(trimNum, annee, banque.getId(), type)
                .orElseGet(() -> {
                    AgiosTrimestriel t = new AgiosTrimestriel();
                    t.setTypeAgios(type);
                    t.setTrimestre(trimNum);
                    t.setAnnee(annee);
                    t.setBanque(banque);
                    t.setDateDebut(debutDuTrimestrePourNumero(trimNum, annee));
                    t.setDateFin(finDuTrimestrePourNumero(trimNum, annee));
                    t.setStatut(StatutTrimestre.OUVERT);
                    return agiosTrimestrielRepository.save(t);
                });
    }

    private BigDecimal reconstituerSoldeAvantDate(Long banqueId, LocalDate date, BigDecimal soldeActuel) {
        OffsetDateTime dateDebut = date.atStartOfDay().atOffset(ZoneOffset.UTC);
        Specification<Transaction> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("banqueDePrelevement").get("id"), banqueId),
                cb.greaterThanOrEqualTo(root.get("dateValeur"), dateDebut));

        BigDecimal solde = soldeActuel ;
        for (Transaction t : transactionRepository.findAll(spec)) {
            BigDecimal credit = t.getCredit() != null ? t.getCredit() : BigDecimal.ZERO;
            BigDecimal debit = t.getDebit() != null ? t.getDebit() : BigDecimal.ZERO;
            solde = solde.subtract(credit).add(debit);
        }
        return solde;
    }

    private List<Transaction> chargerTransactions(Long banqueId, LocalDate debut, LocalDate fin) {
        OffsetDateTime dDebut = debut.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime dFin = fin.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        Specification<Transaction> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("banqueDePrelevement").get("id"), banqueId),
                cb.greaterThanOrEqualTo(root.get("dateValeur"), dDebut),
                cb.lessThan(root.get("dateValeur"), dFin));
        return transactionRepository.findAll(spec);
    }

    // --- Utilitaires trimestre ---
    private int trimestreDeLaDate(LocalDate d) { return (d.getMonthValue() - 1) / 3 + 1; }
    private LocalDate debutDuTrimestre(LocalDate d) { return debutDuTrimestrePourNumero(trimestreDeLaDate(d), d.getYear()); }
    private LocalDate finDuTrimestre(LocalDate d) { return finDuTrimestrePourNumero(trimestreDeLaDate(d), d.getYear()); }
    private LocalDate debutDuTrimestrePourNumero(int t, int a) { return LocalDate.of(a, (t - 1) * 3 + 1, 1); }
    private LocalDate finDuTrimestrePourNumero(int t, int a) { return LocalDate.of(a, t * 3, 1).plusMonths(1).minusDays(1); }
}