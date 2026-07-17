package staport.finance.gBanque.recapitulatif.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.dtos.response.BanqueResponse;
import staport.finance.gBanque.Bank.services.IBanqueService;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.recapitulatif.dtos.response.CategorieAmountDto;
import staport.finance.gBanque.recapitulatif.dtos.response.DecaissementSectionDto;
import staport.finance.gBanque.recapitulatif.dtos.response.EncaissementSectionDto;
import staport.finance.gBanque.recapitulatif.dtos.response.RecapGeneralDetailleAllBanquesDto;
import staport.finance.gBanque.recapitulatif.dtos.response.RecapGeneralDetailleDto;
import staport.finance.gBanque.recapitulatif.dtos.response.ResumeSectionDto;
import staport.finance.gBanque.recapitulatif.repositories.RecapitulatifEncoursRepository;
import staport.finance.gBanque.recapitulatif.services.interfaces.IRecapGeneralDetailleService;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static staport.finance.gBanque.transactions.enums.TransactionSource.*;

/**
 * Même remplacement que {@link RecapitulatifServiceImpl} — voir sa javadoc de classe.
 *
 * <p>Le regroupement par catégorie (ex. "REGLEMENT FRNS" = Cheque + Lcn + Virement +
 * MiseADisposition) est fait en Java à partir d'une somme déjà agrégée par
 * {@code (sourceType, sens)} en base ({@link RecapitulatifEncoursRepository#sumByBanqueAndSourceBetween})
 * — la boucle de regroupement porte sur ~19 sources, jamais sur le nombre de lignes d'encours.</p>
 */
@Service
@Transactional(readOnly = true)
public class RecapGeneralDetailleServiceImpl implements IRecapGeneralDetailleService {

    private final IBanqueService banqueService;
    private final RecapitulatifEncoursRepository encoursRepository;

    /** Ordre et libellés des catégories — identiques à l'ancien {@code buildDecaissements}. */
    private static final Map<TransactionSource, String> DECAISSEMENT_CATEGORIES = new LinkedHashMap<>();
    private static final List<String> DECAISSEMENT_ORDER = List.of(
            "Leasing", "CMT", "REGLEMENT FRNS", "Virement International", "Préfinancement",
            "Factoring", "Droit Constaté", "Commission", "Agios", "Prélèvement",
            "Frais Bancaires", "Impôts & Taxes", "Impayée", "Opération Erronée"
    );

    /** Ordre et libellés des catégories — identiques à l'ancien {@code buildEncaissements}. */
    private static final Map<TransactionSource, String> ENCAISSEMENT_CATEGORIES = new LinkedHashMap<>();
    private static final List<String> ENCAISSEMENT_ORDER = List.of(
            "REGLEMENT FRNS", "Virement International", "Préfinancement", "Factoring",
            "Droit Constaté", "Décompte", "Restitution", "Impayée"
    );

    static {
        DECAISSEMENT_CATEGORIES.put(LEASING, "Leasing");
        DECAISSEMENT_CATEGORIES.put(CMT, "CMT");
        DECAISSEMENT_CATEGORIES.put(CHEQUE, "REGLEMENT FRNS");
        DECAISSEMENT_CATEGORIES.put(LCN_DECAISSEMENT, "REGLEMENT FRNS");
        DECAISSEMENT_CATEGORIES.put(Virement, "REGLEMENT FRNS");
        DECAISSEMENT_CATEGORIES.put(MiseADisposition, "REGLEMENT FRNS");
        DECAISSEMENT_CATEGORIES.put(VIREMENT_INTERNATIONAL, "Virement International");
        DECAISSEMENT_CATEGORIES.put(PREFINANCEMENT, "Préfinancement");
        DECAISSEMENT_CATEGORIES.put(FACTORING, "Factoring");
        DECAISSEMENT_CATEGORIES.put(DROIT_CONSTATE, "Droit Constaté");
        DECAISSEMENT_CATEGORIES.put(COMMISSION, "Commission");
        DECAISSEMENT_CATEGORIES.put(AGIOS, "Agios");
        DECAISSEMENT_CATEGORIES.put(PRELEVEMENT, "Prélèvement");
        DECAISSEMENT_CATEGORIES.put(FRAIS_BANCAIRE, "Frais Bancaires");
        DECAISSEMENT_CATEGORIES.put(IMPOT_TAXE, "Impôts & Taxes");
        DECAISSEMENT_CATEGORIES.put(IMPAYEE, "Impayée");
        DECAISSEMENT_CATEGORIES.put(OPERATION_ERRONE, "Opération Erronée");

        ENCAISSEMENT_CATEGORIES.put(CHEQUE, "REGLEMENT FRNS");
        ENCAISSEMENT_CATEGORIES.put(LCN_ENCAISSEMENT, "REGLEMENT FRNS");
        ENCAISSEMENT_CATEGORIES.put(LCN_ESCOMPTE, "REGLEMENT FRNS");
        ENCAISSEMENT_CATEGORIES.put(Virement, "REGLEMENT FRNS");
        ENCAISSEMENT_CATEGORIES.put(MiseADisposition, "REGLEMENT FRNS");
        ENCAISSEMENT_CATEGORIES.put(VIREMENT_INTERNATIONAL, "Virement International");
        ENCAISSEMENT_CATEGORIES.put(PREFINANCEMENT, "Préfinancement");
        ENCAISSEMENT_CATEGORIES.put(FACTORING, "Factoring");
        ENCAISSEMENT_CATEGORIES.put(DROIT_CONSTATE, "Droit Constaté");
        ENCAISSEMENT_CATEGORIES.put(DECOMPTES, "Décompte");
        ENCAISSEMENT_CATEGORIES.put(RESTITUTION, "Restitution");
        ENCAISSEMENT_CATEGORIES.put(IMPAYEE, "Impayée");
    }

    public RecapGeneralDetailleServiceImpl(IBanqueService banqueService, RecapitulatifEncoursRepository encoursRepository) {
        this.banqueService = banqueService;
        this.encoursRepository = encoursRepository;
    }

    @Override
    public RecapGeneralDetailleDto getRecapForBanque(Long banqueId) {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        LocalDate endOfMonth = today.with(TemporalAdjusters.lastDayOfMonth());

        BanqueResponse banque = this.banqueService.getBanqueById(banqueId);

        ResumeSectionDto resume = buildResume(banque, today);
        DecaissementSectionDto decaissements = buildDecaissements(banque, tomorrow, endOfMonth);
        EncaissementSectionDto encaissements = buildEncaissements(banque, tomorrow, endOfMonth);

        BigDecimal soldePrevi = resume.soldeProjete()
                .subtract(decaissements.total())
                .add(encaissements.total());

        return new RecapGeneralDetailleDto(
                banque.id(),
                banque.nom(),
                today,
                resume,
                decaissements,
                encaissements,
                soldePrevi
        );
    }

    @Override
    public RecapGeneralDetailleAllBanquesDto getRecapAllBanques() {
        LocalDate today = LocalDate.now();

        List<RecapGeneralDetailleDto> banques = this.banqueService.getAllBanqueIds()
                .stream()
                .map(this::getRecapForBanque)
                .collect(Collectors.toList());

        return new RecapGeneralDetailleAllBanquesDto(today, banques);
    }

    private ResumeSectionDto buildResume(BanqueResponse banque, LocalDate today) {
        Long banqueId = banque.id();

        BigDecimal soldeActuel = banque.soldeActuel();
        BigDecimal plafondLigne = banqueService.getPlafondFC(banqueId);
        BigDecimal disponibilite = soldeActuel.add(plafondLigne);

        Map<Type, BigDecimal> sums = encoursRepository.sumByBanqueAndSensUntil(banqueId, today).stream()
                .collect(Collectors.toMap(
                        row -> (Type) row[0],
                        row -> (BigDecimal) row[1]
                ));

        BigDecimal encoursMoins = sums.getOrDefault(Type.DECAISSEMENT, BigDecimal.ZERO);
        BigDecimal encoursPlus = sums.getOrDefault(Type.ENCAISSEMENT, BigDecimal.ZERO);

        BigDecimal soldeProjete = disponibilite.add(encoursPlus).subtract(encoursMoins);

        return new ResumeSectionDto(soldeActuel, plafondLigne, disponibilite, encoursPlus, encoursMoins, soldeProjete);
    }

    private DecaissementSectionDto buildDecaissements(BanqueResponse banque, LocalDate from, LocalDate until) {
        Map<TransactionSource, BigDecimal> sumsBySource =
                sumsBySource(banque.id(), Type.DECAISSEMENT, from, until);

        List<CategorieAmountDto> categories = DECAISSEMENT_ORDER.stream()
                .map(category -> categorie(category, sumForCategory(category, DECAISSEMENT_CATEGORIES, sumsBySource)))
                .toList();

        BigDecimal total = sumTotal(categories);
        return new DecaissementSectionDto(categories, total);
    }

    private EncaissementSectionDto buildEncaissements(BanqueResponse banque, LocalDate from, LocalDate until) {
        Map<TransactionSource, BigDecimal> sumsBySource =
                sumsBySource(banque.id(), Type.ENCAISSEMENT, from, until);

        List<CategorieAmountDto> categories = ENCAISSEMENT_ORDER.stream()
                .map(category -> categorie(category, sumForCategory(category, ENCAISSEMENT_CATEGORIES, sumsBySource)))
                .toList();

        BigDecimal total = sumTotal(categories);
        return new EncaissementSectionDto(categories, total);
    }

    private Map<TransactionSource, BigDecimal> sumsBySource(Long banqueId, Type sens, LocalDate from, LocalDate until) {
        Map<TransactionSource, BigDecimal> sums = new LinkedHashMap<>();
        for (Object[] row : encoursRepository.sumByBanqueAndSourceBetween(banqueId, from, until)) {
            TransactionSource source = (TransactionSource) row[0];
            Type rowSens = (Type) row[1];
            BigDecimal montant = (BigDecimal) row[2];
            if (rowSens == sens) {
                sums.put(source, montant);
            }
        }
        return sums;
    }

    private BigDecimal sumForCategory(
            String category, Map<TransactionSource, String> categoryOf, Map<TransactionSource, BigDecimal> sums) {
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<TransactionSource, BigDecimal> entry : sums.entrySet()) {
            if (category.equals(categoryOf.get(entry.getKey()))) {
                total = total.add(entry.getValue());
            }
        }
        return total;
    }

    private BigDecimal sumTotal(List<CategorieAmountDto> categories) {
        BigDecimal total = BigDecimal.ZERO;
        for (CategorieAmountDto c : categories) {
            if (c.montant() != null) {
                total = total.add(c.montant());
            }
        }
        return total;
    }

    private CategorieAmountDto categorie(String nom, BigDecimal montant) {
        return new CategorieAmountDto(nom, montant);
    }
}
