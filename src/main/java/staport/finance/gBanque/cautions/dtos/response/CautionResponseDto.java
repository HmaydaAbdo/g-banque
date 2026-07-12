package staport.finance.gBanque.cautions.dtos.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de réponse immutable pour le module Caution.
 *
 * <p>Expose uniquement les données nécessaires au client — jamais les entités JPA directement.
 * Les associations ({Banque}, {Nature}, {Marche})
 * sont projetées sous forme de champs plats (ID + libellé) pour éviter le sur-chargement
 * et les problèmes de sérialisation.
 */
public record CautionResponseDto(

    Long id,

    // ── Banque ────────────────────────────────────────────────────────────────
    Long    banqueId,
    String  banqueNom,

    // ── Nature ────────────────────────────────────────────────────────────────
    Long    natureId,
    String  natureLabel,
    String  natureCode,

    // ── Marché (optionnel) ───────────────────────────────────────────────────
    Long    marcheId,
    String  marcheNumero,
    String  marcheNom,

    // ── Données caution ──────────────────────────────────────────────────────
    LocalDate dateDePrise,
    LocalDate dateDeMainLevee,
    String    numeroCaution,
    String    designation,
    String    organisme,
    BigDecimal montant,
    BigDecimal taux,
    Boolean    actif,

    // ── Audit ────────────────────────────────────────────────────────────────
    LocalDate createdAt,
    LocalDate updatedAt

) {}
