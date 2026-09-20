package staport.finance.gBanque.validation.dtos.lot;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import staport.finance.gBanque.transactions.enums.SensOperation;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Requête de validation (rapprochement) — un point d'entrée unique pour toutes les sources, qu'il
 * s'agisse de valider une seule opération ({@code ids} à un élément) ou un lot ({@code ids} à
 * plusieurs éléments). Voir {@code conventions/architecture.md}, Règle 1 (révisée le 19/09/2026) :
 * valider une opération n'est qu'un cas particulier de valider un lot de taille 1, il n'existe donc
 * plus d'endpoint ni de DTO unitaire séparé.
 * <p>
 * {@code tauxTva}, {@code dateValeur}, {@code dateOperation} et {@code rapprochementId} sont
 * toujours communs à toutes les opérations de la requête (qu'il y en ait une ou N) — ils
 * proviennent du relevé bancaire traité, jamais de l'opération elle-même.
 * <p>
 * {@code montantTva}, {@code observation} et {@code libelle} sont en revanche propres à une
 * opération : les autoriser sur un vrai lot écraserait des valeurs différentes d'une opération à
 * l'autre avec une seule valeur commune, ce qui n'a pas de sens (ni comptable pour le montant de
 * TVA, ni métier pour le libellé/l'observation). {@link staport.finance.gBanque.validation.services.BatchValidationService}
 * refuse donc la requête si l'un de ces trois champs est renseigné alors que {@code ids} contient
 * plusieurs éléments. Quand ils sont absents (vrai lot, ou opération unique sans édition), chaque
 * {@code BatchValidationHandler} retombe sur l'entité source via son mapper — {@code dateEmission}
 * n'a pas besoin d'un tel champ ici : le dialogue de validation unitaire la verrouille toujours sur
 * la valeur de l'entité (politique actée le 19/09/2026), le fallback sur l'entité produit donc déjà
 * exactement le même résultat.
 * <p>
 * {@code sens} n'est obligatoire que pour les types qui distinguent décaissement et encaissement
 * (Virement, Cheque, Lcn, MiseADisposition, VirementInternational) ; ignoré pour les autres types.
 * <p>
 * {@code banqueId} n'est obligatoire que pour les types dont la validation l'exige explicitement
 * (Decompte, DroitConstate, Factoring, Prefinancement) ; ignoré pour les autres types.
 */
public record BatchValidationRequestDto(

        @NotNull(message = "La source de transaction est obligatoire")
        TransactionSource transactionSource,

        SensOperation sens,

        Long banqueId,

        @NotEmpty(message = "La liste des identifiants à valider est obligatoire et ne peut être vide")
        List<Long> ids,

        @NotNull(message = "La date de valeur est obligatoire")
        LocalDate dateValeur,

        @NotNull(message = "La date d'opération est obligatoire")
        LocalDate dateOperation,

        Double tauxTva,

        /**
         * Montant de TVA en saisie libre — n'autorisé que si {@code ids} a un seul élément.
         * Exclusif de {@code tauxTva} (voir {@link #isSaisieTvaCoherente()}) : sur un vrai lot,
         * {@code tauxTva} seul est accepté (un montant fixe appliqué à N opérations de montants
         * différents n'a pas de sens comptable) ; sur une opération unique, l'appelant choisit
         * l'un ou l'autre, comme sur tous les DTO unitaires historiques.
         */
        BigDecimal montantTva,

        /** Observation propre à l'opération — n'autorisée que si {@code ids} a un seul élément. */
        String observation,

        /** Libellé propre à l'opération — n'autorisé que si {@code ids} a un seul élément. */
        String libelle,

        @NotNull(message = "rapprochementId est obligatoire")
        Long rapprochementId

) {
    /**
     * Exactement l'un des deux — {@code tauxTva} ou {@code montantTva} — doit être renseigné,
     * jamais les deux ni aucun. Même contrat que {@code TvaResolver}/les DTO unitaires historiques.
     */
    @JsonIgnore
    @AssertTrue(message = "Renseigner soit un taux de TVA, soit un montant de TVA, mais pas les deux")
    public boolean isSaisieTvaCoherente() {
        return (tauxTva == null) ^ (montantTva == null);
    }
}
