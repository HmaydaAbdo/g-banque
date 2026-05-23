package staport.finance.gBanque.rapprochement.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.services.IBanqueService;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.rapprochement.dtos.RapprochementAggregates;
import staport.finance.gBanque.rapprochement.dtos.RapprochementResponseDto;
import staport.finance.gBanque.rapprochement.dtos.RapprochementSearchDto;
import staport.finance.gBanque.rapprochement.dtos.ReportDto;
import staport.finance.gBanque.rapprochement.entities.Rapprochement;
import staport.finance.gBanque.rapprochement.enums.StatutRapprochement;
import staport.finance.gBanque.rapprochement.repository.RapprochementRepository;
import staport.finance.gBanque.rapprochement.mapper.RapprochementMapper;
import staport.finance.gBanque.rapprochement.specs.RapprochementSpecifications;
import staport.finance.gBanque.reports.enums.ReportType;
import staport.finance.gBanque.reports.service.StaportPdfReportService;
import staport.finance.gBanque.transactions.dtos.TransactionResponseDto;
import staport.finance.gBanque.transactions.dtos.TransactionSearchDto;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.repositories.TransactionRepository;
import staport.finance.gBanque.transactions.mapper.TransactionMapper;
import staport.finance.gBanque.transactions.spec.TransactionSpecifications;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class RapprochementService {

    private final RapprochementRepository rapprochementRepository;
    private final TransactionRepository transactionRepository;
    private final IBanqueService banqueService;
    private final RapprochementMapper rapprochementMapper;
    private final StaportPdfReportService pdfReportService;

    public RapprochementService(
            RapprochementRepository rapprochementRepository,
            TransactionRepository transactionRepository,
            IBanqueService banqueService,
            RapprochementMapper rapprochementMapper,
            StaportPdfReportService pdfReportService
    ) {
        this.rapprochementRepository = rapprochementRepository;
        this.transactionRepository   = transactionRepository;
        this.banqueService           = banqueService;
        this.rapprochementMapper     = rapprochementMapper;
        this.pdfReportService        = pdfReportService;
    }

    // ── Search (paginated list) ───────────────────────────────────────────────

    /**
     * Paginated search. Aggregates are batch-fetched in a single extra query
     * to avoid N+1 — one DB call for the page, one for all aggregates on that page.
     */
    @Transactional(readOnly = true)
    public Page<RapprochementResponseDto> searchRapprochements(RapprochementSearchDto searchDto, Pageable pageable) {

        Specification<Rapprochement> spec = RapprochementSpecifications.bySearchDto(searchDto);
        Page<Rapprochement> page = rapprochementRepository.findAll(spec, pageable);

        if (page.isEmpty()) {
            return page.map(r -> rapprochementMapper.toResponseDto(r, RapprochementAggregates.EMPTY));
        }

        // Batch-fetch aggregates for all rapprochements on this page — single query
        List<Long> ids = page.getContent().stream().map(Rapprochement::getId).toList();
        Map<Long, RapprochementAggregates> aggMap = buildAggregatesMap(ids);

        return page.map(r -> rapprochementMapper.toResponseDto(
                r, aggMap.getOrDefault(r.getId(), RapprochementAggregates.EMPTY)
        ));
    }

    // ── Detail ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public RapprochementResponseDto getRapprochementById(Long id, TransactionSearchDto searchDto, Pageable pageable) {

        Rapprochement rapprochement = findOrThrow(id);

        // Build transaction filter specs
        List<Specification<Transaction>> specs = new ArrayList<>();
        specs.add(TransactionSpecifications.hasRapprochementId(id));

        if (searchDto.getLibelle() != null && !searchDto.getLibelle().isEmpty()) {
            specs.add(TransactionSpecifications.libelleContains(searchDto.getLibelle()));
        }
        if (searchDto.getObservation() != null && !searchDto.getObservation().isEmpty()) {
            specs.add(TransactionSpecifications.observationContains(searchDto.getObservation()));
        }
        if (searchDto.getMontant() != null) {
            specs.add(TransactionSpecifications.hasMontant(searchDto.getMontant()));
        }
        if (searchDto.getDateOperationDebut() != null || searchDto.getDateOperationFin() != null) {
            specs.add(TransactionSpecifications.hasDateOperationBetween(
                    searchDto.getDateOperationDebut(), searchDto.getDateOperationFin()));
        }
        if (searchDto.getTransactionSources() != null && !searchDto.getTransactionSources().isEmpty()) {
            specs.add(TransactionSpecifications.hasTransactionSources(searchDto.getTransactionSources()));
        }

        Page<Transaction> transactionPage = transactionRepository.findAll(
                TransactionSpecifications.withFilters(specs), pageable);

        Page<TransactionResponseDto> transactionDtos = transactionPage.map(TransactionMapper::toDto);

        // Single aggregate query for this rapprochement
        RapprochementAggregates agg = fetchAggregates(id);

        RapprochementResponseDto dto = rapprochementMapper.toResponseDto(rapprochement, agg);
        dto.setTransactions(transactionDtos);
        return dto;
    }

    // ── Report data (PDF) ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ReportDto getReportDataById(Long rapprochementId) {

        Rapprochement rapprochement = findOrThrow(rapprochementId);

        List<Transaction> allTransactions = transactionRepository.findAll(
                TransactionSpecifications.hasRapprochementId(rapprochementId));

        List<TransactionResponseDto> transactionDtos = allTransactions.stream()
                .map(TransactionMapper::toDto)
                .sorted(Comparator.comparing(TransactionResponseDto::getDateOperations))
                .toList();

        // Single aggregate query — no Java stream summation
        RapprochementAggregates agg = fetchAggregates(rapprochementId);

        ReportDto dto = rapprochementMapper.toReportDto(rapprochement, agg);
        dto.setTransactions(transactionDtos);
        return dto;
    }

    // ── Clôture ───────────────────────────────────────────────────────────────

    @Transactional
    public RapprochementResponseDto cloturerRapprochement(Long banqueId) {

        Rapprochement rapprochement = rapprochementRepository
                .findByBanqueIdAndStatut(banqueId, StatutRapprochement.EN_COURS)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucun rapprochement EN_COURS trouvé pour la banque id: " + banqueId));

        // Compute aggregates via a single SQL call — no List<Transaction> loaded in memory
        RapprochementAggregates agg = fetchAggregates(rapprochement.getId());
        BigDecimal soldeFin = agg.soldeFin(rapprochement.getSoldeDebut());

        // Close current rapprochement — only the statut changes on the entity
        rapprochement.setStatut(StatutRapprochement.CLOTURE_DEFINITIVE);
        Rapprochement rapprochementCloture = rapprochementRepository.save(rapprochement);

        // Open the next month's rapprochement, seeded with the computed closing balance
        Rapprochement next = new Rapprochement();
        next.setBanque(banqueService.findOrThrow(banqueId));
        next.setDateRapprochement(rapprochement.getDateRapprochement().plusMonths(1));
        next.setSoldeDebut(soldeFin);
        next.setStatut(StatutRapprochement.EN_COURS);
        rapprochementRepository.save(next);

        return rapprochementMapper.toResponseDto(rapprochementCloture, agg);
    }

    // ── PDF generation ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public byte[] generateRapprochementPdf(Long rapprochementId, ReportType reportType) {
        ReportDto reportDto = getReportDataById(rapprochementId);
        try {
            return pdfReportService.generateRapprochementReport(reportDto, reportType);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Erreur lors de la génération du rapport PDF pour le rapprochement ID: " + rapprochementId, e);
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private Rapprochement findOrThrow(Long id) {
        return rapprochementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rapprochement not found with id: " + id));
    }

    /**
     * Fetches aggregates for a single rapprochement.
     * The query always returns exactly one row (COALESCE ensures this even with no transactions).
     * Falls back to EMPTY as a safety net only.
     */
    private RapprochementAggregates fetchAggregates(Long rapprochementId) {
        List<Object[]> rows = transactionRepository.computeAggregatesForRapprochement(rapprochementId);
        return rows.isEmpty() ? RapprochementAggregates.EMPTY : RapprochementAggregates.fromRow(rows.get(0));
    }

    /**
     * Batch-fetches aggregates for a list of rapprochement IDs.
     * Returns a Map keyed by rapprochementId — missing entries mean zero transactions.
     */
    private Map<Long, RapprochementAggregates> buildAggregatesMap(List<Long> ids) {
        return transactionRepository
                .computeAggregatesByRapprochementIds(ids)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> new RapprochementAggregates(
                                (BigDecimal) row[1],
                                (BigDecimal) row[2],
                                (BigDecimal) row[3],
                                (BigDecimal) row[4]
                        )
                ));
    }
}
