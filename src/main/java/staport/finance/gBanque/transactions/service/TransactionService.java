package staport.finance.gBanque.transactions.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.transactions.dtos.AgreratedTransactionResponseDto;
import staport.finance.gBanque.transactions.dtos.TransactionResponseDto;
import staport.finance.gBanque.transactions.dtos.TransactionSearchDto;
import staport.finance.gBanque.transactions.dtos.TransactionUpdateRequestDto;
import staport.finance.gBanque.transactions.entities.Transaction;
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

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
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
        Page<Transaction> transactions = transactionRepository.findAll(combinedSpec, pageable);

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
        Page<Transaction> paginatedTransactions = transactionRepository.findAll(combinedSpec, pageable);
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
        if (transaction.getRapprochement().getStatut() == StatutRapprochement.CLOTURE_DEFINITIVE) {
            throw new ValidationException(
                    "Impossible de modifier la transaction " + id
                    + " : le rapprochement est définitivement clôturé.");
        }

        transaction.setDateValeur(updateRequestDto.dateValeur());
        transaction.setDateOperations(updateRequestDto.dateOperation());
        transaction.setTvaCollectee(updateRequestDto.tvaCollecte());
        transaction.setTvaDeductible(updateRequestDto.tvaDeductible());

        return TransactionMapper.toDto(transactionRepository.save(transaction));
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