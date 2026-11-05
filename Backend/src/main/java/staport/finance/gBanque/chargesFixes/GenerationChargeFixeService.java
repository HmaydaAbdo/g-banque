package staport.finance.gBanque.chargesFixes;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Agios.dtos.AgiosRequestDto;
import staport.finance.gBanque.Agios.service.IAgiosService;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.chargesFixes.config.entities.ChargeFixeConfig;
import staport.finance.gBanque.chargesFixes.config.services.interfaces.IChargeFixeConfigService;
import staport.finance.gBanque.fraisBancaires.dtos.FraisBancaireRequestDto;
import staport.finance.gBanque.fraisBancaires.service.IFraisBancaireService;
import staport.finance.gBanque.impot_taxes.dtos.request.ImpotEtTaxeRequestDto;
import staport.finance.gBanque.impot_taxes.service.IImpotEtTaxeService;
import staport.finance.gBanque.prelevements.dtos.PrelevementRequestDto;
import staport.finance.gBanque.prelevements.service.IPrelevementService;
import staport.finance.gBanque.exception.customExceptions.ValidationException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class GenerationChargeFixeService {

    private static final String A_GENERER = "A_GENERER";
    private static final String DEJA_PRESENTE = "DEJA_PRESENTE";
    private static final String DOUBLON_CONFIGURATION = "DOUBLON_CONFIGURATION";

    private final IChargeFixeConfigService configService;
    private final IFraisBancaireService fraisBancaireService;
    private final IImpotEtTaxeService impotEtTaxeService;
    private final IAgiosService agiosService;
    private final IPrelevementService prelevementService;

    public GenerationChargeFixeService(IChargeFixeConfigService configService,
                                       IFraisBancaireService fraisBancaireService,
                                       IImpotEtTaxeService impotEtTaxeService,
                                       IAgiosService agiosService,
                                       IPrelevementService prelevementService) {
        this.configService = configService;
        this.fraisBancaireService = fraisBancaireService;
        this.impotEtTaxeService = impotEtTaxeService;
        this.agiosService = agiosService;
        this.prelevementService = prelevementService;
    }

    @Transactional(readOnly = true)
    public GenerationChargeFixeResultDto apercu(GenerationChargeFixeRequest request) {
        return planifier(request, false);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public GenerationChargeFixeResultDto generer(GenerationChargeFixeRequest request) {
        if (request.configurationIds() == null || request.configurationIds().isEmpty()) {
            throw new ValidationException("Sélectionnez au moins une configuration mensuelle depuis l'aperçu.");
        }
        YearMonth moisDemande = YearMonth.of(request.annee(), request.mois());
        if (!moisDemande.isBefore(YearMonth.now())) {
            throw new ValidationException("Le cycle mensuel ne peut être rattrapé qu'après le mois de son exécution automatique.");
        }
        return planifier(request, true);
    }

    private GenerationChargeFixeResultDto planifier(GenerationChargeFixeRequest request, boolean enregistrer) {
        YearMonth mois = YearMonth.of(request.annee(), request.mois());
        Set<Long> selection = request.configurationIds() == null ? null : new HashSet<>(request.configurationIds());
        List<GenerationChargeFixeLineDto> lignes = new ArrayList<>();
        Set<String> dejaPlanifiees = new HashSet<>();
        int excluesTrimestrielles = 0;
        int generees = 0;

        List<ChargeFixeConfig> configurations = configService.findAllEntities();
        if (selection != null) {
            Set<Long> idsDisponibles = configurations.stream().map(ChargeFixeConfig::getId)
                    .collect(java.util.stream.Collectors.toSet());
            if (!idsDisponibles.containsAll(selection)) {
                throw new ValidationException("Une ou plusieurs configurations sélectionnées n'existent plus. Actualisez l'aperçu.");
            }
            boolean selectionTrimestrielle = configurations.stream()
                    .anyMatch(c -> selection.contains(c.getId()) && c.getJourEmission() == -2);
            if (selectionTrimestrielle) {
                throw new ValidationException("Les configurations trimestrielles ne peuvent pas être rattrapées depuis cet écran.");
            }
        }

        for (ChargeFixeConfig config : configurations) {
            if (config.getJourEmission() == -2) {
                excluesTrimestrielles++;
                continue;
            }
            if (selection != null && !selection.contains(config.getId())) continue;

            Banque banque = config.getBanque();
            LocalDate dateEmission = dateEmission(mois, config.getJourEmission());
            String libelle = construireLibelle(config, dateEmission);
            boolean exists = existe(config, banque.getId(), libelle, mois.atDay(1), mois.atEndOfMonth());
            String cle = config.getType() + ":" + banque.getId() + ":" + libelle.toLowerCase() + ":" + dateEmission;
            boolean doublonConfig = !exists && !dejaPlanifiees.add(cle);
            String statut = exists ? DEJA_PRESENTE : doublonConfig ? DOUBLON_CONFIGURATION : A_GENERER;
            String motif = doublonConfig
                    ? "Plusieurs configurations produisent la même écriture. Vérifier les configurations avant le rattrapage."
                    : null;

            if (enregistrer && A_GENERER.equals(statut)) {
                creer(config, banque, libelle, dateEmission);
                generees++;
                statut = "GENEREE";
            }
            lignes.add(new GenerationChargeFixeLineDto(config.getId(), banque.getNom(), config.getType(),
                    libelle, config.getMontant(), dateEmission, statut, motif));
        }

        int candidates = (int) lignes.stream().filter(l -> A_GENERER.equals(l.statut()) || "GENEREE".equals(l.statut())).count();
        int dejaPresentes = (int) lignes.stream().filter(l -> DEJA_PRESENTE.equals(l.statut())).count();
        return new GenerationChargeFixeResultDto(mois, candidates, generees, dejaPresentes,
                excluesTrimestrielles, List.copyOf(lignes));
    }

    private boolean existe(ChargeFixeConfig config, Long banqueId, String libelle,
                           LocalDate debut, LocalDate fin) {
        return switch (config.getType()) {
            case FRAIS_BANCAIRE -> fraisBancaireService.existsChargeFixeGeneree(banqueId, libelle, debut, fin);
            case IMPOT_TAXE -> impotEtTaxeService.existsImpotTaxeGenere(banqueId, libelle, debut, fin);
            case AGIOS -> agiosService.existsChargeFixeGeneree(banqueId, libelle, debut, fin);
            case PRELEVEMENT -> prelevementService.existsChargeFixeGeneree(banqueId, libelle, debut, fin);
        };
    }

    private void creer(ChargeFixeConfig config, Banque banque, String libelle, LocalDate dateEmission) {
        switch (config.getType()) {
            case FRAIS_BANCAIRE -> fraisBancaireService.createFraisBancaire(new FraisBancaireRequestDto(
                    libelle, libelle, config.getMontant(), banque.getId(), dateEmission));
            case IMPOT_TAXE -> impotEtTaxeService.createImpotEtTaxe(new ImpotEtTaxeRequestDto(
                    libelle, libelle, config.getMontant(), banque.getId(), config.getMotif(), dateEmission));
            case AGIOS -> agiosService.createAgios(new AgiosRequestDto(
                    libelle, libelle, config.getMontant(), banque.getId(), dateEmission));
            case PRELEVEMENT -> {
                PrelevementRequestDto dto = new PrelevementRequestDto();
                dto.setLibelle(libelle);
                dto.setObservation(libelle);
                dto.setMontant(config.getMontant());
                dto.setBanqueId(banque.getId());
                dto.setDateEmission(dateEmission);
                prelevementService.createPrelevement(dto);
            }
        }
    }

    private static LocalDate dateEmission(YearMonth mois, int jourEmission) {
        if (jourEmission > 0) return mois.atDay(Math.min(jourEmission, mois.lengthOfMonth()));
        if (jourEmission == -1) return mois.atEndOfMonth();
        if (jourEmission == -2) {
            int moisFinTrimestre = mois.get(IsoFields.QUARTER_OF_YEAR) * 3;
            return YearMonth.of(mois.getYear(), moisFinTrimestre).atEndOfMonth();
        }
        throw new IllegalArgumentException("jourEmission invalide : " + jourEmission);
    }

    private static String construireLibelle(ChargeFixeConfig config, LocalDate dateEmission) {
        if (!config.isAppendPeriode()) return config.getLibelle();
        String periode;
        if (config.getJourEmission() == -2) {
            periode = "T" + dateEmission.get(IsoFields.QUARTER_OF_YEAR) + "/" + dateEmission.getYear();
        } else {
            LocalDate mois = config.isMoisPrecedent() ? dateEmission.minusMonths(1) : dateEmission;
            periode = String.format("%02d/%d", mois.getMonthValue(), mois.getYear());
        }
        return config.getLibelle() + " " + periode;
    }

    /** Appelé uniquement par le scheduler historique, qui continue à traiter les charges trimestrielles. */
    @Transactional
    public void genererToutesChargesDuMois(YearMonth mois) {
        List<ChargeFixeConfig> configs = configService.findAllEntities();
        Set<String> dejaPlanifiees = new HashSet<>();
        for (ChargeFixeConfig config : configs) {
            Banque banque = config.getBanque();
            LocalDate emission = dateEmission(mois, config.getJourEmission());
            String libelle = construireLibelle(config, emission);
            LocalDate debut = config.getJourEmission() == -2
                    ? YearMonth.from(emission).withMonth(((emission.getMonthValue() - 1) / 3) * 3 + 1).atDay(1)
                    : mois.atDay(1);
            LocalDate fin = config.getJourEmission() == -2
                    ? emission
                    : mois.atEndOfMonth();
            String key = config.getType() + ":" + banque.getId() + ":" + libelle.toLowerCase() + ":" + emission;
            if (!dejaPlanifiees.add(key) || existe(config, banque.getId(), libelle, debut, fin)) continue;
            creer(config, banque, libelle, emission);
        }
    }
}
