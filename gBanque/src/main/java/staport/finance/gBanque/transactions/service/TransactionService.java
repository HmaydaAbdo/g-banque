package staport.finance.gBanque.transactions.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.transactions.dtos.AgreratedTransactionResponseDto;
import staport.finance.gBanque.transactions.dtos.TransactionResponseDto;
import staport.finance.gBanque.transactions.dtos.TransactionSearchDto;
import staport.finance.gBanque.transactions.dtos.TransactionUpdateRequestDto;
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
    private final BanqueRepository banqueRepository;

    public TransactionService(TransactionRepository transactionRepository, BanqueRepository banqueRepository) {
        this.transactionRepository = transactionRepository;
        this.banqueRepository = banqueRepository;
    }

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

        BigDecimal tvaDue=totalTvaCollectee.subtract(totalTvaDeductible);
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
        aggregatedResponse.setTvaDue(tvaDue);
        return aggregatedResponse;
    }

    public TransactionResponseDto updateTransaction(Long id, TransactionUpdateRequestDto dto) {

        // 1. Vérifier transaction existe
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction with ID " + id + " not found"));

        // 2. Mettre à jour tous les champs
        transaction.setDateEmission(dto.getDateEmission());
        transaction.setDateValeur(dto.getDateValeur());
        transaction.setDateOperations(dto.getDateOperations());

        transaction.setLibelle(dto.getLibelle());
        transaction.setObservation(dto.getObservation());

        transaction.setTvaDeductible(dto.getTvaDeductible());
        transaction.setTvaCollectee(dto.getTvaCollectee());

        transaction.setTransactionSource(dto.getTransactionSource());

        transaction.setDebit(dto.getDebit());
        transaction.setCredit(dto.getCredit());

        // 3. Mise à jour banque
        if (dto.getBanqueId() != null) {
            Banque banque = banqueRepository.findById(dto.getBanqueId())
                    .orElseThrow(() -> new ResourceNotFoundException("Banque with ID " + dto.getBanqueId() + " not found"));
            transaction.setBanqueDePrelevement(banque);
        }

        // 4. Sauvegarde
        Transaction saved = transactionRepository.save(transaction);

        // 5. Convertir en DTO response
        return TransactionMapper.toDto(saved);
    }

    public TransactionResponseDto getTransactionById(Long id) {
        return transactionRepository.findById(id)
                .map(TransactionMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction with ID " + id + " not found"));
    }

    public void deleteTransactionById(Long id) {
        if (!transactionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Transaction with ID " + id + " not found");
        }
        transactionRepository.deleteById(id);
    }


}
