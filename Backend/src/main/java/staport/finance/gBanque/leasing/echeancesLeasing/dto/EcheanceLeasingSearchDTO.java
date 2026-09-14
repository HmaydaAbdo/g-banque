package staport.finance.gBanque.leasing.echeancesLeasing.dto;

import org.springframework.format.annotation.DateTimeFormat;
import staport.finance.gBanque.leasing.echeancesLeasing.enums.StatutEcheanceLeasing;

import java.time.LocalDate;

/**
 * DTO utilisé pour les critères de recherche avancée des échéances.
 * Peut être utilisé pour filtrer le tableau d'amortissement ou pour une recherche globale.
 * Tous les champs sont optionnels. Pas de constructeur sans argument : Spring MVC lie ce
 * record aux query params GET via son constructeur canonique (binding implicite
 * @ModelAttribute, cf. EcheanceLeasingController) — cf. le même piège déjà rencontré et
 * documenté sur EcheanceCMTSearchDto.
 */
public record EcheanceLeasingSearchDTO(
        Long contratLeasingId,
        Integer numeroEcheance,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateEcheanceDebut,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateEcheanceFin,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateOperationDebut,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateOperationFin,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateValeurDebut,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateValeurFin,
        StatutEcheanceLeasing statutPaiement
) {}
