package staport.finance.gBanque.transactions.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
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
    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }



    /**
     * Retrieves a paginated and sortable history of all transactions,
     * filtered by criteria provided in the search DTO.
     *
     * @param searchDto A DTO containing optional search criteria.
     * @param pageable The Pageable object for pagination and sorting.
     * @return A Page of Transaction DTOs.
     */
    public Page<TransactionResponseDto> getTransactionHistory(TransactionSearchDto searchDto, Pageable pageable) {
        List<Specification<Transaction>> specs = new ArrayList<>();

        // Apply filters from the DTO only if the values are present.
        if (searchDto.getLibelle() != null && !searchDto.getLibelle().isEmpty()) {
            specs.add(libelleContains(searchDto.getLibelle()));
        }

        if (searchDto.getBanqueId() != null) {
            specs.add(hasBanqueId(searchDto.getBanqueId()));
        }


        if (searchDto.getTransactionSources() != null && !searchDto.getTransactionSources().isEmpty()) {
            specs.add(hasTransactionSources(searchDto.getTransactionSources()));
        }

        // Combine all specifications with an 'AND' clause
        Specification<Transaction> combinedSpec = withFilters(specs);

        // Retrieve the paginated and sorted results from the repository
        Page<Transaction> transactions = transactionRepository.findAll(combinedSpec, pageable);

        // Map the entity page to a DTO page and return
        return transactions.map(TransactionMapper::toDto);
    }


    /**
     * Retrieves a paginated list of transactions for a specific bank,
     * filtered by the current month and optional criteria, and returns them
     * with aggregated totals.
     * @param banqueId The ID of the bank to filter by (mandatory).
     * @param searchDto A DTO containing optional search criteria.
     * @param pageable The Pageable object for pagination and sorting.
     * @return An AggregatedTransactionResponseDto containing the paginated
     * transactions and the total aggregated values.
     */
    public AgreratedTransactionResponseDto getTransactionsByBanque(Long banqueId, TransactionSearchDto searchDto, Pageable pageable) {
        List<Specification<Transaction>> specs = new ArrayList<>();
        specs.add(isConfirmed(false));
        specs.add(hasBanqueId(banqueId));

        if (searchDto.getLibelle() != null && !searchDto.getLibelle().isEmpty()) {
            specs.add(libelleContains(searchDto.getLibelle()));
        }

        if (searchDto.getObservation() != null && !searchDto.getObservation().isEmpty()) {
            specs.add(observationContains(searchDto.getObservation()));
        }
        if (searchDto.getTransactionSources() != null && !searchDto.getTransactionSources().isEmpty()) {
            specs.add(hasTransactionSources(searchDto.getTransactionSources()));
        }

        Specification<Transaction> combinedSpec = withFilters(specs);

        // 1. Fetch ALL transactions that match the combined specification.
        // This is necessary to calculate the totals for the entire filtered dataset.
        List<Transaction> allFilteredTransactions = transactionRepository.findAll(combinedSpec);

        // 2. Perform the aggregation on the complete filtered list.
        BigDecimal totalDebit = allFilteredTransactions.stream()
                .map(Transaction::getDebit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredit = allFilteredTransactions.stream()
                .map(Transaction::getCredit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalTvaDeductible = allFilteredTransactions.stream()
                .map(Transaction::getTvaDeductible)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalTvaCollectee = allFilteredTransactions.stream()
                .map(Transaction::getTvaCollectee)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Fetch the paginated results specifically for the current page.
        Page<Transaction> paginatedTransactions = transactionRepository.findAll(combinedSpec, pageable);

        // 4. Map the paginated transactions to DTOs.
        Page<TransactionResponseDto> paginatedDtos = paginatedTransactions.map(TransactionMapper::toDto);

        // 5. Create and populate the AggregatedTransactionResponseDto.
        AgreratedTransactionResponseDto aggregatedResponse = new AgreratedTransactionResponseDto();
        aggregatedResponse.setPage(paginatedDtos);
        aggregatedResponse.setTotalDebit(totalDebit);
        aggregatedResponse.setTotalCredit(totalCredit);
        aggregatedResponse.setTotalTvaDeductible(totalTvaDeductible);
        aggregatedResponse.setTotalTvaCollectee(totalTvaCollectee);

        return aggregatedResponse;
    }
}
