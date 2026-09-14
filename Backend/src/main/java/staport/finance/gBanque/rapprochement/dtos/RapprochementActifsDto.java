package staport.finance.gBanque.rapprochement.dtos;

import java.math.BigDecimal;

/**
 * Snapshot of the two non-definitive rapprochements that can coexist
 * for a given bank at any moment:
 *
 * <ul>
 *   <li><b>enCours</b> — always present (mandatory)</li>
 *   <li><b>provisoire</b> — present only when a CLOTURE_PROVISOIRE exists</li>
 * </ul>
 *
 * The frontend uses this DTO to build the rapprochement-selection page
 * and to determine which rapprochementId to send when validating operations.
 */
public record RapprochementActifsDto(

        // ── EN_COURS ─────────────────────────────────────────────────────────

        /** Always non-null. */
        Long    enCoursId,

        /** "YYYY-MM" formatted period, e.g. "2025-04". Always non-null. */
        String  enCoursPeriode,

        /** Opening balance of the EN_COURS rapprochement. */
        BigDecimal enCoursSoldeDebut,

        /** Computed closing balance (soldeDebut + credits - debits). */
        BigDecimal enCoursSoldeFin,

        /** Running total of debits in EN_COURS. */
        BigDecimal enCoursDebit,

        /** Running total of credits in EN_COURS. */
        BigDecimal enCoursCredit,

        /** Number of transactions in EN_COURS. */
        long enCoursNbTransactions,


        // ── CLOTURE_PROVISOIRE (nullable) ─────────────────────────────────────

        /** Null when no provisional rapprochement exists. */
        Long    provisoireId,

        /** Null when no provisional rapprochement exists. */
        String  provisoirePeriode,

        BigDecimal provisoireSoldeDebut,
        BigDecimal provisoireSoldeFin,
        BigDecimal provisoireDebit,
        BigDecimal provisoireCredit,
        long provisoireNbTransactions

) {}
