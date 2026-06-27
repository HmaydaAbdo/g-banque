package staport.finance.gBanque.recapitulatif.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Agios.entities.Agios;
import staport.finance.gBanque.Agios.repository.AgiosRepository;
import staport.finance.gBanque.Agios.spec.AgiosSpec;
import staport.finance.gBanque.Bank.dtos.response.BanqueResponse;
import staport.finance.gBanque.Bank.services.IBanqueService;
import staport.finance.gBanque.impayees.entities.Impayee;
import staport.finance.gBanque.impayees.repository.ImpayeeRepository;
import staport.finance.gBanque.impayees.specs.ImpayeeSpecifications;
import staport.finance.gBanque.impot_taxes.entities.ImpotEtTaxe;
import staport.finance.gBanque.impot_taxes.repositories.ImpotEtTaxeRepository;
import staport.finance.gBanque.impot_taxes.specs.ImpotEtTaxeSpecifications;
import staport.finance.gBanque.moyensDePaiements.cheques.entities.Cheque;
import staport.finance.gBanque.moyensDePaiements.cheques.repositories.ChequeRepository;
import staport.finance.gBanque.moyensDePaiements.cheques.specs.ChequeSpec;
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
import staport.finance.gBanque.fraisBancaires.spec.FraisBancaireSpec;
import staport.finance.gBanque.moyensDePaiements.lcn.entities.Lcn;
import staport.finance.gBanque.moyensDePaiements.lcn.repositories.LcnRepository;
import staport.finance.gBanque.moyensDePaiements.lcn.specs.LcnSpecification;
import staport.finance.gBanque.leasing.echeancesLeasing.entities.EcheanceLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.repositories.EcheanceLeasingRepository;
import staport.finance.gBanque.leasing.echeancesLeasing.specification.EcheanceLeasingSpecification;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.entities.MiseADisposition;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.repositories.MiseADispositionRepository;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.specs.MiseADispositionSpec;
import staport.finance.gBanque.moyensDePaiements.virementInternational.entities.VirementInternational;
import staport.finance.gBanque.moyensDePaiements.virementInternational.repositories.VirementInternationalRepository;
import staport.finance.gBanque.moyensDePaiements.virementInternational.specs.VirementInternationalSpec;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.operationErrone.entities.OperationErrone;
import staport.finance.gBanque.operationErrone.repositories.OperationErroneRepository;
import staport.finance.gBanque.operationErrone.specs.OperationErroneSpec;
import staport.finance.gBanque.prefinancement.entities.Prefinancement;
import staport.finance.gBanque.prefinancement.repository.PrefinancementRepository;
import staport.finance.gBanque.prefinancement.spec.PrefinancementSpecification;
import staport.finance.gBanque.prelevements.entitie.Prelevement;
import staport.finance.gBanque.prelevements.repository.PrelevementRepository;
import staport.finance.gBanque.prelevements.spec.PrelevementSpec;
import staport.finance.gBanque.recapitulatif.dtos.AgregatedRecapDto;
import staport.finance.gBanque.recapitulatif.dtos.DecaissementSearchDto;
import staport.finance.gBanque.recapitulatif.dtos.RecaputilatifGlobal;
import staport.finance.gBanque.recapitulatif.dtos.RecaputilatifItem;
import staport.finance.gBanque.recapitulatif.mappers.RecapitulatifDecaissementMapper;
import staport.finance.gBanque.recapitulatif.mappers.RecapitulatifEncaissementMapper;
import staport.finance.gBanque.restitution.entities.Restitution;
import staport.finance.gBanque.restitution.repository.RestitutionRepository;
import staport.finance.gBanque.restitution.specs.RestitutionSpec;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.moyensDePaiements.virements.entities.Virement;
import staport.finance.gBanque.moyensDePaiements.virements.repositories.VirementRepository;
import staport.finance.gBanque.moyensDePaiements.virements.specs.VirementSpec;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;



/**
 * Service to generate a consolidated summary of all expected disbursements
 * (leasing and CMT installments) for the current month.
 */
@Service
@Transactional(readOnly = true)
public class RecapitulatifService {



    private final RecapitulatifDecaissementMapper decaissementMapper;
    private final RecapitulatifEncaissementMapper encaissementMapper;
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

    LocalDate endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());


    public RecapitulatifService(
            EcheanceLeasingRepository echeanceLeasingRepository,
            EcheanceCMTRepository echeanceCMTRepository,
            RecapitulatifDecaissementMapper mapper, RecapitulatifEncaissementMapper encaissementMapper, IBanqueService banqueService, ChequeRepository chequeRepository, LcnRepository lcnRepository, VirementRepository virementRepository, MiseADispositionRepository miseADispositionRepository, PrefinancementRepository prefinancementRepository, DroitConstateRepository droitConstateRepository, DecompteRepository decompteRepository, CommisionRepository commissionRepository, AgiosRepository agiosRepository, PrelevementRepository prelevementRepository, FraisBancaireRepository fraisBancaireRepository, FactoringRepository factoringRepository, ImpotEtTaxeRepository impotEtTaxeRepository, RestitutionRepository restitutionRepository, OperationErroneRepository operationErroneRepository, VirementInternationalRepository virementInternationalRepository, ImpayeeRepository impayeeRepository) {
        this.echeanceLeasingRepository = echeanceLeasingRepository;
        this.echeanceCMTRepository = echeanceCMTRepository;
        this.decaissementMapper = mapper;
        this.encaissementMapper = encaissementMapper;
        this.banqueService = banqueService;
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




    public AgregatedRecapDto getRecapitulatifEncaissementForCurrentMonthForGivenBanque(Pageable pageable, DecaissementSearchDto searchDto) {

        // Retrieve parameters from the DTO
        Long banqueId = searchDto.getBanqueId();
        BanqueResponse banque = this.banqueService.getBanqueById(banqueId);
        List<TransactionSource> transactionSources = searchDto.getTransactionSources();
        String libelleFilter = searchDto.getLibelle();
        String observationFilter = searchDto.getObservation();

        // Step 1: Create specifications for each entity type, filtering by bank and current month.

        //Moyens de paiement
        Specification<Virement> virementSpec = VirementSpec.encoursEncaissement(banque.nom(),endOfMonth);
        Specification<Cheque> chequeSpec = ChequeSpec.encoursEncaissements(banque.id(),endOfMonth);
        Specification<Lcn> lcnSpec = LcnSpecification.encoursEncaissement(banqueId,endOfMonth);
        Specification<VirementInternational> virementInternationalSpec=VirementInternationalSpec.encoursEncaissement(banqueId,endOfMonth);

        // préfinancement (Déblocage only)
        Specification<Prefinancement> prefinancementSpecification = PrefinancementSpecification.outstandingDeblocageUntil(endOfMonth)
                .and(PrefinancementSpecification.byBanqueId(banqueId));

        // Droit Constaté ( Déblocage only )
        Specification<DroitConstate> droitConstateSpecification = DroitConstateSpec.outstandingDeblocageUntil(endOfMonth)
                .and(DroitConstateSpec.byBanqueId(banqueId));

        // Factoring ( Déblocage only )
        Specification<Factoring> factoringSpecification = FactoringSpec.outstandingDeblocageUntil(endOfMonth)
                .and(FactoringSpec.byBanqueId(banqueId));

        //décomptes
        Specification<Decompte> decompteSpecification = DecompteSpec.outstandingUntil(endOfMonth)
                .and(DecompteSpec.byBanqueId(banqueId));

        // Restitutions
        Specification<Restitution> restitutionSpecification = RestitutionSpec.outstandingUntil(endOfMonth)
                .and(RestitutionSpec.hasBanque(banqueId));

        // Impayée
        Specification<Impayee> impayeeSpecification = ImpayeeSpecifications.outstandingUntil(endOfMonth,Type.ENCAISSEMENT)
                .and(ImpayeeSpecifications.hasBanque(banqueId));



        // Step 2: Fetch all relevant data for the current month from all repositories.
        List<Virement> outstandingVirements = virementRepository.findAll(virementSpec);
        List<Cheque> outstandingCheques = chequeRepository.findAll(chequeSpec);
        List<Lcn> outstandingLcns = lcnRepository.findAll(lcnSpec);
        List<VirementInternational> outstandingInternationalVirements=virementInternationalRepository.findAll(virementInternationalSpec);

        List<Prefinancement> outstandingPrefinancements = prefinancementRepository.findAll(prefinancementSpecification);
        List<DroitConstate> outstandingDroitsConstate = droitConstateRepository.findAll(droitConstateSpecification);
        List<Factoring> outstandingFactoring = factoringRepository.findAll(factoringSpecification);
        List<Decompte> outstandingDecomptes = decompteRepository.findAll(decompteSpecification);

        List<Restitution> outstandingRestitutions = restitutionRepository.findAll(restitutionSpecification);
        List<Impayee> outstandingImpayee = impayeeRepository.findAll(impayeeSpecification);


        // Step 3: Combine all lists and map them to the unified DTO.
        List<RecaputilatifItem> combinedItems = new ArrayList<>();
        combinedItems.addAll(outstandingPrefinancements.stream().map(encaissementMapper::mapPrefinancementToDto).toList());
        combinedItems.addAll(outstandingDroitsConstate.stream().map(encaissementMapper::mapDroitConstateToDto).toList());
        combinedItems.addAll(outstandingFactoring.stream().map(encaissementMapper::mapFactoringToDto).toList());
        combinedItems.addAll(outstandingDecomptes.stream().map(encaissementMapper::mapDecompteToDto).toList());
        combinedItems.addAll(outstandingVirements.stream().map(encaissementMapper::mapVirementToDto).toList());
        combinedItems.addAll(outstandingInternationalVirements.stream().map(encaissementMapper::mapVirementInternationalToDto).toList());
        combinedItems.addAll(outstandingCheques.stream().map(encaissementMapper::mapChequeToDto).toList());
        combinedItems.addAll(outstandingLcns.stream().map(encaissementMapper::mapLcnToDto).toList());
        combinedItems.addAll(outstandingRestitutions.stream().map(encaissementMapper::mapRestitutionToDto).toList());
        combinedItems.addAll(outstandingImpayee.stream().map(encaissementMapper::mapImpayeeToDto).toList());

        // Apply the filters from the DTO
        if (transactionSources != null && !transactionSources.isEmpty()) {
            combinedItems = combinedItems.stream()
                    .filter(item -> transactionSources.contains(item.getSource()))
                    .collect(Collectors.toList());
        }

        if (libelleFilter != null && !libelleFilter.isBlank()) {
            combinedItems = combinedItems.stream()
                    .filter(item -> item.getLibelle() != null && item.getLibelle().toLowerCase().contains(libelleFilter.toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (observationFilter != null && !observationFilter.isBlank()) {
            combinedItems = combinedItems.stream()
                    .filter(item -> item.getObservations() != null && item.getObservations().toLowerCase().contains(observationFilter.toLowerCase()))
                    .collect(Collectors.toList());
        }

        // New Step: Calculate the total debit and credit of the filtered list before pagination
        BigDecimal totalDebit = combinedItems.stream()
                .map(RecaputilatifItem::getDebit)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredit = combinedItems.stream()
                .map(RecaputilatifItem::getCredit)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Step 4: Sort the entire consolidated list based on the Pageable's sort information.
        Comparator<RecaputilatifItem> comparator = getComparatorForSorting(pageable);
        combinedItems.sort(comparator);

        // Step 5: Manually apply pagination to the sorted list.
        int pageSize = pageable.getPageSize();
        int currentPage = pageable.getPageNumber();
        int startItem = currentPage * pageSize;
        List<RecaputilatifItem> pagedItems;

        if (combinedItems.size() < startItem) {
            pagedItems = List.of();
        } else {
            int toIndex = Math.min(startItem + pageSize, combinedItems.size());
            pagedItems = combinedItems.subList(startItem, toIndex);
        }

        // Step 6: Wrap the sublist in a Page object
        Page<RecaputilatifItem> pageResult = new PageImpl<>(pagedItems, pageable, combinedItems.size());

        // Step 7: Create and return the AgregatedRecapDto
        AgregatedRecapDto aggregatedDto = new AgregatedRecapDto();
        aggregatedDto.setPage(pageResult);
        aggregatedDto.setTotalDebit(totalDebit);
        aggregatedDto.setTotalCredit(totalCredit);

        return aggregatedDto;
    }
    public List<RecaputilatifItem> getEncaissementByBanque(BanqueResponse banque) {

        List<RecaputilatifItem> encaissements= new ArrayList<>();

        //Moyens de paiement
        Specification<Virement> virementSpec = VirementSpec.encoursEncaissement(banque.nom(),endOfMonth);
        Specification<Cheque> chequeSpec = ChequeSpec.encoursEncaissements(banque.id(),endOfMonth);
        Specification<Lcn> lcnSpec = LcnSpecification.encoursEncaissement(banque.id(),endOfMonth);
        Specification<VirementInternational> virementInternationalSpec=VirementInternationalSpec.encoursEncaissement(banque.id(),endOfMonth);

        // préfinancement (Déblocage only)
        Specification<Prefinancement> prefinancementSpecification = PrefinancementSpecification.outstandingDeblocageUntil(endOfMonth)
                .and(PrefinancementSpecification.byBanqueId(banque.id()));

        // Droit Constaté ( Déblocage only )
        Specification<DroitConstate> droitConstateSpecification = DroitConstateSpec.outstandingDeblocageUntil(endOfMonth)
                .and(DroitConstateSpec.byBanqueId(banque.id()));

        // Factoring ( Déblocage only )
        Specification<Factoring> factoringSpecification = FactoringSpec.outstandingDeblocageUntil(endOfMonth)
                .and(FactoringSpec.byBanqueId(banque.id()));

        //décomptes
        Specification<Decompte> decompteSpecification = DecompteSpec.outstandingUntil(endOfMonth)
                .and(DecompteSpec.byBanqueId(banque.id()));

        // Restitutions
        Specification<Restitution> restitutionSpecification = RestitutionSpec.outstandingUntil(endOfMonth)
                .and(RestitutionSpec.hasBanque(banque.id()));

        Specification<Impayee> impayeeSpecification = ImpayeeSpecifications.outstandingUntil(endOfMonth,Type.ENCAISSEMENT)
                .and(ImpayeeSpecifications.hasBanque(banque.id()));



        // Step 2: Fetch all relevant data for the current month from all repositories.
        List<Prefinancement> outstandingPrefinancements = prefinancementRepository.findAll(prefinancementSpecification);
        List<DroitConstate> outstandingDroitsConstate = droitConstateRepository.findAll(droitConstateSpecification);
        List<Factoring> outstandingFactoring = factoringRepository.findAll(factoringSpecification);
        List<Decompte> outstandingDecomptes = decompteRepository.findAll(decompteSpecification);

        List<Virement> outstandingVirements = virementRepository.findAll(virementSpec);
        List<Cheque> outstandingCheques = chequeRepository.findAll(chequeSpec);
        List<Lcn> outstandingLcns = lcnRepository.findAll(lcnSpec);
        List<VirementInternational> outstandingInternationalVirements=virementInternationalRepository.findAll(virementInternationalSpec);

        List<Restitution> outstandingRestitutions = restitutionRepository.findAll(restitutionSpecification);
        List<Impayee> oustandingImpayee = impayeeRepository.findAll(impayeeSpecification);


        encaissements.addAll(outstandingPrefinancements.stream().map(encaissementMapper::mapPrefinancementToDto).toList());
        encaissements.addAll(outstandingDroitsConstate.stream().map(encaissementMapper::mapDroitConstateToDto).toList());
        encaissements.addAll(outstandingFactoring.stream().map(encaissementMapper::mapFactoringToDto).toList());
        encaissements.addAll(outstandingDecomptes.stream().map(encaissementMapper::mapDecompteToDto).toList());
        encaissements.addAll(outstandingVirements.stream().map(encaissementMapper::mapVirementToDto).toList());
        encaissements.addAll(outstandingInternationalVirements.stream().map(encaissementMapper::mapVirementInternationalToDto).toList());
        encaissements.addAll(outstandingCheques.stream().map(encaissementMapper::mapChequeToDto).toList());
        encaissements.addAll(outstandingLcns.stream().map(encaissementMapper::mapLcnToDto).toList());
        encaissements.addAll(outstandingRestitutions.stream().map(encaissementMapper::mapRestitutionToDto).toList());
        encaissements.addAll(oustandingImpayee.stream().map(encaissementMapper::mapImpayeeToDto).toList());

        return encaissements;
    }

    public AgregatedRecapDto getRecapitulatifDecaissementForCurrentMonthForGivenBanque(Pageable pageable, DecaissementSearchDto searchDto) {

        // Retrieve parameters from the DTO
        Long banqueId = searchDto.getBanqueId();
        List<TransactionSource> transactionSources = searchDto.getTransactionSources();
        String libelleFilter = searchDto.getLibelle();
        String observationFilter = searchDto.getObservation();

        // Step 1: Create specifications for each entity type, filtering by bank and current month.

        //leasing
        Specification<EcheanceLeasing> leasingSpec = Specification.where(EcheanceLeasingSpecification.outstandingUntil(endOfMonth))
                .and(EcheanceLeasingSpecification.byContratBanqueId(banqueId));
        //CMT
        Specification<EcheanceCMT> cmtSpec = Specification.where(EcheanceCMTSpecification.outstandingUntil(endOfMonth))
                .and(EcheanceCMTSpecification.byCreditBanqueId(banqueId));

        //moyens de paiement
        Specification<Cheque> chequeSpec = ChequeSpec.encoursDecaissements(banqueId,endOfMonth);
        Specification<Lcn> lcnSpec = LcnSpecification.encoursDecaissement(banqueId,endOfMonth);
        Specification<Virement> virementSpec = VirementSpec.encoursDecaissement(banqueId,endOfMonth);
        Specification<MiseADisposition> madSpec = MiseADispositionSpec.encoursDecaissement(banqueId,endOfMonth);
        Specification<VirementInternational> virementInternationalSpec= VirementInternationalSpec.encoursDecaissement(banqueId,endOfMonth);

        //operation erronées
        Specification<OperationErrone> operationErroneSpec = OperationErroneSpec.outstandingUntil(endOfMonth)
                .and(OperationErroneSpec.hasBanque(banqueId));

        // préfinancement (Remboursement only)
        Specification<Prefinancement> prefinancementSpecification = PrefinancementSpecification.outstandingRemboursementUntil(endOfMonth)
                .and(PrefinancementSpecification.byBanqueId(banqueId));

        //Droit constatés ( Remboursement only )
        Specification<DroitConstate> droitConstateSpecification = DroitConstateSpec.outstandingRemboursementUntil(endOfMonth)
                .and(DroitConstateSpec.byBanqueId(banqueId));

        //Factoring ( Remboursement only )
        Specification<Factoring> factoringSpecification = FactoringSpec.outstandingRemboursementUntil(endOfMonth)
                .and(FactoringSpec.byBanqueId(banqueId));

        //Commissions
        Specification<Commission> commissionSpecification= CommissionSpecifications.outstandingUntil(endOfMonth)
                .and(CommissionSpecifications.hasBanque(banqueId));

        //Agios
        Specification<Agios> agiosSpecification= AgiosSpec.outstandingUntil(endOfMonth)
                .and(AgiosSpec.hasBanque(banqueId));


        //prelevements
        Specification<Prelevement> prelevementSpecification= PrelevementSpec.outstandingUntil(endOfMonth)
                .and(PrelevementSpec.hasBanque(banqueId));

        // frais bancaire
        Specification<FraisBancaire> fraisBancaireSpecification= FraisBancaireSpec.outstandingUntil(endOfMonth)
                .and(FraisBancaireSpec.hasBanque(banqueId));

        //impots et taxes
        Specification<ImpotEtTaxe> impotEtTaxeSpecification= ImpotEtTaxeSpecifications.outstandingUntil(endOfMonth)
                .and(ImpotEtTaxeSpecifications.hasBanque(banqueId));

        //impayee
        Specification<Impayee> impayeeSpec=ImpayeeSpecifications.outstandingUntil(endOfMonth, Type.DECAISSEMENT)
                .and(ImpayeeSpecifications.hasBanque(banqueId));

        // Step 2: Fetch all relevant data for the current month from all repositories.
        List<EcheanceLeasing> outstandingLeasingPayments = echeanceLeasingRepository.findAll(leasingSpec);
        List<EcheanceCMT> outstandingCmtPayments = echeanceCMTRepository.findAll(cmtSpec);
        List<Cheque> activeCheques = chequeRepository.findAll(chequeSpec);
        List<Lcn> activeLcns = lcnRepository.findAll(lcnSpec);
        List<Virement> activeVirements = virementRepository.findAll(virementSpec);
        List<MiseADisposition> activeMisesADisposition = miseADispositionRepository.findAll(madSpec);
        List<VirementInternational> activeVirementInternationals=virementInternationalRepository.findAll(virementInternationalSpec);
        List<OperationErrone> outstandingOperationsErrones = operationErroneRepository.findAll(operationErroneSpec);
        List<Prefinancement> outstandingPrefinancements = prefinancementRepository.findAll(prefinancementSpecification);
        List<DroitConstate> outstandingDroitsConstate = droitConstateRepository.findAll(droitConstateSpecification);
        List<Factoring> outstandingFactoring = factoringRepository.findAll(factoringSpecification);
        List<Commission> outstandingCommissions=commissionRepository.findAll(commissionSpecification);
        List<Agios> outstandingAgios=agiosRepository.findAll(agiosSpecification);
        List<Prelevement> outstandingPrelevements=prelevementRepository.findAll(prelevementSpecification);
        List<FraisBancaire> outstandingFraisBancaire=fraisBancaireRepository.findAll(fraisBancaireSpecification);
        List<ImpotEtTaxe> outstandingImpotsEtTaxes=impotEtTaxeRepository.findAll(impotEtTaxeSpecification);
        List<Impayee> outstandingImpayee=impayeeRepository.findAll(impayeeSpec);

        // Step 3: Combine all lists and map them to the unified DTO.
        List<RecaputilatifItem> combinedItems = new ArrayList<>();
        // Add items from the new data sources.
        combinedItems.addAll(outstandingLeasingPayments.stream().map(decaissementMapper::mapLeasingToDto).toList());
        combinedItems.addAll(outstandingCmtPayments.stream().map(decaissementMapper::mapCmtToDto).toList());
        combinedItems.addAll(activeCheques.stream().map(decaissementMapper::mapChequeToDto).toList());
        combinedItems.addAll(activeLcns.stream().map(decaissementMapper::mapLcnToDto).toList());
        combinedItems.addAll(activeVirements.stream().map(decaissementMapper::mapVirementToDto).toList());
        combinedItems.addAll(activeMisesADisposition.stream().map(decaissementMapper::mapMiseADispositionToDto).toList());
        combinedItems.addAll(activeVirementInternationals.stream().map(decaissementMapper::mapVirementInternationalToDto).toList());
        combinedItems.addAll(outstandingOperationsErrones.stream().map(decaissementMapper::mapOperationErroneToDto).toList());
        combinedItems.addAll(outstandingPrefinancements.stream().map(decaissementMapper::mapPrefinancementToDto).toList());
        combinedItems.addAll(outstandingDroitsConstate.stream().map(decaissementMapper::mapDroitConstateToDto).toList());
        combinedItems.addAll(outstandingFactoring.stream().map(decaissementMapper::mapFactoringToDto).toList());
        combinedItems.addAll(outstandingCommissions.stream().map(decaissementMapper::mapCommissionToDto).toList());
        combinedItems.addAll(outstandingAgios.stream().map(decaissementMapper::mapAgiosToDto).toList());
        combinedItems.addAll(outstandingPrelevements.stream().map(decaissementMapper::mapPrelevementToDto).toList());
        combinedItems.addAll(outstandingFraisBancaire.stream().map(decaissementMapper::mapFraisBancaireToDto).toList());
        combinedItems.addAll(outstandingImpotsEtTaxes.stream().map(decaissementMapper::mapImpotEtTaxrToDto).toList());
        combinedItems.addAll(outstandingImpayee.stream().map(decaissementMapper::mapImpayeeToDto).toList());

        // Apply the filter using the DTO value
        if (transactionSources != null && !transactionSources.isEmpty()) {
            combinedItems = combinedItems.stream()
                    .filter(item -> transactionSources.contains(item.getSource()))
                    .collect(Collectors.toList());
        }

        if (libelleFilter != null && !libelleFilter.isBlank()) {
            combinedItems = combinedItems.stream()
                    .filter(item -> item.getLibelle() != null && item.getLibelle().toLowerCase().contains(libelleFilter.toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (observationFilter != null && !observationFilter.isBlank()) {
            combinedItems = combinedItems.stream()
                    .filter(item -> item.getObservations() != null && item.getObservations().toLowerCase().contains(observationFilter.toLowerCase()))
                    .collect(Collectors.toList());
        }

        // New Step: Calculate the total debit and credit of the filtered list before pagination
        BigDecimal totalDebit = combinedItems.stream()
                .map(RecaputilatifItem::getDebit)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredit = combinedItems.stream()
                .map(RecaputilatifItem::getCredit)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Step 4: Sort the entire consolidated list based on the Pageable's sort information.
        Comparator<RecaputilatifItem> comparator = getComparatorForSorting(pageable);
        combinedItems.sort(comparator);

        // Step 5: Manually apply pagination to the sorted list.
        int pageSize = pageable.getPageSize();
        int currentPage = pageable.getPageNumber();
        int startItem = currentPage * pageSize;
        List<RecaputilatifItem> pagedItems;

        if (combinedItems.size() < startItem) {
            pagedItems = List.of();
        } else {
            int toIndex = Math.min(startItem + pageSize, combinedItems.size());
            pagedItems = combinedItems.subList(startItem, toIndex);
        }

        // Step 6: Wrap the sublist in a Page object
        Page<RecaputilatifItem> pageResult = new PageImpl<>(pagedItems, pageable, combinedItems.size());

        // Step 7: Create and return the AgregatedRecapDto
        AgregatedRecapDto aggregatedDto = new AgregatedRecapDto();
        aggregatedDto.setPage(pageResult);
        aggregatedDto.setTotalDebit(totalDebit);
        aggregatedDto.setTotalCredit(totalCredit);

        return aggregatedDto;
    }
    public List<RecaputilatifItem> getDecaissementByBanque(BanqueResponse banque) {

        List<RecaputilatifItem> decaissementList= new ArrayList<>();

        //leasing
        Specification<EcheanceLeasing> leasingSpec = Specification.where(EcheanceLeasingSpecification.outstandingUntil(endOfMonth))
                .and(EcheanceLeasingSpecification.byContratBanqueId(banque.id()));
        //CMT
        Specification<EcheanceCMT> cmtSpec = Specification.where(EcheanceCMTSpecification.outstandingUntil(endOfMonth))
                .and(EcheanceCMTSpecification.byCreditBanqueId(banque.id()));

        //moyens de paiement
        Specification<Cheque> chequeSpec = ChequeSpec.encoursDecaissements(banque.id(),endOfMonth);
        Specification<Lcn> lcnSpec = LcnSpecification.encoursDecaissement(banque.id(),endOfMonth);
        Specification<Virement> virementSpec = VirementSpec.encoursDecaissement(banque.id(),endOfMonth);
        Specification<MiseADisposition> madSpec = MiseADispositionSpec.encoursDecaissement(banque.id(),endOfMonth);
        Specification<VirementInternational> virementInternationalSpec=VirementInternationalSpec.encoursDecaissement(banque.id(),endOfMonth);

        //operation erronées
        Specification<OperationErrone> operationErroneSpec = OperationErroneSpec.outstandingUntil(endOfMonth)
                .and(OperationErroneSpec.hasBanque(banque.id()));

        // préfinancement (Remboursement only)
        Specification<Prefinancement> prefinancementSpecification = PrefinancementSpecification.outstandingRemboursementUntil(endOfMonth)
                .and(PrefinancementSpecification.byBanqueId(banque.id()));

        //Droit constatés ( Remboursement only )
        Specification<DroitConstate> droitConstateSpecification = DroitConstateSpec.outstandingRemboursementUntil(endOfMonth)
                .and(DroitConstateSpec.byBanqueId(banque.id()));

        //Factoring ( Remboursement only )
        Specification<Factoring> factoringSpecification = FactoringSpec.outstandingRemboursementUntil(endOfMonth)
                .and(FactoringSpec.byBanqueId(banque.id()));

        //Commissions
        Specification<Commission> commissionSpecification= CommissionSpecifications.outstandingUntil(endOfMonth)
                .and(CommissionSpecifications.hasBanque(banque.id()));
        //Agios
        Specification<Agios> agiosSpecification= AgiosSpec.outstandingUntil(endOfMonth)
                .and(AgiosSpec.hasBanque(banque.id()));

        //prelevement
        Specification<Prelevement> prelevementSpecification= PrelevementSpec.outstandingUntil(endOfMonth)
                .and(PrelevementSpec.hasBanque(banque.id()));


        // frais bancaire
        Specification<FraisBancaire> fraisBancaireSpecification= FraisBancaireSpec.outstandingUntil(endOfMonth)
                .and(FraisBancaireSpec.hasBanque(banque.id()));

        //impot et taxes
        Specification<ImpotEtTaxe> impotEtTaxeSpecification=ImpotEtTaxeSpecifications.outstandingUntil(endOfMonth)
                .and(ImpotEtTaxeSpecifications.hasBanque(banque.id()));

        //Impayée
        Specification<Impayee> impayeeSpecification=ImpayeeSpecifications.outstandingUntil(endOfMonth,Type.DECAISSEMENT)
                .and(ImpayeeSpecifications.hasBanque(banque.id()));

        // Step 2: Fetch all relevant data for the current month from all repositories.
        List<EcheanceLeasing> outstandingLeasingPayments = echeanceLeasingRepository.findAll(leasingSpec);
        List<EcheanceCMT> outstandingCmtPayments = echeanceCMTRepository.findAll(cmtSpec);
        List<Cheque> activeCheques = chequeRepository.findAll(chequeSpec);
        List<Lcn> activeLcns = lcnRepository.findAll(lcnSpec);
        List<Virement> activeVirements = virementRepository.findAll(virementSpec);
        List<MiseADisposition> activeMisesADisposition = miseADispositionRepository.findAll(madSpec);
        List<VirementInternational> activevirementInternationals=virementInternationalRepository.findAll(virementInternationalSpec);
        List<OperationErrone> outstandingOperationsErrones = operationErroneRepository.findAll(operationErroneSpec);
        List<Prefinancement> outstandingPrefinancements = prefinancementRepository.findAll(prefinancementSpecification);
        List<DroitConstate> outstandingDroitsConstate = droitConstateRepository.findAll(droitConstateSpecification);
        List<Factoring> outstandingFactoring = factoringRepository.findAll(factoringSpecification);
        List<Commission> outstandingCommissions=commissionRepository.findAll(commissionSpecification);
        List<Agios> outstandingAgios=agiosRepository.findAll(agiosSpecification);
        List<Prelevement> outstandingPrelevements=prelevementRepository.findAll(prelevementSpecification);
        List<FraisBancaire> outstandingFraisBancaire=fraisBancaireRepository.findAll(fraisBancaireSpecification);
        List<ImpotEtTaxe> outstandingImpotEtTaxes=impotEtTaxeRepository.findAll(impotEtTaxeSpecification);
        List<Impayee> outstandingImpayee=impayeeRepository.findAll(impayeeSpecification);

        // Step 3: Combine all lists and map them to the unified DTO.
        // Add items from the new data sources.
        decaissementList.addAll(outstandingLeasingPayments.stream().map(decaissementMapper::mapLeasingToDto).toList());
        decaissementList.addAll(outstandingCmtPayments.stream().map(decaissementMapper::mapCmtToDto).toList());
        decaissementList.addAll(activeCheques.stream().map(decaissementMapper::mapChequeToDto).toList());
        decaissementList.addAll(activeLcns.stream().map(decaissementMapper::mapLcnToDto).toList());
        decaissementList.addAll(activeVirements.stream().map(decaissementMapper::mapVirementToDto).toList());
        decaissementList.addAll(activeMisesADisposition.stream().map(decaissementMapper::mapMiseADispositionToDto).toList());
        decaissementList.addAll(activevirementInternationals.stream().map(decaissementMapper::mapVirementInternationalToDto).toList());
        decaissementList.addAll(outstandingOperationsErrones.stream().map(decaissementMapper::mapOperationErroneToDto).toList());
        decaissementList.addAll(outstandingPrefinancements.stream().map(decaissementMapper::mapPrefinancementToDto).toList());
        decaissementList.addAll(outstandingDroitsConstate.stream().map(decaissementMapper::mapDroitConstateToDto).toList());
        decaissementList.addAll(outstandingFactoring.stream().map(decaissementMapper::mapFactoringToDto).toList());
        decaissementList.addAll(outstandingCommissions.stream().map(decaissementMapper::mapCommissionToDto).toList());
        decaissementList.addAll(outstandingAgios.stream().map(decaissementMapper::mapAgiosToDto).toList());
        decaissementList.addAll(outstandingPrelevements.stream().map(decaissementMapper::mapPrelevementToDto).toList());
        decaissementList.addAll(outstandingFraisBancaire.stream().map(decaissementMapper::mapFraisBancaireToDto).toList());
        decaissementList.addAll(outstandingImpotEtTaxes.stream().map(decaissementMapper::mapImpotEtTaxrToDto).toList());
        decaissementList.addAll(outstandingImpayee.stream().map(decaissementMapper::mapImpayeeToDto).toList());

        return decaissementList;
    }


    public RecaputilatifGlobal getRecapitulatifGlobal(Long banqueId) {
        // Récupérer la banque
        BanqueResponse banque = this.banqueService.getBanqueById(banqueId);

        BigDecimal plafondFc=banqueService.getPlafondFC(banqueId);
        // Récupérer les décaissements (débits)
        List<RecaputilatifItem> decaissements = getDecaissementByBanque(banque);

        // Récupérer les encaissements (crédits)
        List<RecaputilatifItem> encaissements = getEncaissementByBanque(banque);



        // Créer et retourner le DTO global
        // Le constructeur calculera automatiquement les totaux et le solde fin de mois
        return new RecaputilatifGlobal(
                banque.id(),
                banque.nom(),
                banque.soldeActuel(),
                plafondFc.negate(),
                encaissements,
                decaissements
        );
    }
    public List<RecaputilatifGlobal> getAllRecapitulatifsGlobaux() {
        // 1. Récupérer la liste de tous les IDs de banque disponibles.
        List<Long> allBankIds = this.banqueService.getAllBanqueIds();

        // 2. Pour chaque ID, appeler la méthode existante getRecapitulatifGlobal.
        return allBankIds.stream()
                .map(this::getRecapitulatifGlobal)
                .collect(Collectors.toList());

    }

    private Comparator<RecaputilatifItem> getComparatorForSorting(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            // Default sort if no sort is specified.
            return Comparator.comparing(RecaputilatifItem::getDateEmission);
        }

        return pageable.getSort().stream()
                .map(order -> {
                    Comparator<RecaputilatifItem> comparator = switch (order.getProperty()) {
                        case "dateEmission" -> Comparator.comparing(RecaputilatifItem::getDateEmission);
                        case "libelle" -> Comparator.comparing(RecaputilatifItem::getLibelle);
                        case "debit" -> Comparator.comparing(RecaputilatifItem::getDebit);
                        default ->
                            // Fallback to the default sort to avoid a NullPointerException.
                                Comparator.comparing(RecaputilatifItem::getDateEmission);
                    };
                    return order.isDescending() ? comparator.reversed() : comparator;
                })
                .reduce(Comparator::thenComparing)
                .orElse(Comparator.comparing(RecaputilatifItem::getDateEmission));
    }

}
