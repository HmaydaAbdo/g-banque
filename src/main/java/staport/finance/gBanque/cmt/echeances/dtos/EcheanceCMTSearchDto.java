package staport.finance.gBanque.cmt.echeances.dtos;

import org.springframework.format.annotation.DateTimeFormat;
import staport.finance.gBanque.cmt.echeances.enums.StatutEcheanceCMT;

import java.time.LocalDate;

/**
 * DTO utilisé pour la recherche et le filtrage des échéances de crédits à moyen terme.
 * Tous les champs sont optionnels pour permettre des requêtes flexibles.
 * {@code creditMoyenTermeId} est renseigné par le Service (pas par l'appelant HTTP).
 */
public record EcheanceCMTSearchDto(
        Long creditMoyenTermeId,
        Integer numeroEcheance,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateEcheanceDebut,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateEcheanceFin,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateOperationDebut,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateOperationFin,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateValeurDebut,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateValeurFin,
        StatutEcheanceCMT statutPaiement
) {

    // Pas de constructeur sans argument ici : Spring MVC lie ce record aux query params GET
    // via son constructeur canonique (binding implicite @ModelAttribute, cf. EcheanceCMTController).
    // Un constructeur sans argument supplémentaire fait basculer le DataBinder sur celui-ci —
    // il instancie alors le DTO avec tous les champs à null, quels que soient les paramètres
    // envoyés : c'était la cause du bug "les filtres du tableau d'amortissement n'ont aucun effet"
    // (corrigé le 2026-07-21). Ne jamais réintroduire de constructeur sans argument sur ce DTO.

    /**
     * Retourne une copie de ce DTO avec l'ID du crédit renseigné.
     * Les DTOs étant immuables, cette méthode remplace l'ancien {@code setCreditMoyenTermeId}.
     */
    public EcheanceCMTSearchDto withCreditMoyenTermeId(Long creditMoyenTermeId) {
        return new EcheanceCMTSearchDto(creditMoyenTermeId, numeroEcheance,
                dateEcheanceDebut, dateEcheanceFin, dateOperationDebut, dateOperationFin,
                dateValeurDebut, dateValeurFin, statutPaiement);
    }
}
