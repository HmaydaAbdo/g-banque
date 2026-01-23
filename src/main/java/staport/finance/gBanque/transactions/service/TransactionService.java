package staport.finance.gBanque.transactions.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.transactions.dtos.AgreratedTransactionResponseDto;
import staport.finance.gBanque.transactions.dtos.TransactionResponseDto;
import staport.finance.gBanque.transactions.dtos.TransactionSearchDto;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.mapper.TransactionMapper;
import staport.finance.gBanque.transactions.repositories.TransactionRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static staport.finance.gBanque.transactions.spec.TransactionSpecifications.*;

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);
    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

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

        if (searchDto.getConfirmed() != null) {
            specs.add(isConfirmed(searchDto.getConfirmed()));
            log.debug("Filtre appliqué: confirmé = {}", searchDto.getConfirmed());
        }

        Specification<Transaction> combinedSpec = withFilters(specs);
        Page<Transaction> transactions = transactionRepository.findAll(combinedSpec, pageable);

        log.info("Historique récupéré: {} transactions trouvées (Page {}/{})",
                transactions.getTotalElements(), transactions.getNumber(), transactions.getTotalPages());

        return transactions.map(TransactionMapper::toDto);
    }

    public AgreratedTransactionResponseDto getTransactionsByBanque(Long banqueId, TransactionSearchDto searchDto, Pageable pageable) {
        log.info("Calcul des agrégations et récupération des transactions pour la banque ID: {}", banqueId);

        List<Specification<Transaction>> specs = new ArrayList<>();
        specs.add(isConfirmed(false));
        specs.add(hasBanqueId(banqueId));

        if (searchDto.getLibelle() != null && !searchDto.getLibelle().isEmpty()) {
            specs.add(libelleContains(searchDto.getLibelle()));
        }

        if (searchDto.getObservation() != null && !searchDto.getObservation().isEmpty()) {
            specs.add(observationContains(searchDto.getObservation()));
        }

        if(searchDto.getMontant()!=null){
            specs.add(hasMontant(searchDto.getMontant()));
            log.debug("Filtre appliqué: montant égal  '{}'", searchDto.getMontant());

        }

        if (searchDto.getTransactionSources() != null && !searchDto.getTransactionSources().isEmpty()) {
            specs.add(hasTransactionSources(searchDto.getTransactionSources()));
            log.debug("Filtre appliqué: sources = {}", searchDto.getTransactionSources());
        }

        Specification<Transaction> combinedSpec = withFilters(specs);

        // 1. Calcul des totaux sur l'ensemble filtré
        List<Transaction> allFilteredTransactions = transactionRepository.findAll(combinedSpec);
        log.debug("Agrégation sur {} transactions correspondantes", allFilteredTransactions.size());

        BigDecimal totalDebit = allFilteredTransactions.stream()
                .map(t -> t.getDebit() != null ? t.getDebit() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredit = allFilteredTransactions.stream()
                .map(t -> t.getCredit() != null ? t.getCredit() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalTvaDeductible = allFilteredTransactions.stream()
                .map(Transaction::getTvaDeductible)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalTvaCollectee = allFilteredTransactions.stream()
                .map(Transaction::getTvaCollectee)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

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
}