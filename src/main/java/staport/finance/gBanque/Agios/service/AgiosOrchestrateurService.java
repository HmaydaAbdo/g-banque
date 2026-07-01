package staport.finance.gBanque.Agios.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import staport.finance.gBanque.Agios.repository.AgiosJournalierRepository;
import staport.finance.gBanque.Agios.repository.AgiosTrimestrielRepository;
import staport.finance.gBanque.Agios.service.calculateurs.AgiosCalculateur;
import staport.finance.gBanque.Agios.service.calculateurs.AgiosCalculateur.ResultatJournalier;
import staport.finance.gBanque.Agios.service.calculateurs.AgiosCalculateurMultiMarche;
import staport.finance.gBanque.Agios.service.calculateurs.PrefinancementAgiosCalculateur;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.Agios.spec.AgiosJournalierSpecification;
import staport.finance.gBanque.Agios.spec.AgiosTrimestrielSpecification;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.rapprochement.entities.Rapprochement;
import staport.finance.gBanque.rapprochement.repository.RapprochementRepository;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.repositories.TransactionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AgiosOrchestrateurService {

    private static final Logger log = LoggerFactory.getLogger(AgiosOrchestrateurService.class);
    private static final BigDecimal TAUX_TVA = new BigDecimal("10");
    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    /**
     * Fuseau horaire du Maroc : UTC+1 permanent (Africa/Casablanca).
     * Utilisé pour convertir les dateValeur (OffsetDateTime) en date locale
     * et pour l'horloge du scheduler.
     */
    private static final ZoneId ZONE_MAROC = ZoneId.of("Africa/Casablanca");

    private final List<AgiosCalculateur> calculateurs;
    private final TransactionRepository transactionRepository;
    private final BanqueRepository banqueRepository;
    private final AgiosJournalierRepository agiosJournalierRepository;
    private final AgiosTrimestrielRepository agiosTrimestrielRepository;
    private final AgiosService agiosService;
    private final RapprochementRepository rapprochementRepository;

    public AgiosOrchestrateurService(List<AgiosCalculateur> calculateurs,
                                     TransactionRepository transactionRepository,
                                     BanqueRepository banqueRepository,
                                     AgiosJournalierRepository agiosJournalierRepository,
                                     AgiosTrimestrielRepository agiosTrimestrielRepository,
                                     AgiosService agiosService,
                                     RapprochementRepository rapprochementRepository) {
        this.calculateurs = calculateurs;
        this.transactionRepository = transactionRepository;
        this.banqueRepository = banqueRepository;
        this.agiosJournalierRepository = agiosJournalierRepository;
        this.agiosTrimestrielRepository = agiosTrimestrielRepository;
        this.agiosService = agiosService;
        this.rapprochementRepository = rapprochementRepository;

        log.info("Orchestrateur agios initialisé avec {} calculateur(s) : {}",
                calculateurs.size(),
                calculateurs.stream().map(c -> c.getType().name()).collect(Collectors.joining(", ")));
    }

    // ═══════════════════════════════════════════════════════════════
    //  SCHEDULER — Chaque nuit à 02h00 heure de Casablanca
    // ═══════════════════════════════════════════════════════════════

    @Scheduled(cron = "0 0 2 * * *", zone = "Africa/Casablanca")
    public void calculQuotidienAutomatique() {
        LocalDate aujourdhui = LocalDate.now(ZONE_MAROC);
        int trimActuel = trimestreDeLaDate(aujourdhui);
        int anneeActuelle = aujourdhui.getYear();

        log.info("══════ Calcul quotidien des agios — {} — {} type(s) actif(s) ══════",
                aujourdhui, calculateurs.size());

        List<Banque> banques = banqueRepository.findAll();

        for (AgiosCalculateur calculateur : calculateurs) {
            log.info("── Type : {} ──", calculateur.getType().getLibelle());

            int succes = 0, nonEligibles = 0, erreurs = 0;

            for (Banque banque : banques) {
                // Vider le cache de taux avant chaque banque pour éviter des données périmées
                if (calculateur instanceof PrefinancementAgiosCalculateur prefiCalc) {
                    prefiCalc.viderCache();
                }

                try {
                    if (!calculateur.estEligible(banque)) {
                        nonEligibles++;
                        continue;
                    }

                    // Tous les trimestres OUVERTS de cette banque (courant + passés non clôturés).
                    // Un trimestre reste recalculé tant qu'il n'est pas clôturé, ce qui permet
                    // de répercuter toute modification de transaction sur les agios correspondants.
                    List<AgiosTrimestriel> ouverts = agiosTrimestrielRepository
                            .findByBanqueIdAndTypeAgiosAndStatut(
                                    banque.getId(), calculateur.getType(), StatutTrimestre.OUVERT);

                    // S'assurer que le trimestre courant est toujours traité,
                    // même s'il n'a pas encore été créé (obtenirOuCreerTrimestre le crée).
                    boolean courantPresent = ouverts.stream()
                            .anyMatch(t -> t.getTrimestre() == trimActuel && t.getAnnee() == anneeActuelle);

                    if (!courantPresent) {
                        recalculerTrimestre(banque, calculateur, trimActuel, anneeActuelle);
                    }

                    for (AgiosTrimestriel t : ouverts) {
                        recalculerTrimestre(banque, calculateur, t.getTrimestre(), t.getAnnee());
                    }

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
    //  RECALCUL MANUEL — Un trimestre précis pour une banque
    // ═══════════════════════════════════════════════════════════════

    /**
     * Recalcul manuel déclenché par action utilisateur pour un trimestre
     * donné (courant ou passé, tant qu'il n'est pas clôturé).
     */
    @Transactional
    public void recalculerTrimestreManuel(Long banqueId, int trimNum, int annee) {
        LocalDate aujourdhui = LocalDate.now(ZONE_MAROC);
        int trimActuel = trimestreDeLaDate(aujourdhui);
        int anneeActuelle = aujourdhui.getYear();

        if (annee > anneeActuelle || (annee == anneeActuelle && trimNum > trimActuel)) {
            throw new IllegalArgumentException(
                    "Impossible de calculer un trimestre futur : T" + trimNum + "/" + annee);
        }

        Banque banque = banqueRepository.findById(banqueId)
                .orElseThrow(() -> new ResourceNotFoundException("Banque non trouvée : " + banqueId));

        log.info("══════ Recalcul manuel T{}/{} — Banque {} ══════", trimNum, annee, banque.getNom());

        for (AgiosCalculateur calculateur : calculateurs) {
            if (!calculateur.estEligible(banque)) {
                log.debug("{} non éligible pour banque {}", calculateur.getType(), banque.getNom());
                continue;
            }
            recalculerTrimestre(banque, calculateur, trimNum, annee);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  RECALCUL COMPLET D'UN TRIMESTRE
    // ═══════════════════════════════════════════════════════════════

    /**
     * Recalcule intégralement l'AgiosTrimestriel pour une banque, un calculateur
     * et un trimestre donné (numéro + année).
     *
     * <p>Principe de calcul bancaire :<br>
     * – Le solde de départ est le {@code soldeDebut} du premier rapprochement
     *   mensuel du trimestre (solde comptable au 1er jour du trimestre).<br>
     * – Les transactions sont chargées depuis les rapprochements des 3 mois
     *   du trimestre, puis appliquées jour par jour selon leur {@code dateValeur}
     *   (date à partir de laquelle la transaction produit un effet financier réel),
     *   convertie en heure locale marocaine (Africa/Casablanca).
     * </p>
     */
    @Transactional
    public void recalculerTrimestre(Banque banque, AgiosCalculateur calculateur, int trimNum, int annee) {
        TypeAgios type = calculateur.getType();
        LocalDate debutTrim = debutDuTrimestrePourNumero(trimNum, annee);
        LocalDate finTrim   = finDuTrimestrePourNumero(trimNum, annee);

        // On ne calcule pas au-delà d'hier (on ne prédit pas le futur)
        LocalDate aujourdhui = LocalDate.now(ZONE_MAROC);
        LocalDate finCalcul  = finTrim.isAfter(aujourdhui) ? aujourdhui.minusDays(1) : finTrim;

        if (finCalcul.isBefore(debutTrim)) return;

        AgiosTrimestriel trimestre = obtenirOuCreerTrimestre(banque, type, trimNum, annee);

        if (!trimestre.isOuvert()) {
            log.debug("{} T{}/{} banque {} : clôturé, ignoré",
                    type, trimNum, annee, banque.getNom());
            return;
        }

        long nbJours = ChronoUnit.DAYS.between(debutTrim, finCalcul) + 1;
        BigDecimal totalHT = BigDecimal.ZERO, totalTVA = BigDecimal.ZERO, totalTTC = BigDecimal.ZERO;
        int nbJoursCalcules = 0;

        if (calculateur instanceof AgiosCalculateurMultiMarche multiCalculateur) {
            // ════════════════════════════════════════════════════════════
            //  FLUX MULTI-MARCHÉ (Préfinancement)
            //  L'encours est reconstitué depuis les Prefinancement PAYE,
            //  indépendamment du solde comptable du rapprochement.
            // ════════════════════════════════════════════════════════════
            for (long i = 0; i < nbJours; i++) {
                LocalDate jour = debutTrim.plusDays(i);

                List<AgiosCalculateurMultiMarche.ResultatParMarche> resultats =
                        multiCalculateur.calculerJourParMarche(banque, jour);

                // Un jour est "actif" si au moins un marché a un encours > 0 ce jour-là
                boolean jourAvecEncours = false;
                for (AgiosCalculateurMultiMarche.ResultatParMarche r : resultats) {
                    ResultatJournalier res = r.resultat();

                    BigDecimal tvaJour = res.agiosHT()
                            .multiply(TAUX_TVA)
                            .divide(BigDecimal.valueOf(100), SCALE, ROUNDING);
                    BigDecimal ttcJour = res.agiosHT().add(tvaJour);

                    enregistrerJournalier(jour, banque, type, trimestre,
                            res.baseCalcul(), res, tvaJour, ttcJour, r.marche());

                    totalHT  = totalHT.add(res.agiosHT());
                    totalTVA = totalTVA.add(tvaJour);
                    totalTTC = totalTTC.add(ttcJour);
                    if (res.baseCalcul().compareTo(BigDecimal.ZERO) > 0) jourAvecEncours = true;
                }
                if (jourAvecEncours) nbJoursCalcules++;
            }

        } else {
            // ════════════════════════════════════════════════════════════
            //  FLUX SOLDE COMPTABLE (Facilité de Caisse et autres)
            //  L'encours est reconstitué depuis le solde du rapprochement.
            // ════════════════════════════════════════════════════════════

            // ── Solde comptable de départ ──────────────────────────────────
            BigDecimal soldeInitial = getSoldeDebutTrimestre(banque.getId(), trimNum, annee);

            // ── Transactions du trimestre ──────────────────────────────────
            List<Transaction> transactions = chargerTransactionsParRapprochement(banque.getId(), trimNum, annee);

            // ── Transactions antérieures au trimestre → ajustement solde ──
            for (Transaction tx : transactions) {
                if (tx.getDateValeur() == null) continue;
                LocalDate dv = tx.getDateValeur();
                if (dv.isBefore(debutTrim)) {
                    BigDecimal credit = tx.getCredit() != null ? tx.getCredit() : BigDecimal.ZERO;
                    BigDecimal debit  = tx.getDebit()  != null ? tx.getDebit()  : BigDecimal.ZERO;
                    soldeInitial = soldeInitial.add(credit).subtract(debit);
                    log.debug("Tx id={} dateValeur={} antérieure au trimestre → ajustement soldeInitial",
                            tx.getId(), dv);
                }
            }

            // ── Groupement par dateValeur ──────────────────────────────────
            Map<LocalDate, List<Transaction>> txParJour = transactions.stream()
                    .filter(t -> t.getDateValeur() != null)
                    .filter(t -> {
                        LocalDate dv = t.getDateValeur();
                        return !dv.isBefore(debutTrim) && !dv.isAfter(finCalcul);
                    })
                    .collect(Collectors.groupingBy(Transaction::getDateValeur));

            // ── Calcul jour par jour ───────────────────────────────────────
            BigDecimal soldeCourant = soldeInitial;

            for (long i = 0; i < nbJours; i++) {
                LocalDate jour = debutTrim.plusDays(i);

                for (Transaction tx : txParJour.getOrDefault(jour, Collections.emptyList())) {
                    BigDecimal credit = tx.getCredit() != null ? tx.getCredit() : BigDecimal.ZERO;
                    BigDecimal debit  = tx.getDebit()  != null ? tx.getDebit()  : BigDecimal.ZERO;
                    soldeCourant = soldeCourant.add(credit).subtract(debit);
                }

                ResultatJournalier resultat = calculateur.calculerJour(banque, jour, soldeCourant);

                BigDecimal tvaJour = resultat.agiosHT()
                        .multiply(TAUX_TVA)
                        .divide(BigDecimal.valueOf(100), SCALE, ROUNDING);
                BigDecimal ttcJour = resultat.agiosHT().add(tvaJour);

                enregistrerJournalier(jour, banque, type, trimestre,
                        soldeCourant, resultat, tvaJour, ttcJour, null);

                totalHT  = totalHT.add(resultat.agiosHT());
                totalTVA = totalTVA.add(tvaJour);
                totalTTC = totalTTC.add(ttcJour);
                if (resultat.baseCalcul().compareTo(BigDecimal.ZERO) > 0) nbJoursCalcules++;
            }
        }

        // Mettre à jour les totaux du trimestre
        trimestre.setTotalAgiosHT(totalHT);
        trimestre.setTotalTVA(totalTVA);
        trimestre.setTotalAgiosTTC(totalTTC);
        trimestre.setNbJoursCalcules(nbJoursCalcules);
        // Dénominateur = total jours du trimestre (pas juste les jours calculés)
        trimestre.setNbJoursTotal((int) ChronoUnit.DAYS.between(debutTrim, finTrim) + 1);
        trimestre.setDateDernierCalcul(OffsetDateTime.now(ZONE_MAROC));
        agiosTrimestrielRepository.save(trimestre);

        log.info("{} — {} T{}/{} : HT={} TVA={} TTC={} | {}/{} jours",
                type, banque.getNom(), trimNum, annee,
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
            AgiosRequestDto req = new AgiosRequestDto(
                    String.format("Agios %s T%d/%d", type.getLibelle(), trimNum, annee),
                    String.format("Clôture trimestrielle | HT: %s | TVA: %s | Jours: %d/%d",
                            trimestre.getTotalAgiosHT(), trimestre.getTotalTVA(),
                            trimestre.getNbJoursCalcules(), trimestre.getNbJoursTotal()),
                    trimestre.getTotalAgiosTTC(),
                    banqueId,
                    LocalDate.now(ZONE_MAROC)
            );
            var agiosCree = agiosService.createAgios(req);
            trimestre.setAgiosDefinitifId(agiosCree.id());
        }

        trimestre.setStatut(StatutTrimestre.CLOTURE);
        trimestre.setDateCloture(OffsetDateTime.now(ZONE_MAROC));
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
                dto.banqueId(), dto.type(), dto.annee(),
                dto.trimestre(), dto.statut(), dto.search());
        return agiosTrimestrielRepository.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));
    }

    @Transactional(readOnly = true)
    public Page<AgiosJournalier> searchJournaliers(AgiosJournalierSearchDto dto, Pageable pageable) {
        LocalDate debut = debutDuTrimestrePourNumero(dto.trimestre(), dto.annee());
        LocalDate fin   = finDuTrimestrePourNumero(dto.trimestre(), dto.annee());
        Specification<AgiosJournalier> spec = AgiosJournalierSpecification.buildSpec(
                dto.banqueId(), dto.type(), debut, fin, dto.dateLimite());
        return agiosJournalierRepository.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));
    }

    @Transactional(readOnly = true)
    public AgiosDetailAggregateDto getAggregates(AgiosJournalierSearchDto dto) {
        LocalDate debut = debutDuTrimestrePourNumero(dto.trimestre(), dto.annee());
        LocalDate fin   = finDuTrimestrePourNumero(dto.trimestre(), dto.annee());
        Specification<AgiosJournalier> spec = AgiosJournalierSpecification.buildSpec(
                dto.banqueId(), dto.type(), debut, fin, dto.dateLimite());

        List<AgiosJournalier> jours = agiosJournalierRepository.findAll(spec);

        BigDecimal totalHT  = BigDecimal.ZERO;
        BigDecimal totalTVA = BigDecimal.ZERO;
        BigDecimal totalTTC = BigDecimal.ZERO;
        // Dates distinctes avec encours/découvert > 0 (évite de compter plusieurs marchés le même jour)
        Set<LocalDate> datesActives = new HashSet<>();

        for (AgiosJournalier j : jours) {
            totalHT  = totalHT.add(j.getAgiosHT()  != null ? j.getAgiosHT()  : BigDecimal.ZERO);
            totalTVA = totalTVA.add(j.getTva()      != null ? j.getTva()      : BigDecimal.ZERO);
            totalTTC = totalTTC.add(j.getAgiosTTC() != null ? j.getAgiosTTC() : BigDecimal.ZERO);
            if (j.getBaseCalcul() != null && j.getBaseCalcul().compareTo(BigDecimal.ZERO) > 0) {
                datesActives.add(j.getDateValeur());
            }
        }

        // Dénominateur = total jours calendaires du trimestre (pas le nombre de lignes)
        int nbJoursTotal = (int) ChronoUnit.DAYS.between(debut, fin) + 1;

        return new AgiosDetailAggregateDto(
                totalHT,
                totalTVA,
                totalTTC,
                datesActives.size(),
                nbJoursTotal
        );
    }

    public List<TypeAgios> getTypesActifs() {
        return calculateurs.stream().map(AgiosCalculateur::getType).collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════
    //  MÉTHODES PRIVÉES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Retourne le solde de départ du trimestre = {@code soldeDebut} du premier
     * rapprochement mensuel du trimestre (solde comptable avant toute transaction
     * du trimestre).
     */
    private BigDecimal getSoldeDebutTrimestre(Long banqueId, int trimNum, int annee) {
        int premierMois = (trimNum - 1) * 3 + 1;
        YearMonth moisDebut = YearMonth.of(annee, premierMois);
        return rapprochementRepository
                .findByBanqueIdAndDateRapprochement(banqueId, moisDebut)
                .map(Rapprochement::getSoldeDebut)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rapprochement introuvable pour la banque " + banqueId
                        + " — période : " + moisDebut));
    }

    /**
     * Charge toutes les transactions appartenant aux rapprochements
     * des 3 mois du trimestre. L'application par dateValeur reste
     * à la charge de la boucle journalière.
     */
    private List<Transaction> chargerTransactionsParRapprochement(Long banqueId, int trimNum, int annee) {
        List<Long> rapprochementIds = getMoisDuTrimestre(trimNum, annee).stream()
                .map(mois -> rapprochementRepository.findByBanqueIdAndDateRapprochement(banqueId, mois))
                .filter(Optional::isPresent)
                .map(opt -> opt.get().getId())
                .collect(Collectors.toList());

        if (rapprochementIds.isEmpty()) {
            log.warn("Aucun rapprochement trouvé pour banque {} T{}/{}", banqueId, trimNum, annee);
            return Collections.emptyList();
        }

        return transactionRepository.findAll(
                (root, query, cb) -> root.get("rapprochement").get("id").in(rapprochementIds));
    }

    /**
     * Retourne les 3 YearMonth correspondant au trimestre donné.
     * T1=[Jan,Fév,Mar] | T2=[Avr,Mai,Jun] | T3=[Jul,Aoû,Sep] | T4=[Oct,Nov,Déc]
     */
    private List<YearMonth> getMoisDuTrimestre(int trimNum, int annee) {
        int premierMois = (trimNum - 1) * 3 + 1;
        return List.of(
                YearMonth.of(annee, premierMois),
                YearMonth.of(annee, premierMois + 1),
                YearMonth.of(annee, premierMois + 2)
        );
    }

    /**
     * Crée ou met à jour un enregistrement journalier.
     *
     * @param solde   solde comptable (flux FC) ou encours préfinancement (flux multi-marché)
     * @param marche  marché rattaché (null pour FC, non-null pour préfinancement)
     */
    private void enregistrerJournalier(LocalDate jour, Banque banque, TypeAgios type,
                                       AgiosTrimestriel trimestre, BigDecimal solde,
                                       ResultatJournalier resultat, BigDecimal tva, BigDecimal ttc,
                                       Marche marche) {

        AgiosJournalier aj = (marche == null)
                ? agiosJournalierRepository
                        .findByDateValeurAndBanqueIdAndTypeAgiosAndMarcheIsNull(jour, banque.getId(), type)
                        .orElse(new AgiosJournalier())
                : agiosJournalierRepository
                        .findByDateValeurAndBanqueIdAndTypeAgiosAndMarcheId(jour, banque.getId(), type, marche.getId())
                        .orElse(new AgiosJournalier());

        aj.setDateValeur(jour);
        aj.setBanque(banque);
        aj.setTypeAgios(type);
        aj.setTrimestre(trimestre);
        aj.setMarche(marche);
        aj.setSoldeJournalier(solde);
        aj.setBaseCalcul(resultat.baseCalcul());
        aj.setTauxApplique(resultat.tauxApplique());
        aj.setAgiosHT(resultat.agiosHT());
        aj.setTva(tva);
        aj.setAgiosTTC(ttc);
        aj.setDateCalcul(OffsetDateTime.now(ZONE_MAROC));
        aj.setNbRecalculs(aj.getNbRecalculs() + 1);
        aj.setMetadata(resultat.metadata());

        agiosJournalierRepository.save(aj);
    }

    private AgiosTrimestriel obtenirOuCreerTrimestre(Banque banque, TypeAgios type, int trimNum, int annee) {
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

    // --- Utilitaires trimestre ---
    private int trimestreDeLaDate(LocalDate d) { return (d.getMonthValue() - 1) / 3 + 1; }
    private LocalDate debutDuTrimestrePourNumero(int t, int a) { return LocalDate.of(a, (t - 1) * 3 + 1, 1); }
    private LocalDate finDuTrimestrePourNumero(int t, int a)   { return LocalDate.of(a, t * 3, 1).plusMonths(1).minusDays(1); }
}
