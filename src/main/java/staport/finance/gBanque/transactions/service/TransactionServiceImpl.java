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
import staport.finance.gBanque.Agios.service.interfaces.IAgiosValidationService;
import staport.finance.gBanque.audit.AuditOperation;
import staport.finance.gBanque.audit.AuditableAction;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.cmt.echeances.services.interfaces.ICmtValidationService;
import staport.finance.gBanque.commision.validation.services.interfaces.ICommissionValidationService;
import staport.finance.gBanque.decomptes.validation.services.interfaces.IDecompteValidationService;
import staport.finance.gBanque.droitConstate.validation.services.interfaces.IDroitConstateValidationService;
import staport.finance.gBanque.factoring.validation.services.interfaces.IFactoringValidationService;
import staport.finance.gBanque.fraisBancaires.validation.services.interfaces.IFraisBancaireValidationService;
import staport.finance.gBanque.impayees.validation.services.interfaces.IImpayeeValidationService;
import staport.finance.gBanque.impot_taxes.validation.services.interfaces.IImpotEtTaxeValidationService;
import staport.finance.gBanque.operationErrone.validation.services.interfaces.IOperationErroneValidationService;
import staport.finance.gBanque.restitution.validation.services.interfaces.IRestitutionValidationService;
import staport.finance.gBanque.leasing.echeancesLeasing.services.interfaces.ILeasingValidationService;
import staport.finance.gBanque.moyensDePaiements.cheques.validation.services.interfaces.IChequeValidationService;
import staport.finance.gBanque.moyensDePaiements.lcn.validation.services.interfaces.ILcnValidationService;
import staport.finance.gBanque.moyensDePaiements.virements.validation.services.interfaces.IVirementValidationService;
import staport.finance.gBanque.moyensDePaiements.virementInternational.validation.services.interfaces.IVirementInternationalValidationService;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.validation.services.interfaces.IMiseADispositionValidationService;
import staport.finance.gBanque.prefinancement.validation.services.interfaces.IPrefinancementValidationService;
import staport.finance.gBanque.prelevements.validation.services.interfaces.IPrelevementValidationService;
import staport.finance.gBanque.rapprochement.enums.StatutRapprochement;
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
    private final ILeasingValidationService leasingValidationService;
    private final ICmtValidationService cmtValidationService;
    private final IChequeValidationService chequeValidationService;
    private final ILcnValidationService lcnValidationService;
    private final IVirementValidationService virementValidationService;
    private final IVirementInternationalValidationService virementInternationalValidationService;
    private final IMiseADispositionValidationService miseADispositionValidationService;
    private final IPrelevementValidationService prelevementValidationService;
    private final IPrefinancementValidationService prefinancementValidationService;
    private final IFactoringValidationService factoringValidationService;
    private final IDroitConstateValidationService droitConstateValidationService;
    private final IDecompteValidationService decompteValidationService;
    private final IImpotEtTaxeValidationService impotEtTaxeValidationService;
    private final ICommissionValidationService commissionValidationService;
    private final IFraisBancaireValidationService fraisBancaireValidationService;
    private final IRestitutionValidationService restitutionValidationService;
    private final IOperationErroneValidationService operationErroneValidationService;
    private final IImpayeeValidationService impayeeValidationService;
    private final IAgiosValidationService agiosValidationService;

    public TransactionServiceImpl(TransactionRepository transactionRepository,
                              TransactionMapper transactionMapper,
                              StaportPdfReportService staportPdfReportService,
                              ILeasingValidationService leasingValidationService,
                              ICmtValidationService cmtValidationService,
                              IChequeValidationService chequeValidationService,
                              ILcnValidationService lcnValidationService,
                              IVirementValidationService virementValidationService,
                              IVirementInternationalValidationService virementInternationalValidationService,
                              IMiseADispositionValidationService miseADispositionValidationService,
                              IPrelevementValidationService prelevementValidationService,
                              IPrefinancementValidationService prefinancementValidationService,
                              IFactoringValidationService factoringValidationService,
                              IDroitConstateValidationService droitConstateValidationService,
                              IDecompteValidationService decompteValidationService,
                              IImpotEtTaxeValidationService impotEtTaxeValidationService,
                              ICommissionValidationService commissionValidationService,
                              IFraisBancaireValidationService fraisBancaireValidationService,
                              IRestitutionValidationService restitutionValidationService,
                              IOperationErroneValidationService operationErroneValidationService,
                              IImpayeeValidationService impayeeValidationService,
                              IAgiosValidationService agiosValidationService) {
        this.transactionRepository = transactionRepository;
        this.transactionMapper = transactionMapper;
        this.staportPdfReportService = staportPdfReportService;
        this.leasingValidationService = leasingValidationService;
        this.cmtValidationService = cmtValidationService;
        this.chequeValidationService = chequeValidationService;
        this.virementValidationService = virementValidationService;
        this.lcnValidationService = lcnValidationService;
        this.virementInternationalValidationService = virementInternationalValidationService;
        this.miseADispositionValidationService = miseADispositionValidationService;
        this.prelevementValidationService = prelevementValidationService;
        this.prefinancementValidationService = prefinancementValidationService;
        this.factoringValidationService = factoringValidationService;
        this.droitConstateValidationService = droitConstateValidationService;
        this.decompteValidationService = decompteValidationService;
        this.impotEtTaxeValidationService = impotEtTaxeValidationService;
        this.commissionValidationService = commissionValidationService;
        this.fraisBancaireValidationService = fraisBancaireValidationService;
        this.restitutionValidationService = restitutionValidationService;
        this.operationErroneValidationService = operationErroneValidationService;
        this.impayeeValidationService = impayeeValidationService;
        this.agiosValidationService = agiosValidationService;
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

        transaction.setDateValeur(updateRequestDto.dateValeur());
        transaction.setDateOperations(updateRequestDto.dateOperation());
        transaction.setTvaCollectee(updateRequestDto.tvaCollecte());
        transaction.setTvaDeductible(updateRequestDto.tvaDeductible());

        Transaction saved = transactionRepository.save(transaction);

        // Synchronisation échéance leasing : si la transaction provient d'une échéance leasing,
        // on propage dateValeur et dateOperation vers l'échéance source (referent = echeanceId).
        if (TransactionSource.LEASING == saved.getTransactionSource() && saved.getReferent() != null) {
            log.info("Transaction LEASING ID: {} mise à jour — synchronisation de l'échéance ID: {}",
                    saved.getId(), saved.getReferent());
            leasingValidationService.syncEcheanceFromTransaction(
                    saved.getReferent(),
                    saved.getDateValeur(),
                    saved.getDateOperations());
        }

        // Synchronisation échéance CMT : si la transaction provient d'une échéance CMT,
        // on propage dateValeur et dateOperation vers l'échéance source (referent = echeanceId).
        if (TransactionSource.CMT == saved.getTransactionSource() && saved.getReferent() != null) {
            log.info("Transaction CMT ID: {} mise à jour — synchronisation de l'échéance ID: {}",
                    saved.getId(), saved.getReferent());
            cmtValidationService.syncEcheanceFromTransaction(
                    saved.getReferent(),
                    saved.getDateValeur(),
                    saved.getDateOperations());
        }

        // Synchronisation chèque : si la transaction provient d'un chèque, on propage
        // dateValeur et dateOperation vers le chèque source (referent = chequeId).
        if (TransactionSource.CHEQUE == saved.getTransactionSource() && saved.getReferent() != null) {
            log.info("Transaction CHEQUE ID: {} mise à jour — synchronisation du chèque ID: {}",
                    saved.getId(), saved.getReferent());
            chequeValidationService.syncChequeFromTransaction(
                    saved.getReferent(),
                    saved.getDateValeur(),
                    saved.getDateOperations());
        }

        // Synchronisation LCN : si la transaction provient d'un LCN (décaissement, encaissement ou
        // escompte), on propage dateValeur et dateOperation vers le LCN source (referent = lcnId).
        if ((TransactionSource.LCN_DECAISSEMENT == saved.getTransactionSource()
                || TransactionSource.LCN_ENCAISSEMENT == saved.getTransactionSource()
                || TransactionSource.LCN_ESCOMPTE == saved.getTransactionSource())
                && saved.getReferent() != null) {
            log.info("Transaction LCN ID: {} mise à jour — synchronisation du LCN ID: {}",
                    saved.getId(), saved.getReferent());
            lcnValidationService.syncLcnFromTransaction(
                    saved.getReferent(),
                    saved.getDateValeur(),
                    saved.getDateOperations());
        }

        // Synchronisation virement : si la transaction provient d'un virement, on propage
        // dateValeur et dateOperation vers le virement source (referent = virementId).
        if (TransactionSource.Virement == saved.getTransactionSource() && saved.getReferent() != null) {
            log.info("Transaction VIREMENT ID: {} mise à jour — synchronisation du virement ID: {}",
                    saved.getId(), saved.getReferent());
            virementValidationService.syncVirementFromTransaction(
                    saved.getReferent(),
                    saved.getDateValeur(),
                    saved.getDateOperations());
        }

        // Synchronisation virement international : si la transaction provient d'un virement
        // international, on propage dateValeur et dateOperation vers le virement international
        // source (referent = virementInternationalId).
        if (TransactionSource.VIREMENT_INTERNATIONAL == saved.getTransactionSource() && saved.getReferent() != null) {
            log.info("Transaction VIREMENT INTERNATIONAL ID: {} mise à jour — synchronisation du virement international ID: {}",
                    saved.getId(), saved.getReferent());
            virementInternationalValidationService.syncVirementInternationalFromTransaction(
                    saved.getReferent(),
                    saved.getDateValeur(),
                    saved.getDateOperations());
        }

        // Synchronisation mise à disposition : si la transaction provient d'une mise à disposition,
        // on propage dateValeur et dateOperation vers la mise à disposition source
        // (referent = miseADispositionId).
        if (TransactionSource.MiseADisposition == saved.getTransactionSource() && saved.getReferent() != null) {
            log.info("Transaction MISE A DISPOSITION ID: {} mise à jour — synchronisation de la mise à disposition ID: {}",
                    saved.getId(), saved.getReferent());
            miseADispositionValidationService.syncMiseADispositionFromTransaction(
                    saved.getReferent(),
                    saved.getDateValeur(),
                    saved.getDateOperations());
        }

        // Synchronisation prélèvement : si la transaction provient d'un prélèvement, on propage
        // dateValeur et dateOperation vers le prélèvement source (referent = prelevementId).
        if (TransactionSource.PRELEVEMENT == saved.getTransactionSource() && saved.getReferent() != null) {
            log.info("Transaction PRELEVEMENT ID: {} mise à jour — synchronisation du prélèvement ID: {}",
                    saved.getId(), saved.getReferent());
            prelevementValidationService.syncPrelevementFromTransaction(
                    saved.getReferent(),
                    saved.getDateValeur(),
                    saved.getDateOperations());
        }

        // Synchronisation préfinancement : si la transaction provient d'un préfinancement, on
        // propage dateValeur et dateOperation vers le préfinancement source (referent = prefinancementId).
        if (TransactionSource.PREFINANCEMENT == saved.getTransactionSource() && saved.getReferent() != null) {
            log.info("Transaction PREFINANCEMENT ID: {} mise à jour — synchronisation du préfinancement ID: {}",
                    saved.getId(), saved.getReferent());
            prefinancementValidationService.syncPrefinancementFromTransaction(
                    saved.getReferent(),
                    saved.getDateValeur(),
                    saved.getDateOperations());
        }

        // Synchronisation factoring : si la transaction provient d'un factoring, on propage
        // dateValeur et dateOperation vers le factoring source (referent = factoringId).
        if (TransactionSource.FACTORING == saved.getTransactionSource() && saved.getReferent() != null) {
            log.info("Transaction FACTORING ID: {} mise à jour — synchronisation du factoring ID: {}",
                    saved.getId(), saved.getReferent());
            factoringValidationService.syncFactoringFromTransaction(
                    saved.getReferent(),
                    saved.getDateValeur(),
                    saved.getDateOperations());
        }

        // Synchronisation droit constaté : si la transaction provient d'un droit constaté, on
        // propage dateValeur et dateOperation vers le droit constaté source (referent = droitConstateId).
        if (TransactionSource.DROIT_CONSTATE == saved.getTransactionSource() && saved.getReferent() != null) {
            log.info("Transaction DROIT_CONSTATE ID: {} mise à jour — synchronisation du droit constaté ID: {}",
                    saved.getId(), saved.getReferent());
            droitConstateValidationService.syncDroitConstateFromTransaction(
                    saved.getReferent(),
                    saved.getDateValeur(),
                    saved.getDateOperations());
        }

        // Synchronisation décompte : si la transaction provient d'un décompte, on propage
        // dateValeur et dateOperation vers le décompte source (referent = decompteId).
        if (TransactionSource.DECOMPTES == saved.getTransactionSource() && saved.getReferent() != null) {
            log.info("Transaction DECOMPTES ID: {} mise à jour — synchronisation du décompte ID: {}",
                    saved.getId(), saved.getReferent());
            decompteValidationService.syncDecompteFromTransaction(
                    saved.getReferent(),
                    saved.getDateValeur(),
                    saved.getDateOperations());
        }

        // Synchronisation impôt/taxe : si la transaction provient d'un impôt/taxe, on propage
        // dateValeur et dateOperation vers l'impôt/taxe source (referent = impotEtTaxeId).
        if (TransactionSource.IMPOT_TAXE == saved.getTransactionSource() && saved.getReferent() != null) {
            log.info("Transaction IMPOT_TAXE ID: {} mise à jour — synchronisation de l'impôt/taxe ID: {}",
                    saved.getId(), saved.getReferent());
            impotEtTaxeValidationService.syncImpotEtTaxeFromTransaction(
                    saved.getReferent(),
                    saved.getDateValeur(),
                    saved.getDateOperations());
        }

        // Synchronisation commission : si la transaction provient d'une commission, on propage
        // dateValeur et dateOperation vers la commission source (referent = commissionId).
        if (TransactionSource.COMMISSION == saved.getTransactionSource() && saved.getReferent() != null) {
            log.info("Transaction COMMISSION ID: {} mise à jour — synchronisation de la commission ID: {}",
                    saved.getId(), saved.getReferent());
            commissionValidationService.syncCommissionFromTransaction(
                    saved.getReferent(),
                    saved.getDateValeur(),
                    saved.getDateOperations());
        }

        // Synchronisation frais bancaire : si la transaction provient d'un frais bancaire, on
        // propage dateValeur et dateOperation vers le frais bancaire source (referent = fraisBancaireId).
        if (TransactionSource.FRAIS_BANCAIRE == saved.getTransactionSource() && saved.getReferent() != null) {
            log.info("Transaction FRAIS_BANCAIRE ID: {} mise à jour — synchronisation du frais bancaire ID: {}",
                    saved.getId(), saved.getReferent());
            fraisBancaireValidationService.syncFraisBancaireFromTransaction(
                    saved.getReferent(),
                    saved.getDateValeur(),
                    saved.getDateOperations());
        }

        // Synchronisation restitution : si la transaction provient d'une restitution, on propage
        // dateValeur et dateOperation vers la restitution source (referent = restitutionId).
        if (TransactionSource.RESTITUTION == saved.getTransactionSource() && saved.getReferent() != null) {
            log.info("Transaction RESTITUTION ID: {} mise à jour — synchronisation de la restitution ID: {}",
                    saved.getId(), saved.getReferent());
            restitutionValidationService.syncRestitutionFromTransaction(
                    saved.getReferent(),
                    saved.getDateValeur(),
                    saved.getDateOperations());
        }

        // Synchronisation opération erronée : si la transaction provient d'une opération erronée, on
        // propage dateValeur et dateOperation vers l'opération erronée source (referent = operationErroneId).
        if (TransactionSource.OPERATION_ERRONE == saved.getTransactionSource() && saved.getReferent() != null) {
            log.info("Transaction OPERATION_ERRONE ID: {} mise à jour — synchronisation de l'opération erronée ID: {}",
                    saved.getId(), saved.getReferent());
            operationErroneValidationService.syncOperationErroneFromTransaction(
                    saved.getReferent(),
                    saved.getDateValeur(),
                    saved.getDateOperations());
        }

        // Synchronisation impayée : si la transaction provient d'une impayée, on propage dateValeur
        // et dateOperation vers l'impayée source (referent = impayeeId).
        if (TransactionSource.IMPAYEE == saved.getTransactionSource() && saved.getReferent() != null) {
            log.info("Transaction IMPAYEE ID: {} mise à jour — synchronisation de l'impayée ID: {}",
                    saved.getId(), saved.getReferent());
            impayeeValidationService.syncImpayeeFromTransaction(
                    saved.getReferent(),
                    saved.getDateValeur(),
                    saved.getDateOperations());
        }

        // Synchronisation agios : si la transaction provient d'un agios, on propage dateValeur
        // et dateOperation vers l'agios source (referent = agiosId).
        if (TransactionSource.AGIOS == saved.getTransactionSource() && saved.getReferent() != null) {
            log.info("Transaction AGIOS ID: {} mise à jour — synchronisation de l'agios ID: {}",
                    saved.getId(), saved.getReferent());
            agiosValidationService.syncAgiosFromTransaction(
                    saved.getReferent(),
                    saved.getDateValeur(),
                    saved.getDateOperations());
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
