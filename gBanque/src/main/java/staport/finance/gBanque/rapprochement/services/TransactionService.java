package staport.finance.gBanque.rapprochement.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import staport.finance.gBanque.rapprochement.dtos.TransactionResponseDto;
import staport.finance.gBanque.rapprochement.dtos.TransactionSearchDto;
import staport.finance.gBanque.rapprochement.entities.Transaction;
import staport.finance.gBanque.rapprochement.mapper.TransactionMapper;
import staport.finance.gBanque.rapprochement.repositories.TransactionRepository;
import java.util.ArrayList;
import java.util.List;

import static staport.finance.gBanque.rapprochement.spec.TransactionSpecifications.*;
import static staport.finance.gBanque.rapprochement.spec.TransactionSpecifications.withFilters;

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


        if (searchDto.getTransactionSource() != null && !searchDto.getTransactionSource().isEmpty()) {
            specs.add(hasTransactionSource(searchDto.getTransactionSource()));
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
     * filtered by the current month and optional criteria, and maps them to DTOs.
     * @param banqueId The ID of the bank to filter by (mandatory).
     * @param searchDto A DTO containing optional search criteria.
     * @param pageable The Pageable object for pagination and sorting.
     * @return A Page of Transaction DTOs.
     */
    public Page<TransactionResponseDto> getTransactionsByBanque(Long banqueId, TransactionSearchDto searchDto, Pageable pageable) {
        List<Specification<Transaction>> specs = new ArrayList<>();
        specs.add(isCurrentMonth());
        specs.add(hasBanqueId(banqueId));

        if (searchDto.getLibelle() != null && !searchDto.getLibelle().isEmpty()) {
            specs.add(libelleContains(searchDto.getLibelle()));
        }

        Specification<Transaction> combinedSpec = withFilters(specs);
        Page<Transaction> transactions = transactionRepository.findAll(combinedSpec, pageable);
        return transactions.map(TransactionMapper::toDto);
    }

}
