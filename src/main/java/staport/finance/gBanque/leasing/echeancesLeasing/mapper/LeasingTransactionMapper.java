package staport.finance.gBanque.leasing.echeancesLeasing.mapper;

import org.mapstruct.*;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.dto.LeasingTransactionRequestDTO;
import staport.finance.gBanque.leasing.echeancesLeasing.dto.LeasingTransactionResponseDTO;
import staport.finance.gBanque.leasing.echeancesLeasing.entities.EcheanceLeasing;
import staport.finance.gBanque.transactions.entities.Transaction;

import java.math.BigDecimal;

/**
 * Mapper MapStruct pour la conversion entre DTOs leasing et l'entité {@link Transaction}.
 * <p>
 * Le montant de TVA est résolu par {@code LeasingValidationService} via {@code TvaResolver} et
 * transmis en {@code @Context} — le mapper ne fait que le poser sur {@code tvaDeductible}.
 */
@Mapper(componentModel = "spring")
public abstract class LeasingTransactionMapper {

    /**
     * Construit une {@link Transaction} à partir du DTO de requête et des entités associées.
     * Le champ {@code credit} et {@code tvaDeductible} sont calculés dans {@link #computeFinancials}.
     */
    @Mapping(target = "observation",  ignore = true)
    @Mapping(target = "dateEmission", ignore = true)
    @Mapping(source = "requestDTO.dateValeur",    target = "dateValeur")
    @Mapping(source = "requestDTO.dateOperation", target = "dateOperations")
    @Mapping(target = "libelle",      ignore = true)
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
            EcheanceLeasing echeanceLeasing,
            @Context BigDecimal montantTva);

    /**
     * Calcule {@code credit} et {@code tvaDeductible}, et applique le fallback observation/libelle/
     * dateEmission sur l'échéance d'origine quand ces champs ne sont pas fournis par le DTO (cas de
     * la validation en lot — voir {@code BatchValidationRequestDto}). Pour dateEmission, l'équivalent
     * sur l'échéance leasing est sa propre date d'échéance ({@code dateEcheance}), qui n'a pas de
     * champ "dateEmission" dédié.
     */
    @AfterMapping
    protected void computeFinancials(
            LeasingTransactionRequestDTO requestDTO,
            EcheanceLeasing echeanceLeasing,
            @Context BigDecimal montantTva,
            @MappingTarget Transaction transaction) {
        transaction.setObservation(requestDTO.observation() != null ? requestDTO.observation() : echeanceLeasing.getObservation());
        transaction.setLibelle(requestDTO.libelle() != null ? requestDTO.libelle() : echeanceLeasing.getLibelle());
        transaction.setDateEmission(requestDTO.dateEmission() != null ? requestDTO.dateEmission() : echeanceLeasing.getDateEcheance());

        transaction.setCredit(BigDecimal.ZERO);
        // Règles de saisie de la TVA (taux XOR montant, bornes) : logique métier, résolue par
        // LeasingValidationService via TvaResolver avant l'appel à ce mapper (conventions/architecture.md).
        transaction.setTvaDeductible(montantTva != null ? montantTva : BigDecimal.ZERO);
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
