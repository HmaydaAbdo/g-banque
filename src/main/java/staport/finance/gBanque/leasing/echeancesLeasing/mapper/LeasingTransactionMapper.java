package staport.finance.gBanque.leasing.echeancesLeasing.mapper;

import org.mapstruct.*;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.dto.LeasingTransactionRequestDTO;
import staport.finance.gBanque.leasing.echeancesLeasing.dto.LeasingTransactionResponseDTO;
import staport.finance.gBanque.leasing.echeancesLeasing.entities.EcheanceLeasing;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.validation.utils.TVAUtils;

import java.math.BigDecimal;

/**
 * Mapper MapStruct pour la conversion entre DTOs leasing et l'entité {@link Transaction}.
 * <p>
 * Note : importe {@code TVAUtils} depuis le package {@code validation.utils},
 * classe utilitaire stateless partagée entre tous les mappers de rapprochement.
 */
@Mapper(componentModel = "spring")
public abstract class LeasingTransactionMapper {

    /**
     * Construit une {@link Transaction} à partir du DTO de requête et des entités associées.
     * Le champ {@code credit} et {@code tvaDeductible} sont calculés dans {@link #computeFinancials}.
     */
    @Mapping(source = "requestDTO.observation",   target = "observation")
    @Mapping(source = "requestDTO.dateEmission",  target = "dateEmission")
    @Mapping(source = "requestDTO.dateValeur",    target = "dateValeur")
    @Mapping(source = "requestDTO.dateOperation", target = "dateOperations")
    @Mapping(source = "requestDTO.libelle",       target = "libelle")
    @Mapping(source = "echeanceLeasing.id",       target = "referent")
    @Mapping(source = "echeanceLeasing.montantTTC", target = "debit")
    @Mapping(source = "contratLeasing.banqueDePrelevement", target = "banqueDePrelevement")
    @Mapping(target = "transactionSource", constant = "LEASING")
    @Mapping(target = "credit",       ignore = true)
    @Mapping(target = "tvaDeductible", ignore = true)
    @Mapping(target = "tvaCollectee", ignore = true)
    @Mapping(target = "id",           ignore = true)
    @Mapping(target = "rapprochement", ignore = true)
    @Mapping(target = "createdAt",    ignore = true)
    @Mapping(target = "updatedAt",    ignore = true)
    public abstract Transaction toEntity(
            LeasingTransactionRequestDTO requestDTO,
            ContratLeasing contratLeasing,
            EcheanceLeasing echeanceLeasing);

    /**
     * Calcule {@code credit} (zéro) et {@code tvaDeductible} après le mapping principal.
     */
    @AfterMapping
    protected void computeFinancials(
            LeasingTransactionRequestDTO requestDTO,
            EcheanceLeasing echeanceLeasing,
            @MappingTarget Transaction transaction) {
        transaction.setCredit(BigDecimal.ZERO);
        BigDecimal tauxTva = requestDTO.tvaDeductible() != null
                ? BigDecimal.valueOf(requestDTO.tvaDeductible())
                : BigDecimal.ZERO;
        transaction.setTvaDeductible(
                tauxTva.compareTo(BigDecimal.ZERO) > 0
                        ? TVAUtils.calculerTVA(echeanceLeasing.getMontantTTC(), tauxTva)
                        : BigDecimal.ZERO);
    }

    /**
     * Convertit une {@link Transaction} en {@link LeasingTransactionResponseDTO}.
     * {@code echeanceId} est mappé depuis {@code referent} (clé de liaison).
     */
    @Mapping(source = "banqueDePrelevement.nom", target = "banqueName")
    @Mapping(source = "referent",               target = "echeanceId")
    @Mapping(source = "dateOperations",          target = "dateOperations")
    public abstract LeasingTransactionResponseDTO toResponseDTO(Transaction transaction);
}
