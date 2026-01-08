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
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.Bank.services.BanqueService;
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
import staport.finance.gBanque.interets.entities.Interet;
import staport.finance.gBanque.interets.repository.InteretRepository;
import staport.finance.gBanque.interets.specs.InteretSpec;
import staport.finance.gBanque.moyensDePaiements.lcn.entities.Lcn;
import staport.finance.gBanque.moyensDePaiements.lcn.repositories.LcnRepository;
import staport.finance.gBanque.moyensDePaiements.lcn.specs.LcnSpecification;
import staport.finance.gBanque.leasing.echeancesLeasing.entities.EcheanceLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.repositories.EcheanceLeasingRepository;
import staport.finance.gBanque.leasing.echeancesLeasing.specification.EcheanceLeasingSpecification;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.entities.MiseADisposition;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.repositories.MiseADispositionRepository;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.specs.MiseADispositionSpec;
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
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.moyensDePaiements.virements.entities.Virement;
import staport.finance.gBanque.moyensDePaiements.virements.repositories.VirementRepository;
import staport.finance.gBanque.moyensDePaiements.virements.specs.VirementSpec;

import java.math.BigDecimal;
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
    private final BanqueRepository banqueRepository;
    private final BanqueService banqueService;

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
    private final InteretRepository interetRepository;
    private final FactoringRepository factoringRepository;
    private final ImpotEtTaxeRepository impotEtTaxeRepository;

    public RecapitulatifService(
            EcheanceLeasingRepository echeanceLeasingRepository,
            EcheanceCMTRepository echeanceCMTRepository,
            RecapitulatifDecaissementMapper mapper, RecapitulatifEncaissementMapper encaissementMapper, BanqueRepository banqueRepository, BanqueService banqueService, ChequeRepository chequeRepository, LcnRepository lcnRepository, VirementRepository virementRepository, MiseADispositionRepository miseADispositionRepository, PrefinancementRepository prefinancementRepository, DroitConstateRepository droitConstateRepository, DecompteRepository decompteRepository, CommisionRepository commissionRepository, AgiosRepository agiosRepository, PrelevementRepository prelevementRepository, FraisBancaireRepository fraisBancaireRepository, InteretRepository interetRepository, FactoringRepository factoringRepository, ImpotEtTaxeRepository impotEtTaxeRepository) {
        this.echeanceLeasingRepository = echeanceLeasingRepository;
        this.echeanceCMTRepository = echeanceCMTRepository;
        this.decaissementMapper = mapper;
        this.encaissementMapper = encaissementMapper;
        this.banqueRepository = banqueRepository;
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
        this.interetRepository = interetRepository;
        this.factoringRepository = factoringRepository;
        this.impotEtTaxeRepository = impotEtTaxeRepository;
    }

    /**
     * Retrieves a pageable and sortable list of outstanding (Prévu) disbursements
     * for the current month.
     *
     * @param searchDto The id of the bank to filter by.
     * @param pageable A Spring data Pageable object for pagination and sorting.
     * @return A consolidated Page of disbursement items.
     */
    public AgregatedRecapDto getRecapitulatifDecaissementForCurrentMonthForGivenBanque(Pageable pageable, DecaissementSearchDto searchDto) {

        // Retrieve parameters from the DTO
        Long banqueId = searchDto.getBanqueId();
        List<TransactionSource> transactionSources = searchDto.getTransactionSources();
        String libelleFilter = searchDto.getLibelle();
        String observationFilter = searchDto.getObservation();

        // Step 1: Create specifications for each entity type, filtering by bank and current month.

        //leasing
        Specification<EcheanceLeasing> leasingSpec = Specification.where(EcheanceLeasingSpecification.outstandingUntil())
                .and(EcheanceLeasingSpecification.byContratBanqueId(banqueId));
        //CMT
        Specification<EcheanceCMT> cmtSpec = Specification.where(EcheanceCMTSpecification.outstandingUntil())
                .and(EcheanceCMTSpecification.byCreditBanqueId(banqueId));

        //moyens de paiement
        Specification<Cheque> chequeSpec = ChequeSpec.encoursDecaissements(banqueId);
        Specification<Lcn> lcnSpec = LcnSpecification.encoursDecaissement(banqueId);
        Specification<Virement> virementSpec = VirementSpec.encoursDecaissement(banqueId);
        Specification<MiseADisposition> madSpec = MiseADispositionSpec.encoursDecaissementForMiseAdispo(banqueId);

        // préfinancement (Remboursement only)
        Specification<Prefinancement> prefinancementSpecification = PrefinancementSpecification.outstandingRemboursementForCurrentMonth()
                .and(PrefinancementSpecification.byBanqueId(banqueId));

        //Droit constatés ( Remboursement only )
        Specification<DroitConstate> droitConstateSpecification = DroitConstateSpec.outstandingRemboursementForCurrentMonth()
                .and(DroitConstateSpec.byBanqueId(banqueId));

        //Factoring ( Remboursement only )
        Specification<Factoring> factoringSpecification = FactoringSpec.outstandingRemboursementForCurrentMonth()
                .and(FactoringSpec.byBanqueId(banqueId));

        //Commissions
        Specification<Commission> commissionSpecification= CommissionSpecifications.outstandingCommissions()
                .and(CommissionSpecifications.hasBanque(banqueId));

        //impots et taxes
        Specification<ImpotEtTaxe> impotEtTaxeSpecification= ImpotEtTaxeSpecifications.outstandingImpotEtTaxes()
                .and(ImpotEtTaxeSpecifications.hasBanque(banqueId));

        //Agios
        Specification<Agios> agiosSpecification= AgiosSpec.outstandingAgios()
                .and(AgiosSpec.hasBanque(banqueId));

        //prelevements
        Specification<Prelevement> prelevementSpecification= PrelevementSpec.outstandingPrelevement()
                .and(PrelevementSpec.hasBanque(banqueId));

        // frais bancaire
        Specification<FraisBancaire> fraisBancaireSpecification= FraisBancaireSpec.outstandingFraisBancaires()
                .and(FraisBancaireSpec.hasBanque(banqueId));

        // interet
        Specification<Interet> interetSpecification= InteretSpec.outstandingInterests()
                .and(InteretSpec.hasBanque(banqueId));

        // Step 2: Fetch all relevant data for the current month from all repositories.
        List<EcheanceLeasing> outstandingLeasingPayments = echeanceLeasingRepository.findAll(leasingSpec);
        List<EcheanceCMT> outstandingCmtPayments = echeanceCMTRepository.findAll(cmtSpec);
        List<Cheque> activeCheques = chequeRepository.findAll(chequeSpec);
        List<Lcn> activeLcns = lcnRepository.findAll(lcnSpec);
        List<Virement> activeVirements = virementRepository.findAll(virementSpec);
        List<MiseADisposition> activeMisesADisposition = miseADispositionRepository.findAll(madSpec);
        List<Prefinancement> outstandingPrefinancements = prefinancementRepository.findAll(prefinancementSpecification);
        List<DroitConstate> outstandingDroitsConstate = droitConstateRepository.findAll(droitConstateSpecification);
        List<Factoring> outstandingFactoring = factoringRepository.findAll(factoringSpecification);
        List<Commission> outstandingCommissions=commissionRepository.findAll(commissionSpecification);
        List<Agios> outstandingAgios=agiosRepository.findAll(agiosSpecification);
        List<Prelevement> outstandingPrelevements=prelevementRepository.findAll(prelevementSpecification);
        List<FraisBancaire> outstandingFraisBancaire=fraisBancaireRepository.findAll(fraisBancaireSpecification);
        List<Interet> outstandingInterets=interetRepository.findAll(interetSpecification);
        List<ImpotEtTaxe> outstandingImpotsEtTaxes=impotEtTaxeRepository.findAll(impotEtTaxeSpecification);

        // Step 3: Combine all lists and map them to the unified DTO.
        List<RecaputilatifItem> combinedItems = new ArrayList<>();
        // Add items from the new data sources.
        combinedItems.addAll(outstandingLeasingPayments.stream().map(decaissementMapper::mapLeasingToDto).toList());
        combinedItems.addAll(outstandingCmtPayments.stream().map(decaissementMapper::mapCmtToDto).toList());
        combinedItems.addAll(activeCheques.stream().map(decaissementMapper::mapChequeToDto).toList());
        combinedItems.addAll(activeLcns.stream().map(decaissementMapper::mapLcnToDto).toList());
        combinedItems.addAll(activeVirements.stream().map(decaissementMapper::mapVirementToDto).toList());
        combinedItems.addAll(activeMisesADisposition.stream().map(decaissementMapper::mapMiseADispositionToDto).toList());
        combinedItems.addAll(outstandingPrefinancements.stream().map(decaissementMapper::mapPrefinancementToDto).toList());
        combinedItems.addAll(outstandingDroitsConstate.stream().map(decaissementMapper::mapDroitConstateToDto).toList());
        combinedItems.addAll(outstandingFactoring.stream().map(decaissementMapper::mapFactoringToDto).toList());
        combinedItems.addAll(outstandingCommissions.stream().map(decaissementMapper::mapCommissionToDto).toList());
        combinedItems.addAll(outstandingAgios.stream().map(decaissementMapper::mapAgiosToDto).toList());
        combinedItems.addAll(outstandingPrelevements.stream().map(decaissementMapper::mapPrelevementToDto).toList());
        combinedItems.addAll(outstandingFraisBancaire.stream().map(decaissementMapper::mapFraisBancaireToDto).toList());
        combinedItems.addAll(outstandingInterets.stream().map(decaissementMapper::mapInteretToDto).toList());
        combinedItems.addAll(outstandingImpotsEtTaxes.stream().map(decaissementMapper::mapImpotEtTaxrToDto).toList());
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

    public AgregatedRecapDto getRecapitulatifEncaissementForCurrentMonthForGivenBanque(Pageable pageable, DecaissementSearchDto searchDto) {

        // Retrieve parameters from the DTO
        Long banqueId = searchDto.getBanqueId();
        Banque banque = banqueRepository.findById(banqueId).orElseThrow(() -> new IllegalArgumentException("Banque with id " + banqueId + " not found."));
        List<TransactionSource> transactionSources = searchDto.getTransactionSources();
        String libelleFilter = searchDto.getLibelle();
        String observationFilter = searchDto.getObservation();

        // Step 1: Create specifications for each entity type, filtering by bank and current month.

        // préfinancement (Déblocage only)
        Specification<Prefinancement> prefinancementSpecification = PrefinancementSpecification.outstandingDeblocageForCurrentMonth()
                .and(PrefinancementSpecification.byBanqueId(banqueId));

        // Droit Constaté ( Déblocage only )
        Specification<DroitConstate> droitConstateSpecification = DroitConstateSpec.outstandingDeblocageForCurrentMonth()
                .and(DroitConstateSpec.byBanqueId(banqueId));

        // Factoring ( Déblocage only )
        Specification<Factoring> factoringSpecification = FactoringSpec.outstandingDeblocageForCurrentMonth()
                .and(FactoringSpec.byBanqueId(banqueId));

        //décomptes
        Specification<Decompte> decompteSpecification = DecompteSpec.outstandingDecomptesForCurrentMonth()
                .and(DecompteSpec.byBanqueId(banqueId));

        //Moyens de paiement
        Specification<Virement> virementSpec = VirementSpec.encoursEncaissement(banque.getNom());
        Specification<Cheque> chequeSpec = ChequeSpec.encoursEncaissements(banque.getId());
        Specification<Lcn> lcnSpec = LcnSpecification.encoursEncaissement(banqueId);


        // Step 2: Fetch all relevant data for the current month from all repositories.
        List<Prefinancement> outstandingPrefinancements = prefinancementRepository.findAll(prefinancementSpecification);
        List<DroitConstate> outstandingDroitsConstate = droitConstateRepository.findAll(droitConstateSpecification);
        List<Factoring> outstandingFactoring = factoringRepository.findAll(factoringSpecification);
        List<Decompte> outstandingDecomptes = decompteRepository.findAll(decompteSpecification);

        List<Virement> outstandingVirements = virementRepository.findAll(virementSpec);
        List<Cheque> outstandingCheques = chequeRepository.findAll(chequeSpec);
        List<Lcn> outstandingLcns = lcnRepository.findAll(lcnSpec);


        // Step 3: Combine all lists and map them to the unified DTO.
        List<RecaputilatifItem> combinedItems = new ArrayList<>();
        combinedItems.addAll(outstandingPrefinancements.stream().map(encaissementMapper::mapPrefinancementToDto).toList());
        combinedItems.addAll(outstandingDroitsConstate.stream().map(encaissementMapper::mapDroitConstateToDto).toList());
        combinedItems.addAll(outstandingFactoring.stream().map(encaissementMapper::mapFactoringToDto).toList());
        combinedItems.addAll(outstandingDecomptes.stream().map(encaissementMapper::mapDecompteToDto).toList());
        combinedItems.addAll(outstandingVirements.stream().map(encaissementMapper::mapVirementToDto).toList());
        combinedItems.addAll(outstandingCheques.stream().map(encaissementMapper::mapChequeToDto).toList());
        combinedItems.addAll(outstandingLcns.stream().map(encaissementMapper::mapLcnToDto).toList());

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

    public RecaputilatifGlobal getRecapitulatifGlobal(Long banqueId) {
        // Récupérer la banque
        Banque banque = banqueRepository.findById(banqueId)
                .orElseThrow(() -> new IllegalArgumentException("Banque avec l'ID " + banqueId + " introuvable."));

        BigDecimal plafondFc=banqueService.getPlafoncFC(banqueId);
        // Récupérer les décaissements (débits)
        List<RecaputilatifItem> decaissements = getDecaissementByBanque(banque);

        // Récupérer les encaissements (crédits)
        List<RecaputilatifItem> encaissements = getEncaissementByBanque(banque);



        // Créer et retourner le DTO global
        // Le constructeur calculera automatiquement les totaux et le solde fin de mois
        return new RecaputilatifGlobal(
                banque.getId(),
                banque.getNom(),
                banque.getSoldeActuel(),
                plafondFc.negate(),
                encaissements,
                decaissements
        );
    }
    public List<RecaputilatifItem> getDecaissementByBanque(Banque banque) {

        List<RecaputilatifItem> decaissementList=new ArrayList<RecaputilatifItem>();
        //leasing
        Specification<EcheanceLeasing> leasingSpec = Specification.where(EcheanceLeasingSpecification.outstandingUntil())
                .and(EcheanceLeasingSpecification.byContratBanqueId(banque.getId()));
        //CMT
        Specification<EcheanceCMT> cmtSpec = Specification.where(EcheanceCMTSpecification.outstandingUntil())
                .and(EcheanceCMTSpecification.byCreditBanqueId(banque.getId()));

        //moyens de paiement
        Specification<Cheque> chequeSpec = ChequeSpec.encoursDecaissements(banque.getId());
        Specification<Lcn> lcnSpec = LcnSpecification.encoursDecaissement(banque.getId());
        Specification<Virement> virementSpec = VirementSpec.encoursDecaissement(banque.getId());
        Specification<MiseADisposition> madSpec = MiseADispositionSpec.encoursDecaissementForMiseAdispo(banque.getId());

        // préfinancement (Remboursement only)
        Specification<Prefinancement> prefinancementSpecification = PrefinancementSpecification.outstandingRemboursementForCurrentMonth()
                .and(PrefinancementSpecification.byBanqueId(banque.getId()));

        //Droit constatés ( Remboursement only )
        Specification<DroitConstate> droitConstateSpecification = DroitConstateSpec.outstandingRemboursementForCurrentMonth()
                .and(DroitConstateSpec.byBanqueId(banque.getId()));

        //Factoring ( Remboursement only )
        Specification<Factoring> factoringSpecification = FactoringSpec.outstandingRemboursementForCurrentMonth()
                .and(FactoringSpec.byBanqueId(banque.getId()));

        //Commissions
        Specification<Commission> commissionSpecification= CommissionSpecifications.outstandingCommissions()
                .and(CommissionSpecifications.hasBanque(banque.getId()));
        //Agios
        Specification<Agios> agiosSpecification= AgiosSpec.outstandingAgios()
                .and(AgiosSpec.hasBanque(banque.getId()));

        Specification<Prelevement> prelevementSpecification= PrelevementSpec.outstandingPrelevement()
                .and(PrelevementSpec.hasBanque(banque.getId()));

        // frais bancaire
        Specification<FraisBancaire> fraisBancaireSpecification= FraisBancaireSpec.outstandingFraisBancaires()
                .and(FraisBancaireSpec.hasBanque(banque.getId()));

        // interet
        Specification<Interet> interetSpecification= InteretSpec.outstandingInterests()
                .and(InteretSpec.hasBanque(banque.getId()));

        //impot et taxes
        Specification<ImpotEtTaxe> impotEtTaxeSpecification=ImpotEtTaxeSpecifications.outstandingImpotEtTaxes()
                .and(ImpotEtTaxeSpecifications.hasBanque(banque.getId()));

        // Step 2: Fetch all relevant data for the current month from all repositories.
        List<EcheanceLeasing> outstandingLeasingPayments = echeanceLeasingRepository.findAll(leasingSpec);
        List<EcheanceCMT> outstandingCmtPayments = echeanceCMTRepository.findAll(cmtSpec);
        List<Cheque> activeCheques = chequeRepository.findAll(chequeSpec);
        List<Lcn> activeLcns = lcnRepository.findAll(lcnSpec);
        List<Virement> activeVirements = virementRepository.findAll(virementSpec);
        List<MiseADisposition> activeMisesADisposition = miseADispositionRepository.findAll(madSpec);
        List<Prefinancement> outstandingPrefinancements = prefinancementRepository.findAll(prefinancementSpecification);
        List<DroitConstate> outstandingDroitsConstate = droitConstateRepository.findAll(droitConstateSpecification);
        List<Factoring> outstandingFactoring = factoringRepository.findAll(factoringSpecification);
        List<Commission> outstandingCommissions=commissionRepository.findAll(commissionSpecification);
        List<Agios> outstandingAgios=agiosRepository.findAll(agiosSpecification);
        List<Prelevement> outstandingPrelevements=prelevementRepository.findAll(prelevementSpecification);
        List<FraisBancaire> outstandingFraisBancaire=fraisBancaireRepository.findAll(fraisBancaireSpecification);
        List<Interet> outstandingInterets=interetRepository.findAll(interetSpecification);
        List<ImpotEtTaxe> outstandingImpotEtTaxes=impotEtTaxeRepository.findAll(impotEtTaxeSpecification);

        // Step 3: Combine all lists and map them to the unified DTO.
        // Add items from the new data sources.
        decaissementList.addAll(outstandingLeasingPayments.stream().map(decaissementMapper::mapLeasingToDto).toList());
        decaissementList.addAll(outstandingCmtPayments.stream().map(decaissementMapper::mapCmtToDto).toList());
        decaissementList.addAll(activeCheques.stream().map(decaissementMapper::mapChequeToDto).toList());
        decaissementList.addAll(activeLcns.stream().map(decaissementMapper::mapLcnToDto).toList());
        decaissementList.addAll(activeVirements.stream().map(decaissementMapper::mapVirementToDto).toList());
        decaissementList.addAll(activeMisesADisposition.stream().map(decaissementMapper::mapMiseADispositionToDto).toList());
        decaissementList.addAll(outstandingPrefinancements.stream().map(decaissementMapper::mapPrefinancementToDto).toList());
        decaissementList.addAll(outstandingDroitsConstate.stream().map(decaissementMapper::mapDroitConstateToDto).toList());
        decaissementList.addAll(outstandingFactoring.stream().map(decaissementMapper::mapFactoringToDto).toList());
        decaissementList.addAll(outstandingCommissions.stream().map(decaissementMapper::mapCommissionToDto).toList());
        decaissementList.addAll(outstandingAgios.stream().map(decaissementMapper::mapAgiosToDto).toList());
        decaissementList.addAll(outstandingPrelevements.stream().map(decaissementMapper::mapPrelevementToDto).toList());
        decaissementList.addAll(outstandingFraisBancaire.stream().map(decaissementMapper::mapFraisBancaireToDto).toList());
        decaissementList.addAll(outstandingInterets.stream().map(decaissementMapper::mapInteretToDto).toList());
        decaissementList.addAll(outstandingImpotEtTaxes.stream().map(decaissementMapper::mapImpotEtTaxrToDto).toList());
        return decaissementList;
    }

    public List<RecaputilatifItem> getEncaissementByBanque(Banque banque) {

        List<RecaputilatifItem> encaissements=new ArrayList<RecaputilatifItem>();

        // préfinancement (Déblocage only)
        Specification<Prefinancement> prefinancementSpecification = PrefinancementSpecification.outstandingDeblocageForCurrentMonth()
                .and(PrefinancementSpecification.byBanqueId(banque.getId()));

        // Droit Constaté ( Déblocage only )
        Specification<DroitConstate> droitConstateSpecification = DroitConstateSpec.outstandingDeblocageForCurrentMonth()
                .and(DroitConstateSpec.byBanqueId(banque.getId()));

        // Factoring ( Déblocage only )
        Specification<Factoring> factoringSpecification = FactoringSpec.outstandingDeblocageForCurrentMonth()
                .and(FactoringSpec.byBanqueId(banque.getId()));

        //décomptes
        Specification<Decompte> decompteSpecification = DecompteSpec.outstandingDecomptesForCurrentMonth()
                .and(DecompteSpec.byBanqueId(banque.getId()));

        //Moyens de paiement
        Specification<Virement> virementSpec = VirementSpec.encoursEncaissement(banque.getNom());
        Specification<Cheque> chequeSpec = ChequeSpec.encoursEncaissements(banque.getId());
        Specification<Lcn> lcnSpec = LcnSpecification.encoursEncaissement(banque.getId());


        // Step 2: Fetch all relevant data for the current month from all repositories.
        List<Prefinancement> outstandingPrefinancements = prefinancementRepository.findAll(prefinancementSpecification);
        List<DroitConstate> outstandingDroitsConstate = droitConstateRepository.findAll(droitConstateSpecification);
        List<Factoring> outstandingFactoring = factoringRepository.findAll(factoringSpecification);
        List<Decompte> outstandingDecomptes = decompteRepository.findAll(decompteSpecification);

        List<Virement> outstandingVirements = virementRepository.findAll(virementSpec);
        List<Cheque> outstandingCheques = chequeRepository.findAll(chequeSpec);
        List<Lcn> outstandingLcns = lcnRepository.findAll(lcnSpec);


        encaissements.addAll(outstandingPrefinancements.stream().map(encaissementMapper::mapPrefinancementToDto).toList());
        encaissements.addAll(outstandingDroitsConstate.stream().map(encaissementMapper::mapDroitConstateToDto).toList());
        encaissements.addAll(outstandingFactoring.stream().map(encaissementMapper::mapFactoringToDto).toList());
        encaissements.addAll(outstandingDecomptes.stream().map(encaissementMapper::mapDecompteToDto).toList());
        encaissements.addAll(outstandingVirements.stream().map(encaissementMapper::mapVirementToDto).toList());
        encaissements.addAll(outstandingCheques.stream().map(encaissementMapper::mapChequeToDto).toList());
        encaissements.addAll(outstandingLcns.stream().map(encaissementMapper::mapLcnToDto).toList());

        return encaissements;
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


    public List<RecaputilatifGlobal> getAllRecapitulatifsGlobaux() {
        // 1. Récupérer la liste de tous les IDs de banque disponibles.
        List<Long> allBankIds = banqueRepository.findAll().stream().map(Banque::getId).toList();

        // 2. Pour chaque ID, appeler la méthode existante getRecapitulatifGlobal.
        return allBankIds.stream()
                .map(this::getRecapitulatifGlobal)
                .collect(Collectors.toList());

    }
}
