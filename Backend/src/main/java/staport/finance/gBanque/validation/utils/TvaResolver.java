package staport.finance.gBanque.validation.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Résout le montant de TVA d'une opération à valider, à partir de l'une des deux saisies
 * possibles et mutuellement exclusives du formulaire de rapprochement :
 *
 * <ul>
 *   <li>un <strong>taux</strong> choisi dans la liste déroulante ({@code tauxTva}, en pourcentage) :
 *       le montant est alors extrait du TTC par {@link TVAUtils#calculerTVA} ;</li>
 *   <li>un <strong>montant</strong> saisi directement par l'utilisateur ({@code montantTvaSaisi},
 *       en MAD) : il est repris tel quel, aucune conversion.</li>
 * </ul>
 *
 * <p>Exactement l'un des deux doit être renseigné. Le calcul reste intégralement serveur : le
 * frontend ne transmet jamais un montant dérivé d'un taux, il transmet le taux et laisse cette
 * classe convertir.
 *
 * <p>À appeler depuis le <strong>service</strong> de validation de chaque module, jamais depuis un
 * mapper : ces règles sont de la logique métier (voir {@code conventions/architecture.md} —
 * « Mapper : conversion uniquement »). Le mapper reçoit le montant déjà résolu et se contente de le
 * poser sur {@code tvaDeductible} ou {@code tvaCollectee} selon le sens de l'opération.
 */
public final class TvaResolver {

    private static final Logger log = LoggerFactory.getLogger(TvaResolver.class);

    private static final BigDecimal TAUX_MIN = BigDecimal.ZERO;
    private static final BigDecimal TAUX_MAX = BigDecimal.valueOf(100);

    private TvaResolver() {
    }

    /**
     * @param montantTtc      montant TTC de l'opération (jamais {@code null})
     * @param tauxTva         taux choisi, en pourcentage (ex : 20 pour 20 %) — {@code null} si
     *                        l'utilisateur a saisi un montant
     * @param montantTvaSaisi montant de TVA saisi directement, en MAD — {@code null} si
     *                        l'utilisateur a choisi un taux
     * @return le montant de TVA à porter sur la transaction, arrondi à 2 décimales
     * @throws ValidationException si aucune ou les deux saisies sont renseignées, si le taux est
     *                             hors bornes, ou si le montant saisi est négatif ou supérieur au TTC
     */
    public static BigDecimal resoudre(BigDecimal montantTtc, Double tauxTva, BigDecimal montantTvaSaisi) {

        if (montantTtc == null) {
            throw new ValidationException(
                    "Le montant de l'opération est introuvable : impossible de déterminer la TVA.");
        }

        boolean parTaux = tauxTva != null;
        boolean parMontant = montantTvaSaisi != null;

        if (parTaux == parMontant) {
            throw new ValidationException(parTaux
                    ? "Renseigner soit un taux de TVA, soit un montant de TVA, mais pas les deux."
                    : "La TVA est obligatoire : renseigner un taux ou un montant.");
        }

        if (parMontant) {
            if (montantTvaSaisi.signum() < 0) {
                throw new ValidationException("Le montant de TVA saisi ne peut pas être négatif.");
            }
            if (montantTvaSaisi.compareTo(montantTtc) > 0) {
                throw new ValidationException(
                        "Le montant de TVA saisi (" + montantTvaSaisi.toPlainString()
                                + ") ne peut pas dépasser le montant de l'opération ("
                                + montantTtc.toPlainString() + ").");
            }
            log.debug("TVA saisie directement : {}", montantTvaSaisi);
            return montantTvaSaisi.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal taux = BigDecimal.valueOf(tauxTva);
        if (taux.compareTo(TAUX_MIN) < 0 || taux.compareTo(TAUX_MAX) > 0) {
            throw new ValidationException(
                    "Le taux de TVA doit être compris entre 0 et 100 (reçu : " + tauxTva + ").");
        }
        return TVAUtils.calculerTVA(montantTtc, taux);
    }
}
