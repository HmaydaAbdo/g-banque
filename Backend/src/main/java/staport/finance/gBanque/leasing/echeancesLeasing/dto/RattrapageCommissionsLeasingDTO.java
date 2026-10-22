package staport.finance.gBanque.leasing.echeancesLeasing.dto;

/**
 * Bilan du rattrapage des commissions leasing sur les échéances prévues existantes.
 *
 * @param contratsTraites     contrats ayant au moins une échéance prévue examinée
 * @param contratsSansRegle   contrats dont la banque / l'organisme n'a aucune règle de commission leasing
 * @param commissionsCreees   commissions créées par ce rattrapage
 * @param dejaPresentes       échéances qui avaient déjà leur commission (ignorées)
 */
public record RattrapageCommissionsLeasingDTO(
        int contratsTraites,
        int contratsSansRegle,
        int commissionsCreees,
        int dejaPresentes
) {}
