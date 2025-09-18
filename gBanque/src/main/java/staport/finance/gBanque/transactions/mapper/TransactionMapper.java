package staport.finance.gBanque.transactions.mapper;


import staport.finance.gBanque.transactions.dtos.TransactionResponseDto;
import staport.finance.gBanque.transactions.entities.Transaction;

public class TransactionMapper {

        /**
         * Converts a Transaction entity to a TransactionResponseDto.
         *
         * @param transaction The Transaction entity to convert.
         * @return The corresponding TransactionResponseDto.
         */
        public static TransactionResponseDto toDto(Transaction transaction) {
            if (transaction == null) {
                return null;
            }

            TransactionResponseDto dto = new TransactionResponseDto();
            dto.setId(transaction.getId());
            dto.setDateEmission(transaction.getDateEmission());
            dto.setDateValeur(transaction.getDateValeur());
            dto.setDateOperations(transaction.getDateOperations());
            dto.setObservation(transaction.getObservation());
            dto.setTvaDeductible( transaction.getTvaDeductible());
            dto.setTvaCollectee(transaction.getTvaCollectee());
            dto.setLibelle(transaction.getLibelle());
            dto.setTransactionSource(transaction.getTransactionSource());
            dto.setDebit(transaction.getDebit());
            dto.setCredit(transaction.getCredit());
            dto.setTraces(transaction.getTraces());
            dto.setCreatedAt(transaction.getCreatedAt());
            dto.setUpdatedAt(transaction.getUpdatedAt());

            // Gérer la relation BanqueDePrelevement
            if (transaction.getBanqueDePrelevement() != null) {
                dto.setBanqueDePrelevementId(transaction.getBanqueDePrelevement().getId());
                dto.setBanqueDePrelevementNom(transaction.getBanqueDePrelevement().getNom());
            }

            return dto;
        }
}
