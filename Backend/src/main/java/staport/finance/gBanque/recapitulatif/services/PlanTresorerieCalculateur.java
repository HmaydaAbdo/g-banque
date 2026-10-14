package staport.finance.gBanque.recapitulatif.services;

import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.recapitulatif.dtos.response.ColonnePlanDto;
import staport.finance.gBanque.recapitulatif.dtos.response.LignePlanDto;
import staport.finance.gBanque.recapitulatif.dtos.response.PlanBanqueDto;
import staport.finance.gBanque.recapitulatif.dtos.response.PlanTresorerieDto;
import staport.finance.gBanque.recapitulatif.dtos.response.PointBasDto;
import staport.finance.gBanque.recapitulatif.dtos.response.SoldeJourDto;
import staport.finance.gBanque.recapitulatif.enums.PeriodeRecap;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Calcul du plan de trésorerie prévisionnel — fonction pure, sans accès à la base ni à l'horloge :
 * la date du jour, les soldes des banques et les flux prévus lui sont passés, ce qui la rend
 * testable avec des dates fixes.
 *
 * <p>Règles :</p>
 * <ul>
 *   <li>Point de départ : le solde actuel de chaque banque (le même que sur les cartes de
 *       l'accueil).</li>
 *   <li>Flux : les encours non encore validés, à leur date de référence. Ceux dont la date est
 *       aujourd'hui ou passée (en retard de validation) sont regroupés dans la colonne « À ce
 *       jour » et comptent dès aujourd'hui.</li>
 *   <li>Colonnes suivantes : semaines (lundi-dimanche) jusqu'à la fin du mois, ou mois civils
 *       jusqu'à la fin du trimestre.</li>
 *   <li>Lignes : les catégories du Récapitulatif général détaillé ({@link CategoriesTresorerie}) ;
 *       une source hors catégorie va sur la ligne « Autres » pour que les totaux restent égaux à
 *       ceux des cartes de l'accueil.</li>
 *   <li>Vue consolidée : somme des banques ; son point bas est calculé sur la courbe consolidée
 *       (et non comme la somme des points bas de chaque banque, qui ne tombent pas le même
 *       jour).</li>
 * </ul>
 */
final class PlanTresorerieCalculateur {

    static final String CODE_ECHU = "ECHU";
    static final String AUTRES = "Autres";
    static final String LIBELLE_CONSOLIDE = "Toutes les banques";

    private static final DateTimeFormatter JOUR_MOIS = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter MOIS_ANNEE = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);

    /** Solde de départ d'une banque. */
    record SoldeBanque(Long banqueId, String nom, BigDecimal soldeActuel, BigDecimal plafondFC) {
    }

    /** Montant prévu d'une source, dans un sens, à une date, pour une banque. */
    record FluxPrevu(Long banqueId, TransactionSource source, Type sens, LocalDate date, BigDecimal montant) {
    }

    private PlanTresorerieCalculateur() {
    }

    static PlanTresorerieDto calculer(PeriodeRecap periode, LocalDate aujourdHui,
                                      List<SoldeBanque> banques, List<FluxPrevu> flux) {
        LocalDate horizon = periode.horizon(aujourdHui);
        List<ColonnePlanDto> colonnes = colonnes(periode, aujourdHui, horizon);
        List<FluxPrevu> retenus = flux.stream()
                .filter(f -> f.date() != null && !f.date().isAfter(horizon) && f.montant() != null)
                .toList();

        List<PlanBanqueDto> plans = new ArrayList<>();
        for (SoldeBanque banque : banques) {
            List<FluxPrevu> fluxBanque = retenus.stream()
                    .filter(f -> banque.banqueId().equals(f.banqueId()))
                    .toList();
            plans.add(plan(banque.banqueId(), banque.nom(), nz(banque.soldeActuel()), nz(banque.plafondFC()),
                    fluxBanque, colonnes, aujourdHui, horizon));
        }

        List<Long> ids = banques.stream().map(SoldeBanque::banqueId).toList();
        List<FluxPrevu> fluxConnus = retenus.stream().filter(f -> ids.contains(f.banqueId())).toList();
        BigDecimal soldeTotal = banques.stream().map(b -> nz(b.soldeActuel())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal plafondTotal = banques.stream().map(b -> nz(b.plafondFC())).reduce(BigDecimal.ZERO, BigDecimal::add);
        PlanBanqueDto consolide = plan(null, LIBELLE_CONSOLIDE, soldeTotal, plafondTotal,
                fluxConnus, colonnes, aujourdHui, horizon);

        return new PlanTresorerieDto(periode, aujourdHui, horizon, colonnes, consolide, plans);
    }

    /**
     * Colonnes du plan : « À ce jour », puis du lendemain jusqu'à l'horizon, par semaine
     * (lundi-dimanche) pour la fin de mois, par mois civil pour la fin de trimestre.
     */
    static List<ColonnePlanDto> colonnes(PeriodeRecap periode, LocalDate aujourdHui, LocalDate horizon) {
        List<ColonnePlanDto> colonnes = new ArrayList<>();
        colonnes.add(new ColonnePlanDto(CODE_ECHU, "À ce jour", null, aujourdHui));

        LocalDate debut = aujourdHui.plusDays(1);
        int numero = 1;
        while (!debut.isAfter(horizon)) {
            LocalDate finNaturelle = periode == PeriodeRecap.FIN_DE_TRIMESTRE
                    ? debut.with(TemporalAdjusters.lastDayOfMonth())
                    : debut.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
            LocalDate fin = finNaturelle.isAfter(horizon) ? horizon : finNaturelle;
            String libelle = periode == PeriodeRecap.FIN_DE_TRIMESTRE
                    ? majuscule(MOIS_ANNEE.format(debut))
                    : "Du " + JOUR_MOIS.format(debut) + " au " + JOUR_MOIS.format(fin);
            colonnes.add(new ColonnePlanDto("P" + numero++, libelle, debut, fin));
            debut = fin.plusDays(1);
        }
        return colonnes;
    }

    private static PlanBanqueDto plan(Long banqueId, String nom, BigDecimal soldeActuel, BigDecimal plafond,
                                      List<FluxPrevu> flux, List<ColonnePlanDto> colonnes,
                                      LocalDate aujourdHui, LocalDate horizon) {
        int n = colonnes.size();
        Map<String, BigDecimal[]> encaissements = lignesVides(Type.ENCAISSEMENT, n);
        Map<String, BigDecimal[]> decaissements = lignesVides(Type.DECAISSEMENT, n);
        // Mouvement net par jour : les flux échus sont tous ramenés à aujourd'hui.
        TreeMap<LocalDate, BigDecimal> netParJour = new TreeMap<>();

        for (FluxPrevu f : flux) {
            int colonne = indexColonne(colonnes, f.date(), aujourdHui);
            String categorie = CategoriesTresorerie.categorie(f.source(), f.sens());
            boolean encaissement = f.sens() == Type.ENCAISSEMENT;
            Map<String, BigDecimal[]> lignes = encaissement ? encaissements : decaissements;
            BigDecimal[] ligne = lignes.computeIfAbsent(categorie != null ? categorie : AUTRES, k -> zeros(n));
            ligne[colonne] = ligne[colonne].add(f.montant());

            LocalDate jour = f.date().isAfter(aujourdHui) ? f.date() : aujourdHui;
            netParJour.merge(jour, encaissement ? f.montant() : f.montant().negate(), BigDecimal::add);
        }

        BigDecimal[] totalEnc = totaux(encaissements, n);
        BigDecimal[] totalDec = totaux(decaissements, n);
        List<BigDecimal> soldesFin = new ArrayList<>();
        List<BigDecimal> disponibilites = new ArrayList<>();
        BigDecimal solde = soldeActuel;
        for (int i = 0; i < n; i++) {
            solde = solde.add(totalEnc[i]).subtract(totalDec[i]);
            soldesFin.add(solde);
            disponibilites.add(solde.add(plafond));
        }

        List<SoldeJourDto> courbe = new ArrayList<>();
        BigDecimal soldeJour = soldeActuel;
        PointBasDto pointBas = null;
        int joursNegatifs = 0;
        for (LocalDate jour = aujourdHui; !jour.isAfter(horizon); jour = jour.plusDays(1)) {
            soldeJour = soldeJour.add(netParJour.getOrDefault(jour, BigDecimal.ZERO));
            courbe.add(new SoldeJourDto(jour, soldeJour));
            if (soldeJour.signum() < 0) {
                joursNegatifs++;
            }
            if (pointBas == null || soldeJour.compareTo(pointBas.solde()) < 0) {
                pointBas = new PointBasDto(jour, soldeJour, soldeJour.add(plafond));
            }
        }
        BigDecimal besoin = pointBas.disponibilite().signum() < 0 ? pointBas.disponibilite().negate() : BigDecimal.ZERO;

        return new PlanBanqueDto(
                banqueId, nom, soldeActuel, plafond,
                lignes(encaissements), lignes(decaissements),
                Arrays.asList(totalEnc), Arrays.asList(totalDec),
                soldesFin, disponibilites,
                solde, solde.add(plafond),
                pointBas, besoin, joursNegatifs, courbe);
    }

    private static int indexColonne(List<ColonnePlanDto> colonnes, LocalDate date, LocalDate aujourdHui) {
        if (!date.isAfter(aujourdHui)) {
            return 0;
        }
        for (int i = 1; i < colonnes.size(); i++) {
            if (!date.isAfter(colonnes.get(i).au())) {
                return i;
            }
        }
        return colonnes.size() - 1;
    }

    /** Toutes les catégories du sens, dans l'ordre d'affichage ; les lignes restées à zéro sont retirées à la fin. */
    private static Map<String, BigDecimal[]> lignesVides(Type sens, int n) {
        Map<String, BigDecimal[]> lignes = new LinkedHashMap<>();
        CategoriesTresorerie.ordre(sens).forEach(c -> lignes.put(c, zeros(n)));
        return lignes;
    }

    private static List<LignePlanDto> lignes(Map<String, BigDecimal[]> lignes) {
        List<LignePlanDto> resultat = new ArrayList<>();
        lignes.forEach((categorie, montants) -> {
            BigDecimal total = Arrays.stream(montants).reduce(BigDecimal.ZERO, BigDecimal::add);
            boolean vide = Arrays.stream(montants).allMatch(m -> m.signum() == 0);
            if (!vide) {
                resultat.add(new LignePlanDto(categorie, Arrays.asList(montants), total));
            }
        });
        return resultat;
    }

    private static BigDecimal[] totaux(Map<String, BigDecimal[]> lignes, int n) {
        BigDecimal[] totaux = zeros(n);
        for (BigDecimal[] montants : lignes.values()) {
            for (int i = 0; i < n; i++) {
                totaux[i] = totaux[i].add(montants[i]);
            }
        }
        return totaux;
    }

    private static BigDecimal[] zeros(int n) {
        BigDecimal[] valeurs = new BigDecimal[n];
        Arrays.fill(valeurs, BigDecimal.ZERO);
        return valeurs;
    }

    private static BigDecimal nz(BigDecimal valeur) {
        return valeur != null ? valeur : BigDecimal.ZERO;
    }

    private static String majuscule(String texte) {
        return texte.isEmpty() ? texte : Character.toUpperCase(texte.charAt(0)) + texte.substring(1);
    }
}
