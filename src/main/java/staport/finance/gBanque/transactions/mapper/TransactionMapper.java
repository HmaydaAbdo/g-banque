package staport.finance.gBanque.transactions.mapper;


import staport.finance.gBanque.rapprochement.enums.StatutRapprochement;
import staport.finance.gBanque.transactions.dtos.TransactionResponseDto;
import staport.finance.gBanque.transactions.entities.Transaction;

import java.math.BigDecimal;

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
            dto.setObservation( toUpper(transaction.getObservation()) );
            dto.setTvaDeductible( transaction.getTvaDeductible()!=null ? transaction.getTvaDeductible() : BigDecimal.ZERO);
            dto.setTvaCollectee(transaction.getTvaCollectee() != null ? transaction.getTvaCollectee() : BigDecimal.ZERO);
            dto.setLibelle( toUpper(transaction.getLibelle()) );
            dto.setTransactionSource(transaction.getTransactionSource());
            dto.setDebit(transaction.getDebit() != null ? transaction.getDebit() : BigDecimal.ZERO);
            dto.setCredit(transaction.getCredit() !=null ? transaction.getCredit() : BigDecimal.ZERO);
            dto.setCreatedAt(transaction.getCreatedAt());
            dto.setUpdatedAt(transaction.getUpdatedAt());
            dto.setStatutRapprochement(
                    transaction.getRapprochement() != null
                            ? transaction.getRapprochement().getStatut()
                            : StatutRapprochement.EN_COURS
            );
            dto.setReferent(transaction.getReferent());
            // Gérer la relation Banque
            if (transaction.getBanqueDePrelevement() != null) {
                dto.setBanqueDePrelevementId(transaction.getBanqueDePrelevement().getId());
                dto.setBanqueDePrelevementNom(toUpper(transaction.getBanqueDePrelevement().getNom()));
            }

            return dto;
        }

        private static String toUpper(String value) {
            return value != null ? value.toUpperCase() : null;
        }
}
