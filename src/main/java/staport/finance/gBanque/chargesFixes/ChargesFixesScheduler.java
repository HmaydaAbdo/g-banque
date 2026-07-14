package staport.finance.gBanque.chargesFixes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Agios.dtos.AgiosRequestDto;
import staport.finance.gBanque.Agios.service.IAgiosService;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.services.IBanqueService;
import staport.finance.gBanque.fraisBancaires.dtos.FraisBancaireRequestDto;
import staport.finance.gBanque.fraisBancaires.service.IFraisBancaireService;
import staport.finance.gBanque.impot_taxes.dtos.request.ImpotEtTaxeRequestDto;
import staport.finance.gBanque.impot_taxes.service.IImpotEtTaxeService;
import staport.finance.gBanque.prelevements.dtos.PrelevementRequestDto;
import staport.finance.gBanque.prelevements.service.IPrelevementService;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Génère automatiquement les charges fixes bancaires (statut PREVU) le 1er de chaque mois.
 *
 * <p>Toutes les charges (mensuelles et trimestrielles) sont tentées à chaque exécution.
 * L'idempotence empêche les doublons :
 * <ul>
 *   <li>Mensuel  → fenêtre = mois courant</li>
 *   <li>Trimestriel → fenêtre = trimestre courant (3 mois)</li>
 * </ul>
 * Ainsi une charge trimestrielle générée en janvier ne sera pas recréée en février ni mars.
 */
@Component
public class ChargesFixesScheduler {

    private static final Logger log = LoggerFactory.getLogger(ChargesFixesScheduler.class);

    private final ChargesFixesRegistry registry;
    private final IBanqueService banqueService;

    private final IFraisBancaireService fraisBancaireService;
    private final IImpotEtTaxeService impotEtTaxeService;
    private final IAgiosService agiosService;
    private final IPrelevementService prelevementService;

    public ChargesFixesScheduler(ChargesFixesRegistry registry,
                                  IBanqueService banqueService,
                                  IFraisBancaireService fraisBancaireService,
                                  IImpotEtTaxeService impotEtTaxeService,
                                  IAgiosService agiosService,
                                  IPrelevementService prelevementService) {
        this.registry = registry;
        this.banqueService = banqueService;
        this.fraisBancaireService = fraisBancaireService;
        this.impotEtTaxeService = impotEtTaxeService;
        this.agiosService = agiosService;
        this.prelevementService = prelevementService;
    }

    // -----------------------------------------------------------------------
    // Point d'entrée : 1er de chaque mois à 07h00
    // -----------------------------------------------------------------------

    @Scheduled(cron = "0 0 7 L * *")
    @Transactional
    public void genererChargesFixesDuMois() {
        LocalDate aujourd_hui = LocalDate.now();
        log.info("[ChargesFixes] Génération du mois {} démarrée", aujourd_hui.withDayOfMonth(1));

        for (Map.Entry<String, List<ChargeFixeDefinition>> entry : registry.getChargesParBanque().entrySet()) {
            String nomBanque = entry.getKey();

            Optional<Banque> banqueOpt = banqueService.findByNomIgnoreCase(nomBanque);
            if (banqueOpt.isEmpty()) {
                log.warn("[ChargesFixes] Banque '{}' introuvable — ignorée.", nomBanque);
                continue;
            }
            Banque banque = banqueOpt.get();

            for (ChargeFixeDefinition charge : entry.getValue()) {
                LocalDate dateEmission = calculerDateEmission(aujourd_hui, charge.jourEmission());
                traiterCharge(banque, charge, dateEmission);
            }
        }

        log.info("[ChargesFixes] Génération terminée");
    }

    // -----------------------------------------------------------------------
    // Dispatch par type
    // -----------------------------------------------------------------------

    private void traiterCharge(Banque banque, ChargeFixeDefinition charge, LocalDate dateEmission) {
        switch (charge.type()) {
            case FRAIS_BANCAIRE -> creerFraisBancaire(banque, charge, dateEmission);
            case IMPOT_TAXE     -> creerImpotTaxe(banque, charge, dateEmission);
            case AGIOS          -> creerAgios(banque, charge, dateEmission);
            case PRELEVEMENT    -> creerPrelevement(banque, charge, dateEmission);
        }
    }

    // -----------------------------------------------------------------------
    // Création FRAIS BANCAIRE
    // -----------------------------------------------------------------------

    private void creerFraisBancaire(Banque banque, ChargeFixeDefinition charge, LocalDate dateEmission) {
        String libelle = construireLibelle(charge, dateEmission);
        if (dejaPresent(fraisBancaireService::existsChargeFixeGeneree,
                        banque.getId(), libelle, charge.jourEmission())) return;

        FraisBancaireRequestDto dto = new FraisBancaireRequestDto();
        dto.setLibelle(libelle);
        dto.setObservation(libelle);
        dto.setMontant(charge.montant());
        dto.setBanqueId(banque.getId());
        dto.setDateEmission(dateEmission);

        fraisBancaireService.createFraisBancaire(dto);
        log.info("[ChargesFixes] Frais bancaire '{}' créé (PREVU) pour {}.", libelle, banque.getNom());
    }

    // -----------------------------------------------------------------------
    // Création IMPOT / TAXE
    // -----------------------------------------------------------------------

    private void creerImpotTaxe(Banque banque, ChargeFixeDefinition charge, LocalDate dateEmission) {
        String libelle = construireLibelle(charge, dateEmission);
        if (dejaPresent(impotEtTaxeService::existsImpotTaxeGenere,
                        banque.getId(), libelle, charge.jourEmission())) return;

        ImpotEtTaxeRequestDto dto = new ImpotEtTaxeRequestDto(
                libelle, libelle, charge.montant(), banque.getId(), charge.motif(), dateEmission);

        impotEtTaxeService.createImpotEtTaxe(dto);
        log.info("[ChargesFixes] Impôt/taxe '{}' créé (PREVU) pour {}.", libelle, banque.getNom());
    }

    // -----------------------------------------------------------------------
    // Création AGIOS
    // -----------------------------------------------------------------------

    private void creerAgios(Banque banque, ChargeFixeDefinition charge, LocalDate dateEmission) {
        String libelle = construireLibelle(charge, dateEmission);
        if (dejaPresent(agiosService::existsChargeFixeGeneree,
                        banque.getId(), libelle, charge.jourEmission())) return;

        AgiosRequestDto dto = new AgiosRequestDto(libelle, libelle, charge.montant(), banque.getId(), dateEmission);

        agiosService.createAgios(dto);
        log.info("[ChargesFixes] Agios '{}' créé (PREVU) pour {}.", libelle, banque.getNom());
    }

    // -----------------------------------------------------------------------
    // Création PRELEVEMENT
    // -----------------------------------------------------------------------

    private void creerPrelevement(Banque banque, ChargeFixeDefinition charge, LocalDate dateEmission) {
        String libelle = construireLibelle(charge, dateEmission);
        if (dejaPresent(prelevementService::existsChargeFixeGeneree,
                        banque.getId(), libelle, charge.jourEmission())) return;

        PrelevementRequestDto dto = new PrelevementRequestDto();
        dto.setLibelle(libelle);
        dto.setObservation(libelle);
        dto.setMontant(charge.montant());
        dto.setBanqueId(banque.getId());
        dto.setDateEmission(dateEmission);

        prelevementService.createPrelevement(dto);
        log.info("[ChargesFixes] Prélèvement '{}' créé (PREVU) pour {}.", libelle, banque.getNom());
    }

    // -----------------------------------------------------------------------
    // Idempotence
    // -----------------------------------------------------------------------

    /**
     * Vérifie si la charge a déjà été créée dans la période courante.
     *
     * <ul>
     *   <li>Mensuel  (jourEmission ≠ -2) → fenêtre = mois courant</li>
     *   <li>Trimestriel (jourEmission = -2) → fenêtre = trimestre courant</li>
     * </ul>
     */
    private boolean dejaPresent(
            TriFunction<Long, String, LocalDate, LocalDate, Boolean> checker,
            Long banqueId,
            String libelle,
            int jourEmission) {

        LocalDate aujourd_hui = LocalDate.now();
        LocalDate debut;
        LocalDate fin;

        if (jourEmission == -2) {
            // Fenêtre = trimestre entier
            int moisDebut = ((aujourd_hui.get(IsoFields.QUARTER_OF_YEAR) - 1) * 3) + 1;
            int moisFin   = moisDebut + 2;
            debut = LocalDate.of(aujourd_hui.getYear(), moisDebut, 1);
            fin   = YearMonth.of(aujourd_hui.getYear(), moisFin).atEndOfMonth();
        } else {
            // Fenêtre = mois entier
            debut = aujourd_hui.withDayOfMonth(1);
            fin   = aujourd_hui.with(TemporalAdjusters.lastDayOfMonth());
        }

        if (checker.apply(banqueId, libelle, debut, fin)) {
            log.debug("[ChargesFixes] '{}' déjà présent dans la période — ignoré.", libelle);
            return true;
        }
        return false;
    }

    @FunctionalInterface
    private interface TriFunction<A, B, C, D, R> {
        R apply(A a, B b, C c, D d);
    }

    // -----------------------------------------------------------------------
    // Calcul de la date d'émission théorique
    // -----------------------------------------------------------------------

    private LocalDate calculerDateEmission(LocalDate reference, int jourEmission) {
        if (jourEmission > 0) {
            int jour = Math.min(jourEmission, reference.lengthOfMonth());
            return reference.withDayOfMonth(jour);
        }
        if (jourEmission == -1) {
            return reference.with(TemporalAdjusters.lastDayOfMonth());
        }
        if (jourEmission == -2) {
            int moisFinTrimestre = reference.get(IsoFields.QUARTER_OF_YEAR) * 3;
            return YearMonth.of(reference.getYear(), moisFinTrimestre).atEndOfMonth();
        }
        throw new IllegalArgumentException("jourEmission invalide : " + jourEmission);
    }

    // -----------------------------------------------------------------------
    // Construction du libellé dynamique
    // -----------------------------------------------------------------------

    private String construireLibelle(ChargeFixeDefinition charge, LocalDate dateEmission) {
        if (!charge.appendPeriode()) return charge.libelle();

        String periode;
        if (charge.jourEmission() == -2) {
            int trimestre = dateEmission.get(IsoFields.QUARTER_OF_YEAR);
            periode = "T" + trimestre + "/" + dateEmission.getYear();
        } else if (charge.moisPrecedent()) {
            LocalDate moisRef = dateEmission.minusMonths(1);
            periode = String.format("%02d/%d", moisRef.getMonthValue(), moisRef.getYear());
        } else {
            periode = String.format("%02d/%d", dateEmission.getMonthValue(), dateEmission.getYear());
        }
        return charge.libelle() + " " + periode;
    }
}
