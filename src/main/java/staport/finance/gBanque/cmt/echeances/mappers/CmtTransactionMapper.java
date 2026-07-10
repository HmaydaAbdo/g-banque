package staport.finance.gBanque.cmt.echeances.mappers;

import org.mapstruct.*;
import staport.finance.gBanque.cmt.echeances.dtos.CmtTransactionRequestDto;
import staport.finance.gBanque.cmt.echeances.dtos.CmtTransactionResponseDto;
import staport.finance.gBanque.cmt.echeances.entities.EcheanceCMT;
import staport.finance.gBanque.cmt.entities.CreditMoyenTerme;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.validation.utils.TVAUtils;

import java.math.BigDecimal;

/**
 * Mapper MapStruct pour la conversion entre DTOs CMT et l'entité {@link Transaction}.
 * <p>
 * Note : importe {@code TVAUtils} depuis le package {@code validation.utils},
 * classe utilitaire stateless partagée entre tous les mappers de rapprochement.
 */
@Mapper(componentModel = "spring")
public abstract class CmtTransactionMapper {

    /**
     * Construit une {@link Transaction} à partir du DTO de requête et des entités associées.
     * Le champ {@code credit} et {@code tvaDeductible} sont calculés dans {@link #computeFinancials}.
     */
    @Mapping(target = "observation",  ignore = true)
    @Mapping(target = "libelle",      ignore = true)
    @Mapping(target = "dateEmission", ignore = true)
    @Mapping(source = "requestDto.dateValeur",    target = "dateValeur")
    @Mapping(source = "requestDto.dateOperation", target = "dateOperations")
    @Mapping(source = "echeanceCMT.id",           target = "referent")
    @Mapping(source = "echeanceCMT.montant",      target = "debit")
    @Mapping(source = "creditMoyenTerme.banque",  target = "banqueDePrelevement")
    @Mapping(target = "transactionSource", constant = "CMT")
    @Mapping(target = "credit",        ignore = true)
    @Mapping(target = "tvaDeductible", ignore = true)
    @Mapping(target = "tvaCollectee",  ignore = true)
    @Mapping(target = "id",            ignore = true)
    @Mapping(target = "rapprochement", ignore = true)
    @Mapping(target = "createdAt",     ignore = true)
    @Mapping(target = "updatedAt",     ignore = true)
    public abstract Transaction toEntity(
            CmtTransactionRequestDto requestDto,
            CreditMoyenTerme creditMoyenTerme,
            EcheanceCMT echeanceCMT);

    /**
     * Calcule {@code credit} et {@code tvaDeductible}, et applique le fallback observation/libelle/
     * dateEmission sur l'échéance d'origine quand ces champs ne sont pas fournis par le DTO (cas de
     * la validation en lot — voir {@code BatchValidationRequestDto}). Pour dateEmission, l'équivalent
     * sur l'échéance CMT est sa propre date d'échéance ({@code dateEcheance}), qui n'a pas de champ
     * "dateEmission" dédié.
     */
    @AfterMapping
    protected void computeFinancials(
            CmtTransactionRequestDto requestDto,
            EcheanceCMT echeanceCMT,
            @MappingTarget Transaction transaction) {
        transaction.setObservation(requestDto.observation() != null ? requestDto.observation() : echeanceCMT.getObservation());
        transaction.setLibelle(requestDto.libelle() != null ? requestDto.libelle() : echeanceCMT.getLibelle());
        transaction.setDateEmission(requestDto.dateEmission() != null ? requestDto.dateEmission() : echeanceCMT.getDateEcheance());

        transaction.setCredit(BigDecimal.ZERO);
        BigDecimal tauxTva = requestDto.tvaDeductible() != null
                ? BigDecimal.valueOf(requestDto.tvaDeductible())
                : BigDecimal.ZERO;
        transaction.setTvaDeductible(
                tauxTva.compareTo(BigDecimal.ZERO) > 0
                        ? TVAUtils.calculerTVA(echeanceCMT.getMontant(), tauxTva)
                        : BigDecimal.ZERO);
    }

    /**
     * Convertit une {@link Transaction} en {@link CmtTransactionResponseDto}.
     * {@code echeanceId} est mappé depuis {@code referent} (clé de liaison).
     */
    @Mapping(source = "banqueDePrelevement.nom", target = "banqueName")
    @Mapping(source = "referent",                target = "echeanceId")
    public abstract CmtTransactionResponseDto toResponseDto(Transaction transaction);
}
