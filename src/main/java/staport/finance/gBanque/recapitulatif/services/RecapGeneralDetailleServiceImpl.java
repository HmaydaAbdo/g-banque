package staport.finance.gBanque.recapitulatif.services;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Agios.entities.Agios;
import staport.finance.gBanque.Agios.repository.AgiosRepository;
import staport.finance.gBanque.Agios.spec.AgiosSpec;
import staport.finance.gBanque.Bank.dtos.response.BanqueResponse;
import staport.finance.gBanque.Bank.services.IBanqueService;
import staport.finance.gBanque.cmt.echeances.entities.EcheanceCMT;
import staport.finance.gBanque.cmt.echeances.repositories.EcheanceCMTRepository;
import staport.finance.gBanque.cmt.echeances.specification.EcheanceCMTSpecification;
import staport.finance.gBanque.commision.entities.Commission;
import staport.finance.gBanque.commision.repository.CommisionRepository;
import staport.finance.gBanque.commision.specs.CommissionSpecifications;
import staport.finance.gBanque.decomptes.entities.Decompte;
import staport.finance.gBanque.decomptes.repositories.DecompteRepository;
import staport.finance.gBanque.decomptes.spec.DecompteSpec;
import staport.finance.gBanque.droitConstate.entities.DroitConstate;
import staport.finance.gBanque.droitConstate.repositories.DroitConstateRepository;
import staport.finance.gBanque.droitConstate.spec.DroitConstateSpec;
import staport.finance.gBanque.factoring.entities.Factoring;
import staport.finance.gBanque.factoring.repository.FactoringRepository;
import staport.finance.gBanque.factoring.spec.FactoringSpec;
import staport.finance.gBanque.fraisBancaires.entities.FraisBancaire;
import staport.finance.gBanque.fraisBancaires.repositories.FraisBancaireRepository;
import staport.finance.gBanque.fraisBancaires.spec.FraisBancaireSpecification;
import staport.finance.gBanque.impayees.entities.Impayee;
import staport.finance.gBanque.impayees.repository.ImpayeeRepository;
import staport.finance.gBanque.impayees.specs.ImpayeeSpecifications;
import staport.finance.gBanque.impot_taxes.entities.ImpotEtTaxe;
import staport.finance.gBanque.impot_taxes.repositories.ImpotEtTaxeRepository;
import staport.finance.gBanque.impot_taxes.specs.ImpotEtTaxeSpecifications;
import staport.finance.gBanque.leasing.echeancesLeasing.entities.EcheanceLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.repositories.EcheanceLeasingRepository;
import staport.finance.gBanque.leasing.echeancesLeasing.specification.EcheanceLeasingSpecification;
import staport.finance.gBanque.moyensDePaiements.cheques.entities.Cheque;
import staport.finance.gBanque.moyensDePaiements.cheques.repositories.ChequeRepository;
import staport.finance.gBanque.moyensDePaiements.cheques.specs.ChequeSpec;
import staport.finance.gBanque.moyensDePaiements.lcn.entities.Lcn;
import staport.finance.gBanque.moyensDePaiements.lcn.repositories.LcnRepository;
import staport.finance.gBanque.moyensDePaiements.lcn.specs.LcnSpecification;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.entities.MiseADisposition;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.repositories.MiseADispositionRepository;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.specs.MiseADispositionSpec;
import staport.finance.gBanque.moyensDePaiements.virementInternational.entities.VirementInternational;
import staport.finance.gBanque.moyensDePaiements.virementInternational.repositories.VirementInternationalRepository;
import staport.finance.gBanque.moyensDePaiements.virementInternational.specs.VirementInternationalSpec;
import staport.finance.gBanque.moyensDePaiements.virements.entities.Virement;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.moyensDePaiements.virements.repositories.VirementRepository;
import staport.finance.gBanque.moyensDePaiements.virements.specs.VirementSpec;
import staport.finance.gBanque.operationErrone.entities.OperationErrone;
import staport.finance.gBanque.operationErrone.repositories.OperationErroneRepository;
import staport.finance.gBanque.operationErrone.specs.OperationErroneSpec;
import staport.finance.gBanque.prefinancement.entities.Prefinancement;
import staport.finance.gBanque.prefinancement.repository.PrefinancementRepository;
import staport.finance.gBanque.prefinancement.spec.PrefinancementSpecification;
import staport.finance.gBanque.prelevements.entitie.Prelevement;
import staport.finance.gBanque.prelevements.repository.PrelevementRepository;
import staport.finance.gBanque.prelevements.spec.PrelevementSpec;
import staport.finance.gBanque.recapitulatif.dtos.response.CategorieAmountDto;
import staport.finance.gBanque.recapitulatif.dtos.response.DecaissementSectionDto;
import staport.finance.gBanque.recapitulatif.dtos.response.EncaissementSectionDto;
import staport.finance.gBanque.recapitulatif.dtos.response.RecapGeneralDetailleAllBanquesDto;
import staport.finance.gBanque.recapitulatif.dtos.response.RecapGeneralDetailleDto;
import staport.finance.gBanque.recapitulatif.dtos.response.ResumeSectionDto;
import staport.finance.gBanque.recapitulatif.services.interfaces.IRecapGeneralDetailleService;
import staport.finance.gBanque.restitution.entities.Restitution;
import staport.finance.gBanque.restitution.repository.RestitutionRepository;
import staport.finance.gBanque.restitution.specs.RestitutionSpec;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Même limite de performance assumée (documentée) que {@link RecapitulatifServiceImpl} —
 * voir sa javadoc de classe.
 */
@Service
@Transactional(readOnly = true)
public class RecapGeneralDetailleServiceImpl implements IRecapGeneralDetailleService {

    private final IBanqueService banqueService;

    private final EcheanceLeasingRepository echeanceLeasingRepository;
    private final EcheanceCMTRepository echeanceCMTRepository;
    private final ChequeRepository chequeRepository;
    private final LcnRepository lcnRepository;
    private final VirementRepository virementRepository;
    private final MiseADispositionRepository miseADispositionRepository;
    private final PrefinancementRepository prefinancementRepository;
    private final DroitConstateRepository droitConstateRepository;
    private final DecompteRepository decompteRepository;
    private final CommisionRepository commissionRepository;
    private final AgiosRepository agiosRepository;
    private final PrelevementRepository prelevementRepository;
    private  final FraisBancaireRepository fraisBancaireRepository;
    private final FactoringRepository factoringRepository;
    private final ImpotEtTaxeRepository impotEtTaxeRepository;
    private final RestitutionRepository restitutionRepository;
    private final OperationErroneRepository operationErroneRepository;
    private final VirementInternationalRepository virementInternationalRepository;
    private final ImpayeeRepository impayeeRepository;


    public RecapGeneralDetailleServiceImpl(IBanqueService banqueService, EcheanceLeasingRepository echeanceLeasingRepository, EcheanceCMTRepository echeanceCMTRepository, ChequeRepository chequeRepository, LcnRepository lcnRepository, VirementRepository virementRepository, MiseADispositionRepository miseADispositionRepository, PrefinancementRepository prefinancementRepository, DroitConstateRepository droitConstateRepository, DecompteRepository decompteRepository, CommisionRepository commissionRepository, AgiosRepository agiosRepository, PrelevementRepository prelevementRepository, FraisBancaireRepository fraisBancaireRepository, FactoringRepository factoringRepository, ImpotEtTaxeRepository impotEtTaxeRepository, RestitutionRepository restitutionRepository, OperationErroneRepository operationErroneRepository, VirementInternationalRepository virementInternationalRepository, ImpayeeRepository impayeeRepository) {
        this.banqueService = banqueService;
        this.echeanceLeasingRepository = echeanceLeasingRepository;
        this.echeanceCMTRepository = echeanceCMTRepository;
        this.chequeRepository = chequeRepository;
        this.lcnRepository = lcnRepository;
        this.virementRepository = virementRepository;
        this.miseADispositionRepository = miseADispositionRepository;
        this.prefinancementRepository = prefinancementRepository;
        this.droitConstateRepository = droitConstateRepository;
        this.decompteRepository = decompteRepository;
        this.commissionRepository = commissionRepository;
        this.agiosRepository = agiosRepository;
        this.prelevementRepository = prelevementRepository;
        this.fraisBancaireRepository = fraisBancaireRepository;
        this.factoringRepository = factoringRepository;
        this.impotEtTaxeRepository = impotEtTaxeRepository;
        this.restitutionRepository = restitutionRepository;
        this.operationErroneRepository = operationErroneRepository;
        this.virementInternationalRepository = virementInternationalRepository;
        this.impayeeRepository = impayeeRepository;
    }

    @Override
    public RecapGeneralDetailleDto getRecapForBanque(Long banqueId) {
        LocalDate today      = LocalDate.now();
        LocalDate tomorrow   = today.plusDays(1);
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

        BigDecimal soldeActuel   = banque.soldeActuel();
        BigDecimal plafondLigne  = banqueService.getPlafondFC(banqueId);
        BigDecimal disponibilite = soldeActuel.add(plafondLigne);

        // ── Encours- : décaissements PREVU jusqu'à aujourd'hui ───────────────────
        BigDecimal encoursMoins = BigDecimal.ZERO;

        encoursMoins = encoursMoins.add(sum(
                echeanceLeasingRepository.findAll(
                        EcheanceLeasingSpecification.outstandingUntil(today)
                                .and(EcheanceLeasingSpecification.byContratBanqueId(banqueId))),
                EcheanceLeasing::getMontantTTC));

        encoursMoins = encoursMoins.add(sum(
                echeanceCMTRepository.findAll(
                        EcheanceCMTSpecification.outstandingUntil(today)
                                .and(EcheanceCMTSpecification.byCreditBanqueId(banqueId))),
                EcheanceCMT::getMontant));

        encoursMoins = encoursMoins.add(sum(
                chequeRepository.findAll(ChequeSpec.encoursDecaissements(banqueId, today)),
                Cheque::getMontant));

        encoursMoins = encoursMoins.add(sum(
                lcnRepository.findAll(LcnSpecification.encoursDecaissement(banqueId, today)),
                Lcn::getMontant));

        encoursMoins = encoursMoins.add(sum(
                virementRepository.findAll(VirementSpec.encoursDecaissement(banqueId, today)),
                Virement::getMontant));

        encoursMoins = encoursMoins.add(sum(
                miseADispositionRepository.findAll(MiseADispositionSpec.encoursDecaissement(banqueId, today)),
                MiseADisposition::getMontant));

        encoursMoins = encoursMoins.add(sum(
                virementInternationalRepository.findAll(VirementInternationalSpec.encoursDecaissement(banqueId, today)),
                VirementInternational::getMontant));

        encoursMoins = encoursMoins.add(sum(
                prefinancementRepository.findAll(
                        PrefinancementSpecification.outstandingRemboursementUntil(today)
                                .and(PrefinancementSpecification.byBanqueId(banqueId))),
                Prefinancement::getMontant));

        encoursMoins = encoursMoins.add(sum(
                factoringRepository.findAll(
                        FactoringSpec.outstandingRemboursementUntil(today)
                                .and(FactoringSpec.byBanqueId(banqueId))),
                Factoring::getMontant));

        encoursMoins = encoursMoins.add(sum(
                droitConstateRepository.findAll(
                        DroitConstateSpec.outstandingRemboursementUntil(today)
                                .and(DroitConstateSpec.byBanqueId(banqueId))),
                DroitConstate::getMontant));

        encoursMoins = encoursMoins.add(sum(
                commissionRepository.findAll(
                        CommissionSpecifications.outstandingUntil(today)
                                .and(CommissionSpecifications.hasBanque(banqueId))),
                Commission::getMontant));

        encoursMoins = encoursMoins.add(sum(
                agiosRepository.findAll(
                        AgiosSpec.outstandingUntil(today)
                                .and(AgiosSpec.hasBanque(banqueId))),
                Agios::getMontant));

        encoursMoins = encoursMoins.add(sum(
                prelevementRepository.findAll(
                        PrelevementSpec.outstandingUntil(today)
                                .and(PrelevementSpec.hasBanque(banqueId))),
                Prelevement::getMontant));

        encoursMoins = encoursMoins.add(sum(
                fraisBancaireRepository.findAll(
                        FraisBancaireSpecification.outstandingUntil(today)
                                .and(FraisBancaireSpecification.hasBanque(banqueId))),
                FraisBancaire::getMontant));

        encoursMoins = encoursMoins.add(sum(
                impotEtTaxeRepository.findAll(
                        ImpotEtTaxeSpecifications.outstandingUntil(today)
                                .and(ImpotEtTaxeSpecifications.hasBanque(banqueId))),
                ImpotEtTaxe::getMontant));

        encoursMoins = encoursMoins.add(sum(
                impayeeRepository.findAll(
                        ImpayeeSpecifications.outstandingUntil(today,Type.DECAISSEMENT)
                                .and(ImpayeeSpecifications.hasBanque(banqueId))),
                Impayee::getMontant));

        encoursMoins = encoursMoins.add(sum(
                operationErroneRepository.findAll(
                        OperationErroneSpec.outstandingUntil(today)
                                .and(OperationErroneSpec.hasBanque(banqueId))),
                OperationErrone::getMontant));

        // ── Encours+ : encaissements PREVU jusqu'à aujourd'hui ───────────────────
        BigDecimal encoursPlus = BigDecimal.ZERO;

        encoursPlus = encoursPlus.add(sum(
                virementRepository.findAll(VirementSpec.encoursEncaissement(banque.nom(), today)),
                Virement::getMontant));

        encoursPlus = encoursPlus.add(sum(
                chequeRepository.findAll(ChequeSpec.encoursEncaissements(banqueId, today)),
                Cheque::getMontant));

        encoursPlus = encoursPlus.add(sum(
                lcnRepository.findAll(LcnSpecification.encoursEncaissement(banqueId, today)),
                Lcn::getMontant));

        encoursPlus = encoursPlus.add(sum(
                virementInternationalRepository.findAll(VirementInternationalSpec.encoursEncaissement(banqueId, today)),
                VirementInternational::getMontant));

        encoursPlus = encoursPlus.add(sum(
                miseADispositionRepository.findAll(MiseADispositionSpec.encoursEncaissement(banqueId, today)),
                MiseADisposition::getMontant));

        encoursPlus = encoursPlus.add(sum(
                prefinancementRepository.findAll(
                        PrefinancementSpecification.outstandingDeblocageUntil(today)
                                .and(PrefinancementSpecification.byBanqueId(banqueId))),
                Prefinancement::getMontant));

        encoursPlus = encoursPlus.add(sum(
                factoringRepository.findAll(
                        FactoringSpec.outstandingDeblocageUntil(today)
                                .and(FactoringSpec.byBanqueId(banqueId))),
                Factoring::getMontant));

        encoursPlus = encoursPlus.add(sum(
                droitConstateRepository.findAll(
                        DroitConstateSpec.outstandingDeblocageUntil(today)
                                .and(DroitConstateSpec.byBanqueId(banqueId))),
                DroitConstate::getMontant));

        encoursPlus = encoursPlus.add(sum(
                decompteRepository.findAll(
                        DecompteSpec.outstandingUntil(today)
                                .and(DecompteSpec.byBanqueId(banqueId))),
                Decompte::getMontant));

        encoursPlus = encoursPlus.add(sum(
                restitutionRepository.findAll(
                        RestitutionSpec.outstandingUntil(today)
                                .and(RestitutionSpec.hasBanque(banqueId))),
                Restitution::getMontant));

        encoursPlus = encoursPlus.add(sum(
                impayeeRepository.findAll(
                        ImpayeeSpecifications.outstandingUntil(today,Type.ENCAISSEMENT)
                                .and(ImpayeeSpecifications.hasBanque(banqueId))),
                Impayee::getMontant));

        // ── Assemblage ────────────────────────────────────────────────────────────
        BigDecimal soldeProjete = disponibilite.add(encoursPlus).subtract(encoursMoins);

        return new ResumeSectionDto(soldeActuel, plafondLigne, disponibilite, encoursPlus, encoursMoins, soldeProjete);
    }

    private DecaissementSectionDto buildDecaissements(BanqueResponse banque, LocalDate from, LocalDate until) {
        Long banqueId = banque.id();
        List<CategorieAmountDto> categories = new ArrayList<>();

        // Leasing
        categories.add(categorie("Leasing", sum(
                echeanceLeasingRepository.findAll(
                        EcheanceLeasingSpecification.outstandingBetween(from, until)
                                .and(EcheanceLeasingSpecification.byContratBanqueId(banqueId))),
                EcheanceLeasing::getMontantTTC)));

        // CMT
        categories.add(categorie("CMT", sum(
                echeanceCMTRepository.findAll(
                        EcheanceCMTSpecification.outstandingBetween(from, until)
                                .and(EcheanceCMTSpecification.byCreditBanqueId(banqueId))),
                EcheanceCMT::getMontant)));

        // Moyens de paiement — DECAISSEMENT

        BigDecimal totalReglementFrns = Stream.of(
                sum(virementRepository.findAll(
                        VirementSpec.outstandingBetweenDecaissement(banque.id(), from, until)
                ), Virement::getMontant),

                sum(chequeRepository.findAll(
                        ChequeSpec.outstandingBetween(banqueId, from, until,
                                Type.DECAISSEMENT)
                ), Cheque::getMontant),

                sum(lcnRepository.findAll(
                        LcnSpecification.outstandingBetween(banqueId, from, until,
                                Type.DECAISSEMENT)
                ), Lcn::getMontant),

                sum(miseADispositionRepository.findAll(
                        MiseADispositionSpec.outstandingBetween(banqueId, from, until,
                                Type.DECAISSEMENT)
                ), MiseADisposition::getMontant)

        ).reduce(BigDecimal.ZERO, BigDecimal::add);

        categories.add(categorie("REGLEMENT FRNS", totalReglementFrns));


        categories.add(categorie("Virement International", sum(
                virementInternationalRepository.findAll(VirementInternationalSpec.outstandingBetween(banqueId, from, until,
                        Type.DECAISSEMENT)),
                VirementInternational::getMontant)));

        // Prefinancement / Factoring / DroitConstate — REMBOURSEMENT
        categories.add(categorie("Préfinancement", sum(
                prefinancementRepository.findAll(
                        PrefinancementSpecification.outstandingBetween(from, until,
                                        staport.finance.gBanque.prefinancement.enums.Type.REMBOURSEMENT)
                                .and(PrefinancementSpecification.byBanqueId(banqueId))),
                Prefinancement::getMontant)));

        categories.add(categorie("Factoring", sum(
                factoringRepository.findAll(
                        FactoringSpec.outstandingBetween(from, until,
                                        staport.finance.gBanque.prefinancement.enums.Type.REMBOURSEMENT)
                                .and(FactoringSpec.byBanqueId(banqueId))),
                Factoring::getMontant)));

        categories.add(categorie("Droit Constaté", sum(
                droitConstateRepository.findAll(
                        DroitConstateSpec.outstandingBetween(from, until,
                                        staport.finance.gBanque.prefinancement.enums.Type.REMBOURSEMENT)
                                .and(DroitConstateSpec.byBanqueId(banqueId))),
                DroitConstate::getMontant)));

        // Groupe 4
        categories.add(categorie("Commission", sum(
                commissionRepository.findAll(
                        CommissionSpecifications.outstandingBetween(from, until)
                                .and(CommissionSpecifications.hasBanque(banqueId))),
                Commission::getMontant)));

        categories.add(categorie("Agios", sum(
                agiosRepository.findAll(
                        AgiosSpec.outstandingBetween(from, until)
                                .and(AgiosSpec.hasBanque(banqueId))),
                Agios::getMontant)));

        categories.add(categorie("Prélèvement", sum(
                prelevementRepository.findAll(
                        PrelevementSpec.outstandingBetween(from, until)
                                .and(PrelevementSpec.hasBanque(banqueId))),
                Prelevement::getMontant)));

        categories.add(categorie("Frais Bancaires", sum(
                fraisBancaireRepository.findAll(
                        FraisBancaireSpecification.outstandingBetween(from, until)
                                .and(FraisBancaireSpecification.hasBanque(banqueId))),
                FraisBancaire::getMontant)));

        categories.add(categorie("Impôts & Taxes", sum(
                impotEtTaxeRepository.findAll(
                        ImpotEtTaxeSpecifications.outstandingBetween(from, until)
                                .and(ImpotEtTaxeSpecifications.hasBanque(banqueId))),
                ImpotEtTaxe::getMontant)));

        categories.add(categorie("Impayée", sum(
                impayeeRepository.findAll(
                        ImpayeeSpecifications.outstandingBetween(from, until,Type.DECAISSEMENT)
                                .and(ImpayeeSpecifications.hasBanque(banqueId))),
                Impayee::getMontant)));

        categories.add(categorie("Opération Erronée", sum(
                operationErroneRepository.findAll(
                        OperationErroneSpec.outstandingBetween(from, until)
                                .and(OperationErroneSpec.hasBanque(banqueId))),
                OperationErrone::getMontant)));

        // Total
        BigDecimal total = categories.stream()
                .map(CategorieAmountDto::montant)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new DecaissementSectionDto(categories, total);
    }
    private EncaissementSectionDto buildEncaissements(BanqueResponse banque, LocalDate from, LocalDate until) {
        Long banqueId = banque.id();
        List<CategorieAmountDto> categories = new ArrayList<>();

        // Moyens de paiement — ENCAISSEMENT
        BigDecimal totalEncaissementFrns = Stream.of(
                sum(virementRepository.findAll(
                        VirementSpec.outstandingBetweenEncaissement(banque.nom(), from, until)
                ), Virement::getMontant),

                sum(chequeRepository.findAll(
                        ChequeSpec.outstandingBetween(banqueId, from, until,
                                staport.finance.gBanque.moyensDePaiements.virements.enums.Type.ENCAISSEMENT)
                ), Cheque::getMontant),

                sum(lcnRepository.findAll(
                        LcnSpecification.outstandingBetween(banqueId, from, until,
                                staport.finance.gBanque.moyensDePaiements.virements.enums.Type.ENCAISSEMENT)
                ), Lcn::getMontant),

                sum(miseADispositionRepository.findAll(
                        MiseADispositionSpec.outstandingBetween(banqueId, from, until,
                                staport.finance.gBanque.moyensDePaiements.virements.enums.Type.ENCAISSEMENT)
                ), MiseADisposition::getMontant)

        ).reduce(BigDecimal.ZERO, BigDecimal::add);

        categories.add(categorie("REGLEMENT FRNS", totalEncaissementFrns));

        categories.add(categorie("Virement International", sum(
                virementInternationalRepository.findAll(VirementInternationalSpec.outstandingBetween(banqueId, from, until,
                        staport.finance.gBanque.moyensDePaiements.virements.enums.Type.ENCAISSEMENT)),
                VirementInternational::getMontant)));



        // Prefinancement / Factoring / DroitConstate — DEBLOCAGE
        categories.add(categorie("Préfinancement", sum(
                prefinancementRepository.findAll(
                        PrefinancementSpecification.outstandingBetween(from, until,
                                        staport.finance.gBanque.prefinancement.enums.Type.DEBLOCAGE)
                                .and(PrefinancementSpecification.byBanqueId(banqueId))),
                Prefinancement::getMontant)));

        categories.add(categorie("Factoring", sum(
                factoringRepository.findAll(
                        FactoringSpec.outstandingBetween(from, until,
                                        staport.finance.gBanque.prefinancement.enums.Type.DEBLOCAGE)
                                .and(FactoringSpec.byBanqueId(banqueId))),
                Factoring::getMontant)));

        categories.add(categorie("Droit Constaté", sum(
                droitConstateRepository.findAll(
                        DroitConstateSpec.outstandingBetween(from, until,
                                        staport.finance.gBanque.prefinancement.enums.Type.DEBLOCAGE)
                                .and(DroitConstateSpec.byBanqueId(banqueId))),
                DroitConstate::getMontant)));

        // Décompte + Restitution
        categories.add(categorie("Décompte", sum(
                decompteRepository.findAll(
                        DecompteSpec.outstandingBetween(from, until)
                                .and(DecompteSpec.byBanqueId(banqueId))),
                Decompte::getMontant)));

        categories.add(categorie("Restitution", sum(
                restitutionRepository.findAll(
                        RestitutionSpec.outstandingBetween(from, until)
                                .and(RestitutionSpec.hasBanque(banqueId))),
                Restitution::getMontant)));

        categories.add(categorie("Impayée", sum(
                impayeeRepository.findAll(
                        ImpayeeSpecifications.outstandingBetween(from, until,Type.ENCAISSEMENT)
                                .and(ImpayeeSpecifications.hasBanque(banqueId))),
                Impayee::getMontant)));

        // Total
        BigDecimal total = categories.stream()
                .map(CategorieAmountDto::montant)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new EncaissementSectionDto(categories, total);
    }

    private CategorieAmountDto categorie(String nom, BigDecimal montant) {
        return new CategorieAmountDto(nom, montant);
    }

    private <T> BigDecimal sum(List<T> items, Function<T, BigDecimal> extractor) {
        return items.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
