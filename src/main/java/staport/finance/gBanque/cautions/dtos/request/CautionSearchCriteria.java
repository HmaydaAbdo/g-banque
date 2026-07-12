package staport.finance.gBanque.cautions.dtos.request;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Critères de recherche dynamique pour les cautions.
 *
 * <p>Tous les champs sont optionnels — un champ {@code null} est simplement ignoré
 * par {@code CautionSpecification}.
 *
 * <p>Utilisé via {@code @ModelAttribute} dans le contrôleur pour le binding des
 * paramètres de requête HTTP.
 */
public record CautionSearchCriteria(

    /* Recherche partielle sur le numéro de caution (LIKE %valeur%). */
    String numeroCaution,

    /* Filtre exact sur la banque. */
    Long banqueId,

    /* Filtre exact sur la nature de la ligne fonctionnelle. */
    Long natureId,

    /* Filtre exact sur le marché associé. */
    Long marcheId,

    /* Filtre exact sur la date de prise (égalité). */
    LocalDate dateDePrise,

    /* Filtre exact sur le montant (égalité). */
    BigDecimal montant,

    /* Filtre sur le statut actif/inactif. {@code null} = tous. */
    Boolean actif,

    /* Recherche partielle sur l'organisme bénéficiaire. */
    String organisme

) {}
