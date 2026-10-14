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
import staport.finance.gBanque.recapitulatif.enums.PeriodeRecap;
import staport.finance.gBanque.recapitulatif.repositories.RecapitulatifEncoursRepository;
import staport.finance.gBanque.recapitulatif.services.interfaces.IRecapGeneralDetailleService;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Même remplacement que {@link RecapitulatifServiceImpl} — voir sa javadoc de classe.
 *
 * <p>Le regroupement par catégorie (ex. "REGLEMENT FRNS" = Cheque + Lcn + Virement +
 * MiseADisposition, voir {@link CategoriesTresorerie}) est fait en Java à partir d'une somme déjà
 * agrégée par {@code (sourceType, sens)} en base ({@link RecapitulatifEncoursRepository#sumByBanqueAndSourceBetween})
 * — la boucle de regroupement porte sur ~19 sources, jamais sur le nombre de lignes d'encours.</p>
 *
 * <p>La partie « à venir » couvre du lendemain jusqu'à l'horizon choisi : fin du mois en cours
 * (par défaut) ou fin du trimestre civil en cours (depuis le 14/10/2026).</p>
 */
@Service
@Transactional(readOnly = true)
public class RecapGeneralDetailleServiceImpl implements IRecapGeneralDetailleService {

    private final IBanqueService banqueService;
    private final RecapitulatifEncoursRepository encoursRepository;

    public RecapGeneralDetailleServiceImpl(IBanqueService banqueService, RecapitulatifEncoursRepository encoursRepository) {
        this.banqueService = banqueService;
        this.encoursRepository = encoursRepository;
    }

    @Override
    public RecapGeneralDetailleDto getRecapForBanque(Long banqueId, PeriodeRecap periode) {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        LocalDate horizon = periode.horizon(today);

        BanqueResponse banque = this.banqueService.getBanqueById(banqueId);

        ResumeSectionDto resume = buildResume(banque, today);
        DecaissementSectionDto decaissements = buildDecaissements(banque, tomorrow, horizon);
        EncaissementSectionDto encaissements = buildEncaissements(banque, tomorrow, horizon);

        BigDecimal soldePrevi = resume.soldeProjete()
                .subtract(decaissements.total())
                .add(encaissements.total());

        return new RecapGeneralDetailleDto(
                banque.id(),
                banque.nom(),
                today,
                periode,
                horizon,
                resume,
                decaissements,
                encaissements,
                soldePrevi
        );
    }

    @Override
    public RecapGeneralDetailleAllBanquesDto getRecapAllBanques(PeriodeRecap periode) {
        LocalDate today = LocalDate.now();

        List<RecapGeneralDetailleDto> banques = this.banqueService.getAllBanqueIds()
                .stream()
                .map(id -> getRecapForBanque(id, periode))
                .collect(Collectors.toList());

        return new RecapGeneralDetailleAllBanquesDto(today, periode, periode.horizon(today), banques);
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

        List<CategorieAmountDto> categories = CategoriesTresorerie.DECAISSEMENT_ORDER.stream()
                .map(category -> categorie(category, sumForCategory(category, Type.DECAISSEMENT, sumsBySource)))
                .toList();

        BigDecimal total = sumTotal(categories);
        return new DecaissementSectionDto(categories, total);
    }

    private EncaissementSectionDto buildEncaissements(BanqueResponse banque, LocalDate from, LocalDate until) {
        Map<TransactionSource, BigDecimal> sumsBySource =
                sumsBySource(banque.id(), Type.ENCAISSEMENT, from, until);

        List<CategorieAmountDto> categories = CategoriesTresorerie.ENCAISSEMENT_ORDER.stream()
                .map(category -> categorie(category, sumForCategory(category, Type.ENCAISSEMENT, sumsBySource)))
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
            String category, Type sens, Map<TransactionSource, BigDecimal> sums) {
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<TransactionSource, BigDecimal> entry : sums.entrySet()) {
            if (category.equals(CategoriesTresorerie.categorie(entry.getKey(), sens))) {
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
