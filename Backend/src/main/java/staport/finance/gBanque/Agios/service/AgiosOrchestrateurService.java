package staport.finance.gBanque.Agios.service;


import com.lowagie.text.Chunk;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
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
import staport.finance.gBanque.reports.service.StaportPdfReportService;
import staport.finance.gBanque.Agios.dtos.AgiosDetailAggregateDto;
import staport.finance.gBanque.Agios.dtos.AgiosJournalierResponseDto;
import staport.finance.gBanque.Agios.dtos.AgiosJournalierSearchDto;
import staport.finance.gBanque.Agios.dtos.MarcheRefDto;
import staport.finance.gBanque.Agios.dtos.AgiosRequestDto;
import staport.finance.gBanque.Agios.dtos.AgiosTrimestrielResponseDto;
import staport.finance.gBanque.Agios.dtos.AgiosTrimestrielSearchDto;
import staport.finance.gBanque.Agios.entities.AgiosJournalier;
import staport.finance.gBanque.Agios.entities.AgiosTrimestriel;
import staport.finance.gBanque.Agios.enums.StatutTrimestre;
import staport.finance.gBanque.Agios.enums.TypeAgios;
import staport.finance.gBanque.Agios.mapper.AgiosCalculatorMapper;
import staport.finance.gBanque.Agios.repository.AgiosJournalierRepository;
import staport.finance.gBanque.Agios.repository.AgiosTrimestrielRepository;
import staport.finance.gBanque.Agios.enums.PeriodeCalculAgios;
import staport.finance.gBanque.Agios.service.calculateurs.AgiosCalculateur;
import staport.finance.gBanque.Agios.service.calculateurs.AgiosCalculateur.ResultatJournalier;
import staport.finance.gBanque.Agios.service.calculateurs.AgiosCalculateurMultiMarche;
import staport.finance.gBanque.Agios.service.calculateurs.DroitConstateAgiosCalculateur;
import staport.finance.gBanque.Agios.service.calculateurs.FactoringAgiosCalculateur;
import staport.finance.gBanque.Agios.service.calculateurs.PrefinancementAgiosCalculateur;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.Agios.spec.AgiosJournalierSpecification;
import staport.finance.gBanque.Agios.spec.AgiosTrimestrielSpecification;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.services.IBanqueService;
import staport.finance.gBanque.audit.AuditOperation;
import staport.finance.gBanque.audit.AuditableAction;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.rapprochement.entities.Rapprochement;
import staport.finance.gBanque.rapprochement.repository.RapprochementRepository;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.repositories.TransactionRepository;

import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AgiosOrchestrateurService {

    private static final Logger log = LoggerFactory.getLogger(AgiosOrchestrateurService.class);
    private static final BigDecimal TAUX_TVA = new BigDecimal("10");
    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Color MED_BLUE = Color.decode("#2563eb");
    private static final Color LIGHT_BLUE = Color.decode("#dbeafe");
    private static final Color ROW_ALT = Color.decode("#f8fafc");
    private static final Color BORDER_GRAY = Color.decode("#e5e7eb");

    /**
     * Fuseau horaire du Maroc : UTC+1 permanent (Africa/Casablanca).
     * Utilisé pour convertir les dateValeur (OffsetDateTime) en date locale
     * et pour l'horloge du scheduler.
     */
    private static final ZoneId ZONE_MAROC = ZoneId.of("Africa/Casablanca");

    private final List<AgiosCalculateur> calculateurs;
    private final TransactionRepository transactionRepository;
    private final IBanqueService banqueService;
    private final AgiosJournalierRepository agiosJournalierRepository;
    private final AgiosTrimestrielRepository agiosTrimestrielRepository;
    private final AgiosService agiosService;
    private final RapprochementRepository rapprochementRepository;
    private final AgiosCalculatorMapper agiosCalculatorMapper;
    private final StaportPdfReportService staportPdfReportService;

    public AgiosOrchestrateurService(List<AgiosCalculateur> calculateurs,
                                     TransactionRepository transactionRepository,
                                     IBanqueService banqueService,
                                     AgiosJournalierRepository agiosJournalierRepository,
                                     AgiosTrimestrielRepository agiosTrimestrielRepository,
                                     AgiosService agiosService,
                                     RapprochementRepository rapprochementRepository,
                                     AgiosCalculatorMapper agiosCalculatorMapper,
                                     StaportPdfReportService staportPdfReportService) {
        this.calculateurs = calculateurs;
        this.transactionRepository = transactionRepository;
        this.banqueService = banqueService;
        this.agiosJournalierRepository = agiosJournalierRepository;
        this.agiosTrimestrielRepository = agiosTrimestrielRepository;
        this.agiosService = agiosService;
        this.rapprochementRepository = rapprochementRepository;
        this.agiosCalculatorMapper = agiosCalculatorMapper;
        this.staportPdfReportService = staportPdfReportService;

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
        int anneeActuelle = aujourdhui.getYear();

        log.info("══════ Calcul quotidien des agios — {} — {} type(s) actif(s) ══════",
                aujourdhui, calculateurs.size());

        List<Banque> banques = banqueService.getAllBanqueEntities();

        for (AgiosCalculateur calculateur : calculateurs) {
            log.info("── Type : {} ──", calculateur.getType().getLibelle());

            // Période courante selon la périodicité du type (mois 1-12 ou trimestre 1-4)
            int periodeActuelleCalc = periodeActuelle(calculateur.getType(), aujourdhui);

            int succes = 0, nonEligibles = 0, erreurs = 0;

            for (Banque banque : banques) {

                // Vider le cache de taux avant chaque banque pour éviter des données périmées
                if (calculateur instanceof PrefinancementAgiosCalculateur prefiCalc) {
                    prefiCalc.viderCache();
                }
                if (calculateur instanceof DroitConstateAgiosCalculateur dcCalc) {
                    dcCalc.viderCache();
                }
                if (calculateur instanceof FactoringAgiosCalculateur factoringCalc) {
                    factoringCalc.viderCache();
                }

                try {
                    if (!calculateur.estEligible(banque)) {
                        nonEligibles++;
                        continue;
                    }

                    // Toutes les périodes OUVERTES de cette banque (courante + passées non clôturées).
                    // Une période reste recalculée tant qu'elle n'est pas clôturée, ce qui permet
                    // de répercuter toute modification de transaction sur les agios correspondants.
                    List<AgiosTrimestriel> ouverts = agiosTrimestrielRepository
                            .findByBanqueIdAndTypeAgiosAndStatut(
                                    banque.getId(), calculateur.getType(), StatutTrimestre.OUVERT);

                    // S'assurer que la période courante est toujours traitée,
                    // même si elle n'a pas encore été créée (obtenirOuCreerTrimestre la crée).
                    boolean courantPresent = ouverts.stream()
                            .anyMatch(t -> t.getTrimestre() == periodeActuelleCalc && t.getAnnee() == anneeActuelle);

                    // Chaque période est isolée : un échec (ex. rapprochement manquant pour le
                    // mois courant) ne doit jamais empêcher le recalcul des autres périodes
                    // ouvertes de la même banque — ni pour le scheduler nocturne, ni pour le
                    // bouton "Recalculer tout" de l'UI qui réutilise ce flux.
                    int erreursPeriodes = 0;

                    if (!courantPresent) {
                        erreursPeriodes += recalculerPeriodeSansInterrompre(
                                banque, calculateur, periodeActuelleCalc, anneeActuelle) ? 0 : 1;
                    }

                    for (AgiosTrimestriel t : ouverts) {
                        erreursPeriodes += recalculerPeriodeSansInterrompre(
                                banque, calculateur, t.getTrimestre(), t.getAnnee()) ? 0 : 1;
                    }

                    if (erreursPeriodes == 0) {
                        succes++;
                    } else {
                        erreurs++;
                    }

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

    /**
     * Recalcule une période en isolant tout échec : l'erreur est loguée avec la
     * période exacte en cause, et le traitement des autres périodes continue.
     *
     * @return {@code true} si le recalcul a abouti, {@code false} en cas d'erreur
     */
    private boolean recalculerPeriodeSansInterrompre(Banque banque, AgiosCalculateur calculateur,
                                                     int num, int annee) {
        try {
            recalculerTrimestre(banque, calculateur, num, annee);
            return true;
        } catch (Exception e) {
            log.error("{} — Banque {} (ID:{}) période {}/{} : {}",
                    calculateur.getType(), banque.getNom(), banque.getId(), num, annee, e.getMessage(), e);
            return false;
        }
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
        int anneeActuelle    = aujourdhui.getYear();

        // Validation globale : plage maximale 1–12 (couvre TRIMESTRIEL 1–4 et MENSUEL 1–12)
        if (trimNum < 1 || trimNum > 12) {
            throw new IllegalArgumentException(
                    "Numéro de période invalide : " + trimNum + " (attendu : 1–12)");
        }
        if (annee > anneeActuelle) {
            throw new IllegalArgumentException(
                    "Impossible de calculer une année future : " + annee);
        }

        Banque banque = banqueService.findOrThrow(banqueId);

        log.info("══════ Recalcul manuel période {}/{} — Banque {} ══════", trimNum, annee, banque.getNom());

        for (AgiosCalculateur calculateur : calculateurs) {
            PeriodeCalculAgios periodeType = calculateur.getType().getPeriodeCalcul();
            int maxPeriode = (periodeType == PeriodeCalculAgios.TRIMESTRIEL) ? 4 : 12;

            // Skip si trimNum hors plage pour ce type (ex : période 7 → ignoré par TRIMESTRIEL)
            if (trimNum > maxPeriode) {
                log.debug("{} ignoré : période {} hors plage (max={})", calculateur.getType(), trimNum, maxPeriode);
                continue;
            }

            // Skip si période dans le futur pour ce type
            int periodeCourante = periodeActuelle(calculateur.getType(), aujourdhui);
            if (annee == anneeActuelle && trimNum > periodeCourante) {
                log.debug("{} ignoré : période {}/{} dans le futur (courante={}/{})",
                        calculateur.getType(), trimNum, annee, periodeCourante, anneeActuelle);
                continue;
            }

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
        PeriodeCalculAgios periodeType = type.getPeriodeCalcul();
        LocalDate debutTrim = debutDePeriode(trimNum, annee, periodeType);
        LocalDate finTrim   = finDePeriode(trimNum, annee, periodeType);

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
            //  FLUX MULTI-MARCHÉ (Préfinancement, Droits Constatés)
            //  L'encours est reconstitué depuis les opérations PAYE,
            //  indépendamment du solde comptable du rapprochement.
            // ════════════════════════════════════════════════════════════

            // Stats marchés sur tout le trimestre
            Set<Long>         marchesVus            = new LinkedHashSet<>(); // ont une ligne → présents dans résultats
            Set<Long>         marchesActifs         = new LinkedHashSet<>(); // encours > 0 au moins un jour
            Map<Long, String> nomParMarcheId        = new LinkedHashMap<>();

            for (long i = 0; i < nbJours; i++) {
                LocalDate jour = debutTrim.plusDays(i);

                List<AgiosCalculateurMultiMarche.ResultatParMarche> resultats =
                        multiCalculateur.calculerJourParMarche(banque, jour);

                // Un jour est "actif" si au moins un marché a un encours > 0 ce jour-là
                boolean jourAvecEncours = false;
                for (AgiosCalculateurMultiMarche.ResultatParMarche r : resultats) {
                    ResultatJournalier res = r.resultat();

                    marchesVus.add(r.marche().getId());
                    nomParMarcheId.putIfAbsent(r.marche().getId(), r.marche().getNom());

                    BigDecimal tvaJour = res.agiosHT()
                            .multiply(TAUX_TVA)
                            .divide(BigDecimal.valueOf(100), SCALE, ROUNDING);
                    BigDecimal ttcJour = res.agiosHT().add(tvaJour);

                    enregistrerJournalier(jour, banque, type, trimestre,
                            res.baseCalcul(), res, tvaJour, ttcJour, r.marche());

                    totalHT  = totalHT.add(res.agiosHT());
                    totalTVA = totalTVA.add(tvaJour);
                    totalTTC = totalTTC.add(ttcJour);
                    if (res.baseCalcul().compareTo(BigDecimal.ZERO) > 0) {
                        jourAvecEncours = true;
                        marchesActifs.add(r.marche().getId());
                    }
                }
                if (jourAvecEncours) nbJoursCalcules++;
            }

            // Marchés avec ligne mais encours toujours nul sur tout le trimestre
            Set<Long> marchesEncoursToujoursNul = new LinkedHashSet<>(marchesVus);
            marchesEncoursToujoursNul.removeAll(marchesActifs);

            // Persistance
            trimestre.setTotalAgiosHT(totalHT);
            trimestre.setTotalTVA(totalTVA);
            trimestre.setTotalAgiosTTC(totalTTC);
            trimestre.setNbJoursCalcules(nbJoursCalcules);
            trimestre.setNbJoursTotal((int) ChronoUnit.DAYS.between(debutTrim, finTrim) + 1);
            trimestre.setDateDernierCalcul(OffsetDateTime.now(ZONE_MAROC));
            agiosTrimestrielRepository.save(trimestre);

            log.info("{} — {} T{}/{} : HT={} TVA={} TTC={} | {}/{} jours avec encours" +
                            " | marchés actifs {}/{} [{}]{}",
                    type, banque.getNom(), trimNum, annee,
                    totalHT, totalTVA, totalTTC,
                    nbJoursCalcules, nbJours,
                    marchesActifs.size(), marchesVus.size(),
                    marchesActifs.stream()
                            .map(id -> nomParMarcheId.getOrDefault(id, "#" + id))
                            .collect(Collectors.joining(", ")),
                    marchesEncoursToujoursNul.isEmpty() ? "" :
                            " | encours nul tout le trimestre : " +
                            marchesEncoursToujoursNul.stream()
                                    .map(id -> nomParMarcheId.getOrDefault(id, "#" + id))
                                    .collect(Collectors.joining(", ")));

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
            BigDecimal soldeCourant    = soldeInitial;
            BigDecimal totalDecouvert  = BigDecimal.ZERO; // cumul découvert sur le trimestre
            BigDecimal tauxFC          = BigDecimal.ZERO; // taux retenu (constant pour une banque)

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
                if (resultat.baseCalcul().compareTo(BigDecimal.ZERO) > 0) {
                    nbJoursCalcules++;
                    totalDecouvert = totalDecouvert.add(resultat.baseCalcul());
                    if (tauxFC.compareTo(BigDecimal.ZERO) == 0) tauxFC = resultat.tauxApplique();
                }
            }

            // Persistance
            trimestre.setTotalAgiosHT(totalHT);
            trimestre.setTotalTVA(totalTVA);
            trimestre.setTotalAgiosTTC(totalTTC);
            trimestre.setNbJoursCalcules(nbJoursCalcules);
            trimestre.setNbJoursTotal((int) ChronoUnit.DAYS.between(debutTrim, finTrim) + 1);
            trimestre.setDateDernierCalcul(OffsetDateTime.now(ZONE_MAROC));
            agiosTrimestrielRepository.save(trimestre);

            BigDecimal decouvertMoyen = nbJoursCalcules > 0
                    ? totalDecouvert.divide(BigDecimal.valueOf(nbJoursCalcules), SCALE, ROUNDING)
                    : BigDecimal.ZERO;

            log.info("FC — {} T{}/{} : HT={} TVA={} TTC={}" +
                            " | {}/{} jours découvert | taux={}% | cumul découvert={} | moy/jour={}",
                    banque.getNom(), trimNum, annee,
                    totalHT, totalTVA, totalTTC,
                    nbJoursCalcules, nbJours,
                    tauxFC, totalDecouvert.setScale(SCALE, ROUNDING), decouvertMoyen);
            return; // log déjà émis, on sort
        }

    }

    // ═══════════════════════════════════════════════════════════════
    //  CLÔTURE TRIMESTRIELLE
    // ═══════════════════════════════════════════════════════════════

    @AuditableAction(operation = AuditOperation.UPDATE, entity = AgiosTrimestriel.class)
    @Transactional
    public AgiosTrimestrielResponseDto cloturerTrimestre(Long banqueId, TypeAgios type, int trimNum, int annee) {
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
        // Mappé ici, dans la transaction — la relation banque (LAZY) ne doit pas être touchée
        // après le retour de cette méthode (spring.jpa.open-in-view: false, voir jpa.md).
        return agiosCalculatorMapper.toTrimDto(trimestre);
    }

    // ═══════════════════════════════════════════════════════════════
    //  CONSULTATION
    // ═══════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public Page<AgiosTrimestrielResponseDto> searchTrimestres(AgiosTrimestrielSearchDto dto, Pageable pageable) {
        Specification<AgiosTrimestriel> spec = AgiosTrimestrielSpecification.searchByCriteria(
                dto.banqueId(), dto.type(), dto.annee(),
                dto.trimestre(), dto.statut(), dto.search());
        Page<AgiosTrimestriel> page = agiosTrimestrielRepository.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));
        // Mappé ici, dans la transaction — banque (LAZY) ne doit pas être touchée hors session
        // (spring.jpa.open-in-view: false, voir jpa.md) ; avant, le mapping se faisait dans le
        // controller, masqué jusqu'ici par open-in-view=true.
        return page.map(agiosCalculatorMapper::toTrimDto);
    }

    @Transactional(readOnly = true)
    public Page<AgiosJournalierResponseDto> searchJournaliers(AgiosJournalierSearchDto dto, Pageable pageable) {
        PeriodeCalculAgios periodeDto = dto.type() != null ? dto.type().getPeriodeCalcul() : PeriodeCalculAgios.TRIMESTRIEL;
        LocalDate debut = debutDePeriode(dto.trimestre(), dto.annee(), periodeDto);
        LocalDate fin   = finDePeriode(dto.trimestre(), dto.annee(), periodeDto);
        Specification<AgiosJournalier> spec = AgiosJournalierSpecification.searchByCriteria(
                dto.banqueId(), dto.type(), debut, fin, dto.dateLimite(), dto.marcheId());
        Page<AgiosJournalier> page = agiosJournalierRepository.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));
        return page.map(agiosCalculatorMapper::toJourDto);
    }

    @Transactional(readOnly = true)
    public AgiosDetailAggregateDto getAggregates(AgiosJournalierSearchDto dto) {
        PeriodeCalculAgios periodeDto = dto.type() != null ? dto.type().getPeriodeCalcul() : PeriodeCalculAgios.TRIMESTRIEL;
        LocalDate debut = debutDePeriode(dto.trimestre(), dto.annee(), periodeDto);
        LocalDate fin   = finDePeriode(dto.trimestre(), dto.annee(), periodeDto);
        Specification<AgiosJournalier> spec = AgiosJournalierSpecification.searchByCriteria(
                dto.banqueId(), dto.type(), debut, fin, dto.dateLimite(), dto.marcheId());

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

    /**
     * Marchés disponibles pour le filtre de l'écran de détail : uniquement ceux effectivement
     * présents dans le détail journalier de la banque/type/période consultée (jamais le
     * référentiel marché complet). Toujours vide pour les types sans marché (ex. Facilité de Caisse).
     */
    @Transactional(readOnly = true)
    public List<MarcheRefDto> getMarchesDisponibles(AgiosJournalierSearchDto dto) {
        PeriodeCalculAgios periodeDto = dto.type() != null ? dto.type().getPeriodeCalcul() : PeriodeCalculAgios.TRIMESTRIEL;
        LocalDate debut = debutDePeriode(dto.trimestre(), dto.annee(), periodeDto);
        LocalDate fin   = finDePeriode(dto.trimestre(), dto.annee(), periodeDto);
        return agiosJournalierRepository
                .findDistinctMarchesByCriteria(dto.banqueId(), dto.type(), debut, fin)
                .stream()
                .map(m -> new MarcheRefDto(m.getId(), m.getNumero(), m.getNom()))
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════
    //  EXPORT PDF
    // ═══════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public byte[] exportTrimestresPdf(AgiosTrimestrielSearchDto dto) {
        log.info("Export PDF des trimestres d'agios - Critères: {}", dto);

        Specification<AgiosTrimestriel> spec = AgiosTrimestrielSpecification.searchByCriteria(
                dto.banqueId(), dto.type(), dto.annee(), dto.trimestre(), dto.statut(), dto.search());

        List<AgiosTrimestrielResponseDto> trimestres = agiosTrimestrielRepository
                .findAll(spec, Sort.by(Sort.Direction.DESC, "annee", "trimestre"))
                .stream()
                .map(agiosCalculatorMapper::toTrimDto)
                .toList();

        List<String> filterLines = buildTrimestreFilterSummary(dto);

        return generateTrimestresPdf(trimestres, filterLines);
    }

    @Transactional(readOnly = true)
    public byte[] exportJournalierPdf(AgiosJournalierSearchDto dto) {
        log.info("Export PDF du détail journalier d'agios - Critères: {}", dto);

        PeriodeCalculAgios periodeDto = dto.type() != null ? dto.type().getPeriodeCalcul() : PeriodeCalculAgios.TRIMESTRIEL;
        LocalDate debut = debutDePeriode(dto.trimestre(), dto.annee(), periodeDto);
        LocalDate fin   = finDePeriode(dto.trimestre(), dto.annee(), periodeDto);
        Specification<AgiosJournalier> spec = AgiosJournalierSpecification.searchByCriteria(
                dto.banqueId(), dto.type(), debut, fin, dto.dateLimite(), dto.marcheId());

        List<AgiosJournalierResponseDto> jours = agiosJournalierRepository
                .findAll(spec, Sort.by(Sort.Direction.ASC, "dateValeur"))
                .stream()
                .map(agiosCalculatorMapper::toJourDto)
                .toList();

        AgiosDetailAggregateDto aggregates = getAggregates(dto);
        List<String> filterLines = buildJournalierFilterSummary(dto);
        boolean multiMarche = jours.stream().anyMatch(j -> j.marcheId() != null);

        return generateJournalierPdf(jours, aggregates, filterLines, multiMarche);
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
                    t.setDateDebut(debutDePeriode(trimNum, annee, type.getPeriodeCalcul()));
                    t.setDateFin(finDePeriode(trimNum, annee, type.getPeriodeCalcul()));
                    t.setStatut(StatutTrimestre.OUVERT);
                    return agiosTrimestrielRepository.save(t);
                });
    }

    // --- Utilitaires trimestre / période ---

    private int trimestreDeLaDate(LocalDate d) { return (d.getMonthValue() - 1) / 3 + 1; }
    private LocalDate debutDuTrimestrePourNumero(int t, int a) { return LocalDate.of(a, (t - 1) * 3 + 1, 1); }
    private LocalDate finDuTrimestrePourNumero(int t, int a)   { return LocalDate.of(a, t * 3, 1).plusMonths(1).minusDays(1); }

    /**
     * Retourne le numéro de période courante selon la périodicité du type :
     * – TRIMESTRIEL → numéro de trimestre (1–4)
     * – MENSUEL     → numéro de mois (1–12)
     */
    private int periodeActuelle(TypeAgios type, LocalDate date) {
        return type.getPeriodeCalcul() == PeriodeCalculAgios.MENSUEL
                ? date.getMonthValue()
                : trimestreDeLaDate(date);
    }

    /**
     * Date de début de période selon la périodicité :
     * – TRIMESTRIEL → 1er jour du trimestre
     * – MENSUEL     → 1er jour du mois
     */
    private LocalDate debutDePeriode(int num, int annee, PeriodeCalculAgios periode) {
        return periode == PeriodeCalculAgios.MENSUEL
                ? LocalDate.of(annee, num, 1)
                : debutDuTrimestrePourNumero(num, annee);
    }

    /**
     * Date de fin de période selon la périodicité :
     * – TRIMESTRIEL → dernier jour du trimestre
     * – MENSUEL     → dernier jour du mois
     */
    private LocalDate finDePeriode(int num, int annee, PeriodeCalculAgios periode) {
        return periode == PeriodeCalculAgios.MENSUEL
                ? YearMonth.of(annee, num).atEndOfMonth()
                : finDuTrimestrePourNumero(num, annee);
    }

    // ── Génération PDF — trimestres ─────────────────────────────────────────

    private byte[] generateTrimestresPdf(List<AgiosTrimestrielResponseDto> trimestres, List<String> filterLines) {
        BigDecimal totalHT  = trimestres.stream().map(AgiosTrimestrielResponseDto::totalAgiosHT)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalTVA = trimestres.stream().map(AgiosTrimestrielResponseDto::totalTVA)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalTTC = trimestres.stream().map(AgiosTrimestrielResponseDto::totalAgiosTTC)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);

        try {
            return staportPdfReportService.generateReport("Agios — Calcul automatique", (doc, writer) -> {
                try {
                    Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
                    Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.decode("#374151"));
                    Font hdrFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
                    Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
                    Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.decode("#1e3a8a"));

                    doc.add(Chunk.NEWLINE);
                    doc.add(Chunk.NEWLINE);

                    Paragraph titleParagraph = new Paragraph("Agios — Calcul automatique", titleFont);
                    titleParagraph.setAlignment(Element.ALIGN_CENTER);
                    doc.add(titleParagraph);

                    String exportInfo = "Exporté le " + LocalDate.now().format(DATE_FMT)
                            + "  •  " + trimestres.size() + " période(s)";
                    if (!filterLines.isEmpty()) {
                        exportInfo += "  •  Filtres : " + String.join("  |  ", filterLines);
                    }
                    Paragraph sub = new Paragraph(exportInfo, subFont);
                    sub.setAlignment(Element.ALIGN_CENTER);
                    sub.setSpacingAfter(8f);
                    doc.add(sub);

                    float[] colWidths = {18f, 16f, 10f, 14f, 12f, 14f, 8f, 8f};
                    PdfPTable table = new PdfPTable(colWidths);
                    table.setWidthPercentage(100f);
                    table.setSpacingBefore(8f);
                    table.setHeaderRows(1);

                    for (String h : new String[]{"Banque", "Type", "Période", "Total HT (MAD)", "TVA (MAD)",
                            "Total TTC (MAD)", "Jours", "Statut"}) {
                        PdfPCell c = new PdfPCell(new Phrase(h, hdrFont));
                        c.setBackgroundColor(MED_BLUE);
                        c.setPadding(5f);
                        c.setHorizontalAlignment(Element.ALIGN_CENTER);
                        c.setBorderColor(Color.WHITE);
                        c.setBorderWidth(0.5f);
                        table.addCell(c);
                    }

                    for (int i = 0; i < trimestres.size(); i++) {
                        AgiosTrimestrielResponseDto t = trimestres.get(i);
                        Color rowBg = i % 2 == 0 ? Color.WHITE : ROW_ALT;

                        addOrchPdfCell(table, t.banqueNom(), cellFont, rowBg, Element.ALIGN_LEFT);
                        addOrchPdfCell(table, t.typeAgios() != null ? t.typeAgios().getLibelle() : "—", cellFont, rowBg, Element.ALIGN_LEFT);
                        addOrchPdfCell(table, "T" + t.trimestre() + "/" + t.annee(), cellFont, rowBg, Element.ALIGN_CENTER);
                        addOrchPdfCell(table, formatMontantOrch(t.totalAgiosHT()), cellFont, rowBg, Element.ALIGN_RIGHT);
                        addOrchPdfCell(table, formatMontantOrch(t.totalTVA()), cellFont, rowBg, Element.ALIGN_RIGHT);
                        addOrchPdfCell(table, formatMontantOrch(t.totalAgiosTTC()), cellFont, rowBg, Element.ALIGN_RIGHT);
                        addOrchPdfCell(table, t.nbJoursCalcules() + "/" + t.nbJoursTotal(), cellFont, rowBg, Element.ALIGN_CENTER);

                        PdfPCell statusCell = new PdfPCell(new Phrase(
                                t.statut() != null ? t.statut().name() : "—",
                                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE)));
                        statusCell.setBackgroundColor(t.statut() == StatutTrimestre.CLOTURE ? Color.decode("#16a34a") : Color.decode("#ea580c"));
                        statusCell.setPadding(4f);
                        statusCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                        statusCell.setBorderColor(BORDER_GRAY);
                        statusCell.setBorderWidth(0.5f);
                        table.addCell(statusCell);
                    }

                    PdfPCell totalLabel = new PdfPCell(new Phrase("TOTAL — " + trimestres.size() + " période(s)", totalFont));
                    totalLabel.setColspan(3);
                    totalLabel.setBackgroundColor(LIGHT_BLUE);
                    totalLabel.setPadding(5f);
                    totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    totalLabel.setBorderColor(MED_BLUE);
                    table.addCell(totalLabel);

                    for (BigDecimal val : new BigDecimal[]{totalHT, totalTVA, totalTTC}) {
                        PdfPCell valCell = new PdfPCell(new Phrase(formatMontantOrch(val), totalFont));
                        valCell.setBackgroundColor(LIGHT_BLUE);
                        valCell.setPadding(5f);
                        valCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        valCell.setBorderColor(MED_BLUE);
                        table.addCell(valCell);
                    }

                    for (int j = 0; j < 2; j++) {
                        PdfPCell empty = new PdfPCell(new Phrase(""));
                        empty.setBackgroundColor(LIGHT_BLUE);
                        empty.setBorderColor(MED_BLUE);
                        table.addCell(empty);
                    }

                    doc.add(table);
                } catch (DocumentException e) {
                    throw new RuntimeException("Erreur lors de la génération du contenu du PDF des trimestres d'agios.", e);
                }
            });
        } catch (DocumentException e) {
            throw new RuntimeException("Erreur lors de la génération du PDF des trimestres d'agios.", e);
        }
    }

    private List<String> buildTrimestreFilterSummary(AgiosTrimestrielSearchDto dto) {
        List<String> lines = new ArrayList<>();
        if (dto == null) return lines;

        if (dto.banqueId() != null) {
            try {
                lines.add("Banque : " + banqueService.findOrThrow(dto.banqueId()).getNom());
            } catch (ResourceNotFoundException e) {
                log.debug("Banque id={} introuvable pour le résumé de filtre export — ignorée", dto.banqueId());
            }
        }
        if (dto.type() != null) {
            lines.add("Type : " + dto.type().getLibelle());
        }
        if (dto.annee() != null) {
            lines.add("Année : " + dto.annee());
        }
        if (dto.trimestre() != null) {
            lines.add("Trimestre : T" + dto.trimestre());
        }
        if (dto.statut() != null) {
            lines.add("Statut : " + dto.statut().name());
        }
        if (dto.search() != null && !dto.search().isBlank()) {
            lines.add("Recherche : " + dto.search());
        }
        return lines;
    }

    // ── Génération PDF — détail journalier ──────────────────────────────────

    private byte[] generateJournalierPdf(List<AgiosJournalierResponseDto> jours, AgiosDetailAggregateDto aggregates,
                                         List<String> filterLines, boolean multiMarche) {
        try {
            return staportPdfReportService.generateReport("Agios — Détail journalier", PageSize.A4.rotate(), (doc, writer) -> {
                try {
                    Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
                    Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.decode("#374151"));
                    Font hdrFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
                    Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
                    Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.decode("#1e3a8a"));

                    doc.add(Chunk.NEWLINE);
                    doc.add(Chunk.NEWLINE);

                    Paragraph titleParagraph = new Paragraph("Agios — Détail journalier", titleFont);
                    titleParagraph.setAlignment(Element.ALIGN_CENTER);
                    doc.add(titleParagraph);

                    String exportInfo = "Exporté le " + LocalDate.now().format(DATE_FMT)
                            + "  •  " + jours.size() + " jour(s)";
                    if (!filterLines.isEmpty()) {
                        exportInfo += "  •  Filtres : " + String.join("  |  ", filterLines);
                    }
                    Paragraph sub = new Paragraph(exportInfo, subFont);
                    sub.setAlignment(Element.ALIGN_CENTER);
                    sub.setSpacingAfter(8f);
                    doc.add(sub);

                    float[] colWidths = multiMarche
                            ? new float[]{10f, 14f, 14f, 14f, 8f, 13f, 13f, 14f}
                            : new float[]{12f, 16f, 16f, 10f, 15f, 15f, 16f};
                    PdfPTable table = new PdfPTable(colWidths);
                    table.setWidthPercentage(100f);
                    table.setSpacingBefore(8f);
                    table.setHeaderRows(1);

                    List<String> headers = new ArrayList<>(List.of("Date"));
                    if (multiMarche) headers.add("Marché");
                    headers.addAll(List.of("Solde / Encours (MAD)", "Base calcul (MAD)", "Taux %",
                            "Agios HT (MAD)", "TVA (MAD)", "Agios TTC (MAD)"));

                    for (String h : headers) {
                        PdfPCell c = new PdfPCell(new Phrase(h, hdrFont));
                        c.setBackgroundColor(MED_BLUE);
                        c.setPadding(5f);
                        c.setHorizontalAlignment(Element.ALIGN_CENTER);
                        c.setBorderColor(Color.WHITE);
                        c.setBorderWidth(0.5f);
                        table.addCell(c);
                    }

                    for (int i = 0; i < jours.size(); i++) {
                        AgiosJournalierResponseDto j = jours.get(i);
                        Color rowBg = i % 2 == 0 ? Color.WHITE : ROW_ALT;

                        addOrchPdfCell(table, j.dateValeur() != null ? j.dateValeur().format(DATE_FMT) : "—", cellFont, rowBg, Element.ALIGN_CENTER);
                        if (multiMarche) {
                            addOrchPdfCell(table, j.marcheNumero() != null ? j.marcheNumero() : "—", cellFont, rowBg, Element.ALIGN_LEFT);
                        }
                        addOrchPdfCell(table, formatMontantOrch(j.soldeJournalier()), cellFont, rowBg, Element.ALIGN_RIGHT);
                        addOrchPdfCell(table, formatMontantOrch(j.baseCalcul()), cellFont, rowBg, Element.ALIGN_RIGHT);
                        addOrchPdfCell(table, j.tauxApplique() != null ? j.tauxApplique().toPlainString() : "—", cellFont, rowBg, Element.ALIGN_RIGHT);
                        addOrchPdfCell(table, formatMontantOrch(j.agiosHT()), cellFont, rowBg, Element.ALIGN_RIGHT);
                        addOrchPdfCell(table, formatMontantOrch(j.tva()), cellFont, rowBg, Element.ALIGN_RIGHT);
                        addOrchPdfCell(table, formatMontantOrch(j.agiosTTC()), cellFont, rowBg, Element.ALIGN_RIGHT);
                    }

                    int labelColspan = multiMarche ? 5 : 4;
                    String totalLabelText = "TOTAL — " + aggregates.nbJoursDecouvert() + "/" + aggregates.nbJoursTotal() + " jours avec encours";

                    PdfPCell totalLabel = new PdfPCell(new Phrase(totalLabelText, totalFont));
                    totalLabel.setColspan(labelColspan);
                    totalLabel.setBackgroundColor(LIGHT_BLUE);
                    totalLabel.setPadding(5f);
                    totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    totalLabel.setBorderColor(MED_BLUE);
                    table.addCell(totalLabel);

                    for (BigDecimal val : new BigDecimal[]{aggregates.totalAgiosHT(), aggregates.totalTVA(), aggregates.totalAgiosTTC()}) {
                        PdfPCell valCell = new PdfPCell(new Phrase(formatMontantOrch(val), totalFont));
                        valCell.setBackgroundColor(LIGHT_BLUE);
                        valCell.setPadding(5f);
                        valCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        valCell.setBorderColor(MED_BLUE);
                        table.addCell(valCell);
                    }

                    doc.add(table);
                } catch (DocumentException e) {
                    throw new RuntimeException("Erreur lors de la génération du contenu du PDF du détail journalier des agios.", e);
                }
            });
        } catch (DocumentException e) {
            throw new RuntimeException("Erreur lors de la génération du PDF du détail journalier des agios.", e);
        }
    }

    private List<String> buildJournalierFilterSummary(AgiosJournalierSearchDto dto) {
        List<String> lines = new ArrayList<>();
        if (dto == null) return lines;

        if (dto.banqueId() != null) {
            try {
                lines.add("Banque : " + banqueService.findOrThrow(dto.banqueId()).getNom());
            } catch (ResourceNotFoundException e) {
                log.debug("Banque id={} introuvable pour le résumé de filtre export — ignorée", dto.banqueId());
            }
        }
        if (dto.type() != null) {
            lines.add("Type : " + dto.type().getLibelle());
        }
        if (dto.annee() != null) {
            lines.add("Année : " + dto.annee());
        }
        if (dto.trimestre() != null) {
            lines.add("Période : " + dto.trimestre());
        }
        if (dto.dateLimite() != null) {
            lines.add("Date limite : " + dto.dateLimite().format(DATE_FMT));
        }
        if (dto.marcheId() != null) {
            lines.add("Marché ID : " + dto.marcheId());
        }
        return lines;
    }

    private void addOrchPdfCell(PdfPTable table, String text, Font font, Color bg, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "—", font));
        cell.setBackgroundColor(bg);
        cell.setPadding(4f);
        cell.setHorizontalAlignment(align);
        cell.setBorderColor(BORDER_GRAY);
        cell.setBorderWidth(0.5f);
        table.addCell(cell);
    }

    private String formatMontantOrch(BigDecimal montant) {
        if (montant == null) return "—";
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRANCE);
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
        DecimalFormat formatter = new DecimalFormat("#,##0.00", symbols);
        return formatter.format(montant);
    }
}
