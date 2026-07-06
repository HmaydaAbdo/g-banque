package staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * DTO de réponse complet pour les lignes fonctionnelles spécifiques. Un seul DTO pour toutes les
 * vues ; {@code encours}/{@code solde} sont {@code null} pour les natures qui ne les calculent pas.
 *
 * @param expired vrai si {@code dateFin} est strictement antérieure à aujourd'hui.
 */
public record LigneFonctionnelleSpecifiqueResponse(
        Long id,
        Long banqueId,
        String banqueNom,
        Long natureLigneFonctionnelleId,
        String natureLigneFonctionnelleLabel,
        String natureLigneFonctionnelleDescription,
        BigDecimal plafond,
        BigDecimal encours,
        BigDecimal solde,
        BigDecimal taux,
        LocalDate dateDepart,
        LocalDate dateFin,
        OffsetDateTime dateCreation,
        OffsetDateTime dateModification,
        boolean actif,
        boolean expired,
        Long marcheId,
        String marcheNom,
        String marcheNumero
) {
}
