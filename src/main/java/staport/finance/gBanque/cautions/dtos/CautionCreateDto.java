package staport.finance.gBanque.cautions.dtos;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de création d'une caution.
 *
 * <p>Les identifiants des associations ({@code banqueId}, {@code natureId}, {@code marcheId})
 * sont transmis par ID uniquement — jamais d'objets imbriqués.
 * {@code marcheId} est optionnel.
 */
public record CautionCreateDto(

    @NotNull(message = "L'identifiant de la banque est obligatoire")
    Long banqueId,

    @NotNull(message = "L'identifiant de la nature est obligatoire")
    Long natureId,

    /* Optionnel — la caution peut exister sans marché associé. */
    Long marcheId,

//    @NotNull(message = "La date de prise est obligatoire")
//    @PastOrPresent(message = "La date de prise ne peut pas être dans le futur")
    LocalDate dateDePrise,

    /* Optionnel à la création — renseigné lors de la levée de la caution. */
    LocalDate dateDeMainLevee,

//    @NotBlank(message = "Le numéro de caution est obligatoire")
//    @Size(max = 100, message = "Le numéro de caution ne peut pas dépasser 100 caractères")
    String numeroCaution,

    @NotBlank(message = "La désignation est obligatoire")
    String designation,

    @NotBlank(message = "L'organisme est obligatoire")
    String organisme,

    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "0.01", message = "Le montant doit être strictement positif")
    @Digits(integer = 17, fraction = 2, message = "Format montant invalide (max 17 entiers, 2 décimales)")
    BigDecimal montant,

    @NotNull(message = "Le taux est obligatoire")
    @DecimalMin(value = "0.000000", inclusive = true, message = "Le taux ne peut pas être négatif")
    @DecimalMax(value = "100.000000", message = "Le taux ne peut pas dépasser 100%")
    @Digits(integer = 3, fraction = 6, message = "Format taux invalide (max 3 entiers, 6 décimales)")
    BigDecimal taux

) {}
