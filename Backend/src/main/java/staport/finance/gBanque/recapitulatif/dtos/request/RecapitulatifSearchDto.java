package staport.finance.gBanque.recapitulatif.dtos.request;

import org.springframework.format.annotation.DateTimeFormat;
import staport.finance.gBanque.impot_taxes.enums.Motif;
import staport.finance.gBanque.transactions.enums.SensOperation;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Critères de recherche de l'écran Récapitulatif — encaissements et décaissements prévisionnels
 * du mois, réunis dans un seul écran depuis le 06/09/2026 (ce DTO s'appelait
 * {@code DecaissementSearchDto} du temps des deux écrans séparés).
 *
 * <p>Le {@code banqueId} n'est pas porté par ce DTO : il provient du {@code @PathVariable} de
 * l'endpoint et est transmis séparément au Service (record immuable, pas de mutation possible
 * après le binding {@code @ModelAttribute}).</p>
 *
 * <p>{@code sens} : filtre optionnel sur le sens de l'opération. {@code null} (valeur par défaut,
 * « Tous » côté écran) signifie « aucun filtre » — les deux sens sont retournés, entrelacés. Le
 * sens est un critère de recherche au même titre que les sources ou le libellé : il vit donc dans
 * ce DTO, et non dans la signature du Service, pour qu'il n'existe qu'une seule source de vérité
 * de ce critère.</p>
 *
 * <p>{@code dateReferenceDepuis}/{@code dateReferenceJusqua} : bornes optionnelles, chacune
 * indépendante de l'autre, appliquées sur la colonne {@code dateReference} de la vue
 * {@code v_recapitulatif_encours} — cette colonne est déjà calculée par source dans la vue SQL
 * (date d'émission pour certaines sources, date d'échéance pour d'autres, etc.), donc filtrer
 * dessus revient à filtrer chaque ligne sur SA colonne de référence métier, sans qu'on ait à
 * connaître ce mapping ici. {@code dateReferenceDepuis} n'a pas de valeur par défaut (absente =
 * aucune borne basse). Si {@code dateReferenceJusqua} est {@code null}, le Service retombe sur la
 * fin du mois en cours (comportement historique, inchangé par l'ajout de la borne basse).</p>
 *
 * <p>{@code montant} : filtre optionnel sur le montant exact de l'opération (égalité stricte,
 * pas une plage) — permet par exemple de retrouver directement une échéance dont le montant est
 * déjà connu, dans un tableau désormais plus dense depuis que les deux sens y sont entrelacés.</p>
 *
 * <p>{@code libelleOuObservation} : un seul champ de recherche textuelle, testé sur les colonnes
 * {@code libelle}, {@code observation} ET {@code recherche_complementaire} (OR, insensible à la
 * casse, sous-chaîne) — fusionné le 21/09/2026 depuis deux champs séparés (demande HMAYDA :
 * l'utilisateur ne sait souvent pas dans laquelle des deux colonnes se trouve le terme recherché).
 * {@code recherche_complementaire} (colonne invisible à l'écran, voir {@code
 * v_recapitulatif_encours.sql}) restaure des critères qui existaient dans les anciens écrans par
 * source et étaient perdus par la vue selon le sens (émetteur pour Virement/Chèque/LCN/
 * VirementInternational/MiseADisposition, organisme pour Leasing).</p>
 *
 * <p>{@code commissionOperation}/{@code impotEtTaxeMotif} : filtres optionnels exacts, chacun ne
 * s'appliquant qu'aux lignes de sa propre source (COMMISSION / IMPOT_TAXE respectivement) — une
 * ligne d'une autre source n'est jamais exclue par ces deux critères. Restaurent le filtre
 * « Motif » des anciens écrans dédiés à ces deux sources (ajoutés le 21/09/2026).</p>
 */
public record RecapitulatifSearchDto(
        List<TransactionSource> transactionSources,
        String libelleOuObservation,
        String numero,
        SensOperation sens,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateReferenceDepuis,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateReferenceJusqua,
        BigDecimal montant,
        TransactionSource commissionOperation,
        Motif impotEtTaxeMotif
) {
}
