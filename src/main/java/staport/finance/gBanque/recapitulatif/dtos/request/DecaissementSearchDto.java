package staport.finance.gBanque.recapitulatif.dtos.request;

import org.springframework.format.annotation.DateTimeFormat;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.time.LocalDate;
import java.util.List;

/**
 * Critères de recherche pour les endpoints de récapitulatif (décaissement/encaissement).
 * Le {@code banqueId} n'est pas porté par ce DTO : il provient du {@code @PathVariable} de
 * l'endpoint et est transmis séparément au Service (record immuable, pas de mutation possible
 * après le binding {@code @ModelAttribute}).
 *
 * <p>{@code dateReferenceJusqua} : borne supérieure optionnelle appliquée sur la colonne
 * {@code dateReference} de la vue {@code v_recapitulatif_encours} — cette colonne est déjà
 * calculée par source dans la vue SQL (date d'émission pour certaines sources, date d'échéance
 * pour d'autres, etc.), donc filtrer dessus revient à filtrer chaque ligne sur SA colonne de
 * référence métier, sans qu'on ait à connaître ce mapping ici. Si {@code null}, le Service
 * retombe sur la fin du mois en cours (comportement historique).</p>
 */
public record DecaissementSearchDto(
        List<TransactionSource> transactionSources,
        String libelle,
        String observation,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateReferenceJusqua
) {
}
