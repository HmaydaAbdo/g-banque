package staport.finance.gBanque.validation.dtos.lot;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import staport.finance.gBanque.transactions.enums.SensOperation;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.time.LocalDate;
import java.util.List;

/**
 * Requête de validation (rapprochement) en lot : applique les mêmes champs communs
 * (TVA, date opération, date valeur, rapprochement cible) à un ensemble d'opérations
 * de même type et même sens, en une seule opération atomique.
 * <p>
 * Volontairement absent de ce DTO : libellé, observation et date d'émission. Ces champs sont
 * propres à chaque opération (saisis à sa création) et ne doivent jamais être écrasés par une
 * valeur unique appliquée à tout le lot — contrairement à la TVA et aux dates d'opération/valeur,
 * qui proviennent du relevé bancaire traité et sont légitimement communes aux opérations d'un même
 * lot. Chaque opération du lot conserve donc automatiquement son propre libellé/observation/date
 * d'émission d'origine (voir les mappers de {@code validation.mapper}, {@code ICmtValidationService}
 * et {@code ILeasingValidationService} : fallback sur l'entité source si le DTO unitaire ne porte
 * pas ces champs).
 * <p>
 * {@code sens} n'est obligatoire que pour les types qui distinguent décaissement et encaissement
 * (Virement, Cheque, Lcn, MiseADisposition, VirementInternational) ; ignoré pour les autres types.
 * <p>
 * {@code banqueId} n'est obligatoire que pour les types dont l'endpoint unitaire l'exige
 * (Decompte, DroitConstate, Factoring, Prefinancement) ; ignoré pour les autres types.
 */
public record BatchValidationRequestDto(

        @NotNull(message = "La source de transaction est obligatoire")
        TransactionSource transactionSource,

        SensOperation sens,

        Long banqueId,

        @NotEmpty(message = "La liste des identifiants à valider est obligatoire et ne peut être vide")
        List<Long> ids,

        @NotNull(message = "La date de valeur est obligatoire")
        LocalDate dateValeur,

        @NotNull(message = "La date d'opération est obligatoire")
        LocalDate dateOperation,

        @NotNull(message = "La TVA déductible est obligatoire")
        Double tvaDeductible,

        @NotNull(message = "rapprochementId est obligatoire")
        Long rapprochementId

) {}
