package staport.finance.gBanque.leasing.contratCadre.dtos;

import staport.finance.gBanque.leasing.contratCadre.enums.StatutContratCadre;

/**
 * Critères de recherche pour la liste paginée des contrats cadres.
 *
 * @param organismeId Filtrer par organisme (optionnel).
 * @param statut      Filtrer par statut (optionnel).
 */
public record ContratCadreSearchDTO(
        Long organismeId,
        StatutContratCadre statut
) {}
