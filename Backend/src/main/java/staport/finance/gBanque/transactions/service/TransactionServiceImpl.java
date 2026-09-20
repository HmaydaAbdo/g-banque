package staport.finance.gBanque.transactions.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.reports.service.StaportPdfReportService;
import staport.finance.gBanque.audit.AuditOperation;
import staport.finance.gBanque.audit.AuditableAction;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.rapprochement.enums.StatutRapprochement;
import staport.finance.gBanque.validation.services.handlers.TransactionSyncHandler;
import staport.finance.gBanque.validation.utils.TvaResolver;
import staport.finance.gBanque.transactions.dtos.AgreratedTransactionResponseDto;
import staport.finance.gBanque.transactions.dtos.TransactionAggregatesDto;
import staport.finance.gBanque.transactions.dtos.TransactionResponseDto;
import staport.finance.gBanque.transactions.dtos.TransactionSearchDto;
import staport.finance.gBanque.transactions.dtos.TransactionUpdateRequestDto;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.transactions.mapper.TransactionMapper;
import staport.finance.gBanque.transactions.repositories.TransactionRepository;

import java.awt.Color;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static staport.finance.gBanque.transactions.spec.TransactionSpecifications.*;

/**
 * Implémentation de la façade publique du domaine Transaction.
 *
 * <p>Dispatch de synchronisation ({@code updateTransactionById}) vers les
 * {@code IXxxValidationService} des modules source (architecture.md — Règle 2).
 */
@Service
public class TransactionServiceImpl implements ITransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionServiceImpl.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Color MED_BLUE = Color.decode("#2563eb");
    private static final Color LIGHT_BLUE = Color.decode("#dbeafe");
    private static final Color ROW_ALT = Color.decode("#f8fafc");
    private static final Color BORDER_GRAY = Color.decode("#e5e7eb");
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final StaportPdfReportService staportPdfReportService;

    /** Un handler par {@link TransactionSource} — voir Javadoc de {@link TransactionSyncHandler}. */
    private final Map<TransactionSource, TransactionSyncHandler> syncHandlerRegistry;

    public TransactionServiceImpl(TransactionRepository transactionRepository,
                              TransactionMapper transactionMapper,
                              StaportPdfReportService staportPdfReportService,
                              List<TransactionSyncHandler> syncHandlers) {
        this.transactionRepository = transactionRepository;
        this.transactionMapper = transactionMapper;
        this.staportPdfReportService = staportPdfReportService;
        this.syncHandlerRegistry = syncHandlers.stream()
                .collect(Collectors.toMap(TransactionSyncHandler::source, h -> h, (a, b) -> {
                    throw new IllegalStateException(
                            "Deux TransactionSyncHandler enregistrés pour la même source: " + a.source());
                }));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponseDto> getTransactionHistory(TransactionSearchDto searchDto, Pageable pageable) {
        log.info("Récupération de l'historique des transactions. Filtres: {}", searchDto);

        Specification<Transaction> combinedSpec = buildHistorySpec(searchDto);
        Page<Transaction> transactions = transactionRepository.findAll(combinedSpec, defaultSort(pageable));

        log.info("Historique récupéré: {} transactions trouvées (Page {}/{})",
                transactions.getTotalElements(), transactions.getNumber(), transactions.getTotalPages());

        return transactions.map(transactionMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportTransactionHistoryPdf(TransactionSearchDto searchDto) {
        log.info("Export PDF de l'historique des transactions. Filtres: {}", searchDto);

        Specification<Transaction> combinedSpec = buildHistorySpec(searchDto);
        List<TransactionResponseDto> transactions = transactionRepository
                .findAll(combinedSpec, Sort.by(Sort.Direction.DESC, "dateEmission")).stream()
                .map(transactionMapper::toDto)
                .toList();

        return generateTransactionHistoryPdf("Historique des Transactions", transactions, buildFilterSummary(searchDto, transactions));
    }

    /**
     * Construit la spécification de recherche partagée par {@link #getTransactionHistory} et
     * {@link #exportTransactionHistoryPdf} — mêmes critères, jamais dupliqués.
     */
    private Specification<Transaction> buildHistorySpec(TransactionSearchDto searchDto) {
        List<Specification<Transaction>> specs = new ArrayList<>();

        if (searchDto.libelle() != null && !searchDto.libelle().isEmpty()) {
            specs.add(libelleContains(searchDto.libelle()));
        }

        if (searchDto.banqueId() != null) {
            specs.add(hasBanqueId(searchDto.banqueId()));
        }

        if (searchDto.transactionSources() != null && !searchDto.transactionSources().isEmpty()) {
            specs.add(hasTransactionSources(searchDto.transactionSources()));
        }

        if (searchDto.statutRapprochement() != null) {
            specs.add(hasRapprochementStatut(searchDto.statutRapprochement()));
        }

        if (searchDto.montant() != null) {
            specs.add(hasMontant(searchDto.montant()));
        }

        if (searchDto.dateEmissionDebut() != null || searchDto.dateEmissionFin() != null) {
            specs.add(hasDateEmissionBetween(searchDto.dateEmissionDebut(), searchDto.dateEmissionFin()));
        }

        if (searchDto.dateOperationDebut() != null || searchDto.dateOperationFin() != null) {
            specs.add(hasDateOperationBetween(searchDto.dateOperationDebut(), searchDto.dateOperationFin()));
        }

        if (searchDto.dateValeurDebut() != null || searchDto.dateValeurFin() != null) {
            specs.add(hasDateValeurBetween(searchDto.dateValeurDebut(), searchDto.dateValeurFin()));
        }

        return withFilters(specs);
    }

    @Override
    @Transactional(readOnly = true)
    public AgreratedTransactionResponseDto getTransactionsByBanque(Long banqueId, TransactionSearchDto searchDto, Pageable pageable) {
        log.info("Calcul des agrégations et récupération des transactions pour la banque ID: {}", banqueId);

        Specification<Transaction> combinedSpec = buildBankTransactionsSpec(banqueId, searchDto);

        // 1. Agrégats via JPQL (SUM en base, pas de List<Transaction> en mémoire)
        TransactionAggregatesDto agg;

        if (searchDto.rapprochementId() != null) {
            // Cas nominal : agrégats pour un rapprochement précis via JPQL optimisé
            agg = computeAggregatesForRapprochement(searchDto.rapprochementId());
        } else {
            // Fallback : comportement d'origine (agrégats en mémoire sur EN_COURS)
            List<Transaction> allFilteredTransactions = transactionRepository.findAll(combinedSpec);
            BigDecimal totalDebit = allFilteredTransactions.stream()
                    .map(t -> t.getDebit() != null ? t.getDebit() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalCredit = allFilteredTransactions.stream()
                    .map(t -> t.getCredit() != null ? t.getCredit() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalTvaDeductible = allFilteredTransactions.stream()
                    .map(Transaction::getTvaDeductible).filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalTvaCollectee = allFilteredTransactions.stream()
                    .map(Transaction::getTvaCollectee).filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            agg = new TransactionAggregatesDto(totalDebit, totalCredit, totalTvaDeductible, totalTvaCollectee);
        }

        BigDecimal tvaDue = agg.totalTvaCollectee().subtract(agg.totalTvaDeductible());

        // 2. Pagination
        Page<Transaction> paginatedTransactions = transactionRepository.findAll(combinedSpec, defaultSort(pageable));
        Page<TransactionResponseDto> paginatedDtos = paginatedTransactions.map(transactionMapper::toDto);

        log.info("Agrégation terminée pour la banque {}. Débit total: {}, Crédit total: {}, TVA due: {}",
                banqueId, agg.totalDebit(), agg.totalCredit(), tvaDue);

        return new AgreratedTransactionResponseDto(
                agg.totalDebit(), agg.totalCredit(), agg.totalTvaDeductible(), agg.totalTvaCollectee(),
                tvaDue, paginatedDtos
        );
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportBankTransactionsPdf(Long banqueId, TransactionSearchDto searchDto) {
        log.info("Export PDF des transactions de la banque ID: {}. Filtres: {}", banqueId, searchDto);

        Specification<Transaction> combinedSpec = buildBankTransactionsSpec(banqueId, searchDto);
        List<TransactionResponseDto> transactions = transactionRepository
                .findAll(combinedSpec, Sort.by(Sort.Direction.DESC, "dateEmission")).stream()
                .map(transactionMapper::toDto)
                .toList();

        return generateTransactionHistoryPdf("Rapprochement en Cours — Transactions", transactions, buildBankFilterSummary(searchDto));
    }

    /**
     * Construit la spécification de recherche partagée par {@link #getTransactionsByBanque} et
     * {@link #exportBankTransactionsPdf} — mêmes critères, jamais dupliqués.
     */
    private Specification<Transaction> buildBankTransactionsSpec(Long banqueId, TransactionSearchDto searchDto) {
        List<Specification<Transaction>> specs = new ArrayList<>();
        specs.add(hasBanqueId(banqueId));

        // Filtrer par rapprochement spécifique si fourni, sinon EN_COURS (compatibilité)
        if (searchDto.rapprochementId() != null) {
            specs.add(hasRapprochementId(searchDto.rapprochementId()));
        } else {
            specs.add(hasRapprochementStatut(StatutRapprochement.EN_COURS));
        }

        if (searchDto.libelle() != null && !searchDto.libelle().isEmpty()) {
            specs.add(libelleContains(searchDto.libelle()));
        }

        if (searchDto.observation() != null && !searchDto.observation().isEmpty()) {
            specs.add(observationContains(searchDto.observation()));
        }

        if (searchDto.montant() != null) {
            specs.add(hasMontant(searchDto.montant()));
        }

        if (searchDto.dateEmissionDebut() != null || searchDto.dateEmissionFin() != null) {
            specs.add(hasDateEmissionBetween(searchDto.dateEmissionDebut(), searchDto.dateEmissionFin()));
        }

        if (searchDto.dateOperationDebut() != null || searchDto.dateOperationFin() != null) {
            specs.add(hasDateOperationBetween(searchDto.dateOperationDebut(), searchDto.dateOperationFin()));
        }

        if (searchDto.dateValeurDebut() != null || searchDto.dateValeurFin() != null) {
            specs.add(hasDateValeurBetween(searchDto.dateValeurDebut(), searchDto.dateValeurFin()));
        }

        if (searchDto.transactionSources() != null && !searchDto.transactionSources().isEmpty()) {
            specs.add(hasTransactionSources(searchDto.transactionSources()));
        }

        return withFilters(specs);
    }

    private List<String> buildBankFilterSummary(TransactionSearchDto dto) {
        List<String> lines = new ArrayList<>();
        if (dto == null) return lines;

        if (dto.libelle() != null && !dto.libelle().isBlank()) {
            lines.add("Libellé : " + dto.libelle());
        }
        if (dto.observation() != null && !dto.observation().isBlank()) {
            lines.add("Observation : " + dto.observation());
        }
        if (dto.montant() != null) {
            lines.add("Montant : " + formatMontantTx(dto.montant()));
        }
        if (dto.dateEmissionDebut() != null) {
            lines.add("Date émission min : " + dto.dateEmissionDebut().format(DATE_FMT));
        }
        if (dto.dateEmissionFin() != null) {
            lines.add("Date émission max : " + dto.dateEmissionFin().format(DATE_FMT));
        }
        if (dto.dateOperationDebut() != null) {
            lines.add("Date opération min : " + dto.dateOperationDebut().format(DATE_FMT));
        }
        if (dto.dateOperationFin() != null) {
            lines.add("Date opération max : " + dto.dateOperationFin().format(DATE_FMT));
        }
        if (dto.dateValeurDebut() != null) {
            lines.add("Date valeur min : " + dto.dateValeurDebut().format(DATE_FMT));
        }
        if (dto.dateValeurFin() != null) {
            lines.add("Date valeur max : " + dto.dateValeurFin().format(DATE_FMT));
        }
        if (dto.transactionSources() != null && !dto.transactionSources().isEmpty()) {
            lines.add("Source : " + dto.transactionSources());
        }
        return lines;
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponseDto getTransactionById(Long id) {
        log.info("Recherche de la transaction ID: {}", id);
        return transactionRepository.findById(id)
                .map(transactionMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("La transaction avec l'ID " + id + " n'a pas été trouvée"));
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = Transaction.class)
    public TransactionResponseDto updateTransactionById(Long id, TransactionUpdateRequestDto updateRequestDto){

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction with id " + id + " not found"));

        // Guard — rapprochement définitivement clôturé : aucune modification autorisée
        // (même garde que ReverseTransactionService.annulerTransaction — cohérence des deux
        // seuls points de mutation d'une Transaction déjà rattachée à un rapprochement clos).
        if (transaction.getRapprochement().getStatut() == StatutRapprochement.CLOTURE_DEFINITIVE) {
            throw new ValidationException(
                    "Impossible de modifier la transaction " + id
                    + " : le rapprochement est définitivement clôturé.");
        }

        // Le montant (débit ou crédit) n'est pas éditable ici — seule la TVA portée dessus l'est.
        // Le sens (quel côté est non nul) ne change jamais après validation, donc le côté qui
        // portait la TVA reste le même ; TvaResolver refait la même conversion/les mêmes bornes
        // qu'à la validation initiale, jamais un montant de TVA fourni tel quel par le client.
        boolean estDecaissement = transaction.getDebit() != null && transaction.getDebit().signum() > 0;
        BigDecimal montantTtc = estDecaissement ? transaction.getDebit() : transaction.getCredit();
        BigDecimal tvaResolue = TvaResolver.resoudre(montantTtc, updateRequestDto.tauxTva(), updateRequestDto.montantTva());

        transaction.setDateValeur(updateRequestDto.dateValeur());
        transaction.setDateOperations(updateRequestDto.dateOperation());
        transaction.setLibelle(updateRequestDto.libelle());
        transaction.setObservation(updateRequestDto.observation());
        if (estDecaissement) {
            transaction.setTvaDeductible(tvaResolue);
            transaction.setTvaCollectee(BigDecimal.ZERO);
        } else {
            transaction.setTvaCollectee(tvaResolue);
            transaction.setTvaDeductible(BigDecimal.ZERO);
        }

        Transaction saved = transactionRepository.save(transaction);

        // Propagation vers l'entité source (architecture.md, Règle 2) — dispatch via le registre
        // de TransactionSyncHandler, jamais un switch central. Un handler manquant (LCN legacy,
        // CAUTION, Importation — sources sans entité à synchroniser) ne fait rien, comme l'ancien
        // if-chain qui ne couvrait déjà pas ces valeurs.
        if (saved.getReferent() != null) {
            TransactionSyncHandler handler = syncHandlerRegistry.get(saved.getTransactionSource());
            if (handler != null) {
                log.info("Transaction {} ID: {} mise à jour — synchronisation de l'entité source ID: {}",
                        saved.getTransactionSource(), saved.getId(), saved.getReferent());
                handler.sync(saved.getReferent(), saved.getDateValeur(), saved.getDateOperations(),
                        saved.getLibelle(), saved.getObservation());
            }
        }

        return transactionMapper.toDto(saved);
    }


    /**
     * Compute live balance of a banque.
     */
    @Override
    @Transactional(readOnly = true)
    public BigDecimal computeBalance(Long banqueId) {
        return transactionRepository
                .computeBalance(banqueId);
    }

    /**
     * Count pending (unconfirmed) transactions.
     */
    @Override
    @Transactional(readOnly = true)
    public long countPendingTransactions(Long banqueId) {
        return transactionRepository.countPendingByBanque(banqueId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, BigDecimal> computeBalancesForBanks(List<Long> bankIds) {
        if (bankIds == null || bankIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // Fetches List<Object[]> where [0] is BankId and [1] is Calculated Balance
        List<Object[]> results = transactionRepository.findBalancesByBankIds(bankIds);

        return results.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (BigDecimal) row[1],
                        (existing, replacement) -> existing
                ));
    }

    // ── Façade pour le domaine Rapprochement ────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponseDto> searchTransactionsForRapprochement(Long rapprochementId, TransactionSearchDto searchDto, Pageable pageable) {
        List<Specification<Transaction>> specs = new ArrayList<>();
        specs.add(hasRapprochementId(rapprochementId));

        if (searchDto.libelle() != null && !searchDto.libelle().isEmpty()) {
            specs.add(libelleContains(searchDto.libelle()));
        }
        if (searchDto.observation() != null && !searchDto.observation().isEmpty()) {
            specs.add(observationContains(searchDto.observation()));
        }
        if (searchDto.montant() != null) {
            specs.add(hasMontant(searchDto.montant()));
        }
        if (searchDto.dateEmissionDebut() != null || searchDto.dateEmissionFin() != null) {
            specs.add(hasDateEmissionBetween(searchDto.dateEmissionDebut(), searchDto.dateEmissionFin()));
        }
        if (searchDto.dateOperationDebut() != null || searchDto.dateOperationFin() != null) {
            specs.add(hasDateOperationBetween(searchDto.dateOperationDebut(), searchDto.dateOperationFin()));
        }
        if (searchDto.dateValeurDebut() != null || searchDto.dateValeurFin() != null) {
            specs.add(hasDateValeurBetween(searchDto.dateValeurDebut(), searchDto.dateValeurFin()));
        }
        if (searchDto.transactionSources() != null && !searchDto.transactionSources().isEmpty()) {
            specs.add(hasTransactionSources(searchDto.transactionSources()));
        }

        return transactionRepository
                .findAll(withFilters(specs), pageable)
                .map(transactionMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByRapprochementId(Long rapprochementId) {
        return transactionRepository.countByRapprochementId(rapprochementId);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionAggregatesDto computeAggregatesForRapprochement(Long rapprochementId) {
        List<Object[]> rows = transactionRepository.computeAggregatesForRapprochement(rapprochementId);
        return rows.isEmpty() ? TransactionAggregatesDto.EMPTY : TransactionAggregatesDto.fromRow(rows.get(0));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, TransactionAggregatesDto> computeAggregatesByRapprochementIds(List<Long> rapprochementIds) {
        if (rapprochementIds == null || rapprochementIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return transactionRepository.computeAggregatesByRapprochementIds(rapprochementIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> new TransactionAggregatesDto(
                                (BigDecimal) row[1], (BigDecimal) row[2], (BigDecimal) row[3], (BigDecimal) row[4])
                ));
    }

    // ── Private helpers ──────────────────────────────────────────────────────────────────────

    private Pageable defaultSort(Pageable pageable) {
        return pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id"));
    }

    // ── Export PDF ───────────────────────────────────────────────────────────────────────────

    /**
     * Génère le PDF de l'historique des transactions filtré — mêmes colonnes que l'écran
     * "Historique des transactions" (ID, Banque, dates, débit/crédit, TVA, source, statut,
     * libellé, observation), plus un résumé des filtres appliqués et les totaux débit/crédit.
     */
    private byte[] generateTransactionHistoryPdf(String title, List<TransactionResponseDto> transactions, List<String> filterLines) {
        BigDecimal totalDebit = transactions.stream()
                .map(TransactionResponseDto::debit)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = transactions.stream()
                .map(TransactionResponseDto::credit)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        try {
            return staportPdfReportService.generateReport(title, PageSize.A4.rotate(), (doc, writer) -> {
                try {
                    Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
                    Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.decode("#374151"));
                    Font hdrFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, Color.WHITE);
                    Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 7, Color.BLACK);
                    Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.decode("#1e3a8a"));

                    doc.add(Chunk.NEWLINE);
                    doc.add(Chunk.NEWLINE);

                    Paragraph titleParagraph = new Paragraph(title, titleFont);
                    titleParagraph.setAlignment(Element.ALIGN_CENTER);
                    doc.add(titleParagraph);

                    String exportInfo = "Exporté le " + LocalDate.now().format(DATE_FMT)
                            + "  •  " + transactions.size() + " transaction(s)";
                    if (!filterLines.isEmpty()) {
                        exportInfo += "  •  Filtres : " + String.join("  |  ", filterLines);
                    }
                    Paragraph sub = new Paragraph(exportInfo, subFont);
                    sub.setAlignment(Element.ALIGN_CENTER);
                    sub.setSpacingAfter(8f);
                    doc.add(sub);

                    float[] colWidths = {5f, 10f, 8f, 8f, 8f, 8f, 8f, 8f, 8f, 9f, 8f, 12f, 12f};
                    PdfPTable table = new PdfPTable(colWidths);
                    table.setWidthPercentage(100f);
                    table.setSpacingBefore(8f);
                    table.setHeaderRows(1);

                    for (String h : new String[]{
                            "ID", "Banque", "Date émission", "Date opération", "Date valeur",
                            "Débit (MAD)", "Crédit (MAD)", "TVA Déd.", "TVA Coll.", "Source", "Statut", "Libellé", "Observation"}) {
                        PdfPCell c = new PdfPCell(new Phrase(h, hdrFont));
                        c.setBackgroundColor(MED_BLUE);
                        c.setPadding(4f);
                        c.setHorizontalAlignment(Element.ALIGN_CENTER);
                        c.setBorderColor(Color.WHITE);
                        c.setBorderWidth(0.5f);
                        table.addCell(c);
                    }

                    for (int i = 0; i < transactions.size(); i++) {
                        TransactionResponseDto t = transactions.get(i);
                        Color rowBg = i % 2 == 0 ? Color.WHITE : ROW_ALT;

                        addTxPdfCell(table, String.valueOf(t.id()), cellFont, rowBg, Element.ALIGN_CENTER);
                        addTxPdfCell(table, t.banqueDePrelevementNom(), cellFont, rowBg, Element.ALIGN_LEFT);
                        addTxPdfCell(table, t.dateEmission() != null ? t.dateEmission().format(DATE_FMT) : "—", cellFont, rowBg, Element.ALIGN_CENTER);
                        addTxPdfCell(table, t.dateOperations() != null ? t.dateOperations().format(DATE_FMT) : "—", cellFont, rowBg, Element.ALIGN_CENTER);
                        addTxPdfCell(table, t.dateValeur() != null ? t.dateValeur().format(DATE_FMT) : "—", cellFont, rowBg, Element.ALIGN_CENTER);
                        addTxPdfCell(table, formatMontantTx(t.debit()), cellFont, rowBg, Element.ALIGN_RIGHT);
                        addTxPdfCell(table, formatMontantTx(t.credit()), cellFont, rowBg, Element.ALIGN_RIGHT);
                        addTxPdfCell(table, formatMontantTx(t.tvaDeductible()), cellFont, rowBg, Element.ALIGN_RIGHT);
                        addTxPdfCell(table, formatMontantTx(t.tvaCollectee()), cellFont, rowBg, Element.ALIGN_RIGHT);
                        addTxPdfCell(table, t.transactionSource() != null ? t.transactionSource().name() : "—", cellFont, rowBg, Element.ALIGN_CENTER);
                        addTxPdfCell(table, t.statutRapprochement() != null ? t.statutRapprochement().name() : "—", cellFont, rowBg, Element.ALIGN_CENTER);
                        addTxPdfCell(table, t.libelle(), cellFont, rowBg, Element.ALIGN_LEFT);
                        addTxPdfCell(table, t.observation(), cellFont, rowBg, Element.ALIGN_LEFT);
                    }

                    PdfPCell totalLabel = new PdfPCell(
                            new Phrase("TOTAL — " + transactions.size() + " transaction(s)", totalFont));
                    totalLabel.setColspan(5);
                    totalLabel.setBackgroundColor(LIGHT_BLUE);
                    totalLabel.setPadding(5f);
                    totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    totalLabel.setBorderColor(MED_BLUE);
                    table.addCell(totalLabel);

                    PdfPCell totalDebitVal = new PdfPCell(new Phrase(formatMontantTx(totalDebit), totalFont));
                    totalDebitVal.setBackgroundColor(LIGHT_BLUE);
                    totalDebitVal.setPadding(5f);
                    totalDebitVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    totalDebitVal.setBorderColor(MED_BLUE);
                    table.addCell(totalDebitVal);

                    PdfPCell totalCreditVal = new PdfPCell(new Phrase(formatMontantTx(totalCredit), totalFont));
                    totalCreditVal.setBackgroundColor(LIGHT_BLUE);
                    totalCreditVal.setPadding(5f);
                    totalCreditVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    totalCreditVal.setBorderColor(MED_BLUE);
                    table.addCell(totalCreditVal);

                    // colonnes restantes : TVA Déd., TVA Coll., Source, Statut, Libellé, Observation (6 colonnes)
                    for (int j = 0; j < 6; j++) {
                        PdfPCell empty = new PdfPCell(new Phrase(""));
                        empty.setBackgroundColor(LIGHT_BLUE);
                        empty.setBorderColor(MED_BLUE);
                        table.addCell(empty);
                    }

                    doc.add(table);
                } catch (DocumentException e) {
                    throw new RuntimeException("Erreur lors de la génération du contenu du PDF de l'historique des transactions.", e);
                }
            });
        } catch (DocumentException e) {
            throw new RuntimeException("Erreur lors de la génération du PDF de l'historique des transactions.", e);
        }
    }

    /**
     * Résumé texte des filtres appliqués. Le nom de la banque est lu depuis la première ligne du
     * résultat (déjà dénormalisé sur {@link TransactionResponseDto}) plutôt que via une nouvelle
     * dépendance vers le domaine Banque — {@code BanqueServiceImpl} dépend déjà de
     * {@code ITransactionService}, une dépendance inverse créerait un cycle.
     */
    private List<String> buildFilterSummary(TransactionSearchDto dto, List<TransactionResponseDto> transactions) {
        List<String> lines = new ArrayList<>();
        if (dto == null) return lines;

        if (dto.libelle() != null && !dto.libelle().isBlank()) {
            lines.add("Libellé : " + dto.libelle());
        }
        if (dto.banqueId() != null) {
            String banqueNom = transactions.stream()
                    .map(TransactionResponseDto::banqueDePrelevementNom)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse("ID " + dto.banqueId());
            lines.add("Banque : " + banqueNom);
        }
        if (dto.transactionSources() != null && !dto.transactionSources().isEmpty()) {
            lines.add("Sources : " + dto.transactionSources().stream()
                    .map(Enum::name)
                    .collect(Collectors.joining(", ")));
        }
        if (dto.statutRapprochement() != null) {
            lines.add("Statut rapprochement : " + dto.statutRapprochement().name());
        }
        if (dto.montant() != null) {
            lines.add("Montant : " + formatMontantTx(dto.montant()));
        }
        if (dto.dateEmissionDebut() != null) {
            lines.add("Date émission min : " + dto.dateEmissionDebut().format(DATE_FMT));
        }
        if (dto.dateEmissionFin() != null) {
            lines.add("Date émission max : " + dto.dateEmissionFin().format(DATE_FMT));
        }
        if (dto.dateOperationDebut() != null) {
            lines.add("Date opération min : " + dto.dateOperationDebut().format(DATE_FMT));
        }
        if (dto.dateOperationFin() != null) {
            lines.add("Date opération max : " + dto.dateOperationFin().format(DATE_FMT));
        }
        if (dto.dateValeurDebut() != null) {
            lines.add("Date valeur min : " + dto.dateValeurDebut().format(DATE_FMT));
        }
        if (dto.dateValeurFin() != null) {
            lines.add("Date valeur max : " + dto.dateValeurFin().format(DATE_FMT));
        }
        return lines;
    }

    private void addTxPdfCell(PdfPTable table, String text, Font font, Color bg, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "—", font));
        cell.setBackgroundColor(bg);
        cell.setPadding(3f);
        cell.setHorizontalAlignment(align);
        cell.setBorderColor(BORDER_GRAY);
        cell.setBorderWidth(0.5f);
        table.addCell(cell);
    }

    private String formatMontantTx(BigDecimal montant) {
        if (montant == null) return "—";
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRANCE);
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
        DecimalFormat formatter = new DecimalFormat("#,##0.00", symbols);
        return formatter.format(montant);
    }
}
