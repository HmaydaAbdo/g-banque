package staport.finance.gBanque.LigneFonctionnelle.generalLignes.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Représentation d'une ligne fonctionnelle générale (ligne mère, sous-ligne, ou indépendante),
 * encours/solde inclus (calculés à la volée, jamais stockés).
 *
 * @param expired       vrai si {@code dateFin} est strictement antérieure à aujourd'hui.
 * @param ligneMereId   renseigné uniquement si {@code estSousLigne} est vrai.
 * @param sousLignes    renseigné uniquement si {@code estLigneMere} est vrai (sinon liste vide).
 */
public record LigneFonctionnelleResponse(
        Long id,
        Long banqueId,
        String banqueNom,
        Long natureLigneFonctionnelleId,
        String natureLigneFonctionnelleLabel,
        String natureLigneFonctionnelleDescription,
        List<MarcheRefDto> marches,
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
        Long ligneMereId,
        List<SousLigneResponseDto> sousLignes,
        boolean estSousLigne,
        boolean estLigneMere
) {
}
