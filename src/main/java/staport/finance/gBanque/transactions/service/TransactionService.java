package staport.finance.gBanque.transactions.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.cmt.echeances.services.interfaces.ICmtValidationService;
import staport.finance.gBanque.leasing.echeancesLeasing.services.interfaces.ILeasingValidationService;
import staport.finance.gBanque.moyensDePaiements.cheques.validation.services.interfaces.IChequeValidationService;
import staport.finance.gBanque.moyensDePaiements.lcn.validation.services.interfaces.ILcnValidationService;
import staport.finance.gBanque.moyensDePaiements.virements.validation.services.interfaces.IVirementValidationService;
import staport.finance.gBanque.moyensDePaiements.virementInternational.validation.services.interfaces.IVirementInternationalValidationService;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.validation.services.interfaces.IMiseADispositionValidationService;
import staport.finance.gBanque.prelevements.validation.services.interfaces.IPrelevementValidationService;
import staport.finance.gBanque.transactions.dtos.AgreratedTransactionResponseDto;
import staport.finance.gBanque.transactions.dtos.TransactionResponseDto;
import staport.finance.gBanque.transactions.dtos.TransactionSearchDto;
import staport.finance.gBanque.transactions.dtos.TransactionUpdateRequestDto;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.transactions.mapper.TransactionMapper;
import staport.finance.gBanque.transactions.repositories.TransactionRepository;

import staport.finance.gBanque.rapprochement.enums.StatutRapprochement;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static staport.finance.gBanque.transactions.spec.TransactionSpecifications.*;

@Service
public class TransactionService implements ITransactionService{

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);
    private final TransactionRepository transactionRepository;
    private final ILeasingValidationService leasingValidationService;
    private final ICmtValidationService cmtValidationService;
    private final IChequeValidationService chequeValidationService;
    private final ILcnValidationService lcnValidationService;
    private final IVirementValidationService virementValidationService;
    private final IVirementInternationalValidationService virementInternationalValidationService;
    private final IMiseADispositionValidationService miseADispositionValidationService;
    private final IPrelevementValidationService prelevementValidationService;

    public TransactionService(TransactionRepository transactionRepository,
                              ILeasingValidationService leasingValidationService,
                              ICmtValidationService cmtValidationService,
                              IChequeValidationService chequeValidationService,
                              ILcnValidationService lcnValidationService,
                              IVirementValidationService virementValidationService,
                              IVirementInternationalValidationService virementInternationalValidationService,
                              IMiseADispositionValidationService miseADispositionValidationService,
                              IPrelevementValidationService prelevementValidationService) {
        this.transactionRepository = transactionRepository;
        this.leasingValidationService = leasingValidationService;
        this.cmtValidationService = cmtValidationService;
        this.chequeValidationService = chequeValidationService;
        this.virementValidationService = virementValidationService;
        this.lcnValidationService = lcnValidationService;
        this.virementInternationalValidationService = virementInternationalValidationService;
        this.miseADispositionValidationService = miseADispositionValidationService;
        this.prelevementValidationService = prelevementValidationService;
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponseDto> getTransactionHistory(TransactionSearchDto searchDto, Pageable pageable) {
        log.info("Récupération de l'historique des transactions. Filtres: {}", searchDto);

        List<Specification<Transaction>> specs = new ArrayList<>();

        if (searchDto.getLibelle() != null && !searchDto.getLibelle().isEmpty()) {
            specs.add(libelleContains(searchDto.getLibelle()));
            log.debug("Filtre appliqué: libellé contient '{}'", searchDto.getLibelle());
        }



        if (searchDto.getBanqueId() != null) {
            specs.add(hasBanqueId(searchDto.getBanqueId()));
            log.debug("Filtre appliqué: ID banque = {}", searchDto.getBanqueId());
        }

        if (searchDto.getTransactionSources() != null && !searchDto.getTransactionSources().isEmpty()) {
            specs.add(hasTransactionSources(searchDto.getTransactionSources()));
            log.debug("Filtre appliqué: sources = {}", searchDto.getTransactionSources());
        }

        if (searchDto.getStatutRapprochement() != null) {
            specs.add(hasRapprochementStatut(searchDto.getStatutRapprochement()));
            log.debug("Filtre appliqué: statut rapprochement = {}", searchDto.getStatutRapprochement());
        }

        if (searchDto.getMontant() != null) {
            specs.add(hasMontant(searchDto.getMontant()));
            log.debug("Filtre appliqué: montant = {}", searchDto.getMontant());
        }

        Specification<Transaction> combinedSpec = withFilters(specs);
        Page<Transaction> transactions = transactionRepository.findAll(combinedSpec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));

        log.info("Historique récupéré: {} transactions trouvées (Page {}/{})",
                transactions.getTotalElements(), transactions.getNumber(), transactions.getTotalPages());

        return transactions.map(TransactionMapper::toDto);
    }

    @Transactional(readOnly = true)
    public AgreratedTransactionResponseDto getTransactionsByBanque(Long banqueId, TransactionSearchDto searchDto, Pageable pageable) {
        log.info("Calcul des agrégations et récupération des transactions pour la banque ID: {}", banqueId);

        List<Specification<Transaction>> specs = new ArrayList<>();
        specs.add(hasBanqueId(banqueId));

        // Filtrer par rapprochement spécifique si fourni, sinon EN_COURS (compatibilité)
        if (searchDto.getRapprochementId() != null) {
            specs.add(hasRapprochementId(searchDto.getRapprochementId()));
        } else {
            specs.add(hasRapprochementStatut(StatutRapprochement.EN_COURS));
        }

        if (searchDto.getLibelle() != null && !searchDto.getLibelle().isEmpty()) {
            specs.add(libelleContains(searchDto.getLibelle()));
        }

        if (searchDto.getObservation() != null && !searchDto.getObservation().isEmpty()) {
            specs.add(observationContains(searchDto.getObservation()));
        }

        if (searchDto.getMontant() != null) {
            specs.add(hasMontant(searchDto.getMontant()));
            log.debug("Filtre appliqué: montant égal '{}'", searchDto.getMontant());
        }

        if (searchDto.getDateOperationDebut() != null || searchDto.getDateOperationFin() != null) {
            specs.add(hasDateOperationBetween(searchDto.getDateOperationDebut(), searchDto.getDateOperationFin()));
            log.debug("Filtre appliqué: date opération entre '{}' et '{}'",
                    searchDto.getDateOperationDebut(), searchDto.getDateOperationFin());
        }

        if (searchDto.getTransactionSources() != null && !searchDto.getTransactionSources().isEmpty()) {
            specs.add(hasTransactionSources(searchDto.getTransactionSources()));
            log.debug("Filtre appliqué: sources = {}", searchDto.getTransactionSources());
        }

        Specification<Transaction> combinedSpec = withFilters(specs);

        // 1. Agrégats via JPQL (SUM en base, pas de List<Transaction> en mémoire)
        BigDecimal totalDebit;
        BigDecimal totalCredit;
        BigDecimal totalTvaDeductible;
        BigDecimal totalTvaCollectee;

        if (searchDto.getRapprochementId() != null) {
            // Cas nominal : agrégats pour un rapprochement précis via JPQL optimisé
            List<Object[]> agg = transactionRepository.computeAggregatesForRapprochement(searchDto.getRapprochementId());
            Object[] row = agg.isEmpty() ? null : agg.get(0);
            totalDebit         = row != null && row[0] != null ? (BigDecimal) row[0] : BigDecimal.ZERO;
            totalCredit        = row != null && row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
            totalTvaDeductible = row != null && row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO;
            totalTvaCollectee  = row != null && row[3] != null ? (BigDecimal) row[3] : BigDecimal.ZERO;
        } else {
            // Fallback : comportement d'origine (agrégats en mémoire sur EN_COURS)
            List<Transaction> allFilteredTransactions = transactionRepository.findAll(combinedSpec);
            totalDebit = allFilteredTransactions.stream()
                    .map(t -> t.getDebit() != null ? t.getDebit() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            totalCredit = allFilteredTransactions.stream()
                    .map(t -> t.getCredit() != null ? t.getCredit() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            totalTvaDeductible = allFilteredTransactions.stream()
                    .map(Transaction::getTvaDeductible).filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            totalTvaCollectee = allFilteredTransactions.stream()
                    .map(Transaction::getTvaCollectee).filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        BigDecimal tvaDue = totalTvaCollectee.subtract(totalTvaDeductible);

        // 2. Pagination
        Page<Transaction> paginatedTransactions = transactionRepository.findAll(combinedSpec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));
        Page<TransactionResponseDto> paginatedDtos = paginatedTransactions.map(TransactionMapper::toDto);

        AgreratedTransactionResponseDto aggregatedResponse = new AgreratedTransactionResponseDto();
        aggregatedResponse.setPage(paginatedDtos);
        aggregatedResponse.setTotalDebit(totalDebit);
        aggregatedResponse.setTotalCredit(totalCredit);
        aggregatedResponse.setTotalTvaDeductible(totalTvaDeductible);
        aggregatedResponse.setTotalTvaCollectee(totalTvaCollectee);
        aggregatedResponse.setTvaDue(tvaDue);

        log.info("Agrégation terminée pour la banque {}. Débit total: {}, Crédit total: {}, TVA due: {}",
                banqueId, totalDebit, totalCredit, tvaDue);

        return aggregatedResponse;
    }

    @Transactional(readOnly = true)
    public TransactionResponseDto getTransactionById(Long id) {
        log.info("Recherche de la transaction ID: {}", id);
        return transactionRepository.findById(id)
                .map(transaction -> {
                    log.debug("Transaction ID: {} trouvée", id);
                    return TransactionMapper.toDto(transaction);
                })
                .orElseThrow(() -> {
                    log.error("Erreur: Transaction ID: {} introuvable", id);
                    return new ResourceNotFoundException("La transaction avec l'ID " + id + " n'a pas été trouvée");
                });
    }

    @Override
    @Transactional
    public TransactionResponseDto updateTransactionById(Long id, TransactionUpdateRequestDto updateRequestDto){

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction with id " + id + " not found"));

        // Guard — rapprochement définitivement clôturé : aucune modification autorisée
//        if (transaction.getRapprochement().getStatut() == StatutRapprochement.CLOTURE_DEFINITIVE) {
//            throw new ValidationException(
//                    "Impossible de modifier la transaction " + id
//                    + " : le rapprochement est définitivement clôturé.");
//        }

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

        return TransactionMapper.toDto(saved);
    }


    /**
     * Compute live balance of a banque.
     */
    @Override
    public BigDecimal computeBalance(Long banqueId) {
        return transactionRepository
                .computeBalance(banqueId);
    }

    /**
     * Count pending (unconfirmed) transactions.
     */
    @Override
    public long countPendingTransactions(Long banqueId) {
        return transactionRepository.countPendingByBanque(banqueId);
    }

    @Override
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
}