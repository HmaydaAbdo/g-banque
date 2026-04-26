package staport.finance.gBanque.cautions.dtos;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de modification d'une caution.
 *
 * <p>Tous les champs sont modifiables — y compris {@code numeroCaution},
 * {@code dateDePrise} et {@code banqueId}.
 *
 * <p>Règle métier : la modification est interdite lorsque la caution est inactive
 * ({@code actif = false}). Cette garde est appliquée au niveau service,
 * pas au niveau DTO.
 */
public record CautionUpdateDto(

    @NotNull(message = "L'identifiant de la banque est obligatoire")
    Long banqueId,

    @NotNull(message = "L'identifiant de la nature est obligatoire")
    Long natureId,

    /* Optionnel — peut être null pour dissocier la caution d'un marché. */
    Long marcheId,

    @NotNull(message = "La date de prise est obligatoire")
    @PastOrPresent(message = "La date de prise ne peut pas être dans le futur")
    LocalDate dateDePrise,

    @NotBlank(message = "Le numéro de caution est obligatoire")
    @Size(max = 100, message = "Le numéro de caution ne peut pas dépasser 100 caractères")
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
