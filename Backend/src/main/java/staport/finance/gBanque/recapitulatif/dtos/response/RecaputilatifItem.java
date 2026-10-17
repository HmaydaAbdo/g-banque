package staport.finance.gBanque.recapitulatif.dtos.response;

import staport.finance.gBanque.transactions.enums.SensOperation;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Ligne unifiée du récapitulatif (décaissement ou encaissement), projetée directement depuis
 * {@code RecapitulatifEncoursView} (vue SQL {@code v_recapitulatif_encours}) via
 * {@code RecapitulatifEncoursMapper}.
 *
 * <p>Les colonnes {@code debit}/{@code credit} restent exclusives l'une de l'autre (une seule des
 * deux est renseignée par ligne, selon le sens) pour préserver le contrat JSON consommé par
 * {@code RecaputilatifItem.ts} côté Angular, qui distingue toujours les deux colonnes dans son
 * tableau.</p>
 *
 * <p>{@code sens} est toujours renseigné ({@code ENCAISSEMENT} ou {@code DECAISSEMENT}), jamais
 * {@code null} : depuis la fusion des deux écrans Récapitulatif en un seul (06/09/2026), les
 * lignes des deux sens sont entrelacées dans le même tableau, et chaque ligne doit donc porter son
 * sens explicitement plutôt que de le laisser déduire de celle des deux colonnes de montant qui
 * est remplie. {@code null} n'a de sens qu'au niveau du filtre de recherche
 * ({@code RecapitulatifSearchDto.sens} = tous sens confondus), jamais au niveau d'une ligne.</p>
 *
 * <p>{@code dateReference} est la date qui range la ligne dans le temps et pilote l'horizon (émission pour la plupart
 * des sources, échéance pour le leasing, le CMT et les LCN — voir la vue SQL). Elle est toujours renseignée, contrairement
 * à {@code dateEcheance}, vide pour les sources sans échéance : le popup des encours de l'accueil s'en sert pour ses périodes.</p>
 */
public record RecaputilatifItem(
        Long id,
        LocalDate dateEmission,
        LocalDate dateEcheance,
        String libelle,
        String numero,
        SensOperation sens,
        BigDecimal debit,
        BigDecimal credit,
        String observations,
        TransactionSource source,
        LocalDate dateReference
) {
}
