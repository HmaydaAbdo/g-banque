package staport.finance.gBanque.decomptes.dtos.response;

import staport.finance.gBanque.prefinancement.enums.Status;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de réponse pour un décompte.
 */
public record DecompteResponseDto(

        Long id,

        // ── Marché ────────────────────────────────────────────────────────────
        Long marcheId,
        String marcheName,
        String marcheNumero,

        // ── Banque du marché ──────────────────────────────────────────────────
        Long marcheBanqueId,
        String marcheBanqueName,

        // ── Banque d'encaissement ─────────────────────────────────────────────
        Long banqueEncaissementId,
        String banqueEncaissementName,

        LocalDate dateEmission,
        BigDecimal montant,
        String observation,
        LocalDate dateOperation,
        LocalDate dateValeur,
        Status status,
        String libelle

) {}
