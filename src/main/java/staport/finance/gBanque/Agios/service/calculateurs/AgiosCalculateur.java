package staport.finance.gBanque.Agios.service.calculateurs;


import staport.finance.gBanque.Agios.enums.TypeAgios;
import staport.finance.gBanque.Bank.entities.Banque;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface AgiosCalculateur {

    /**
     * @return Le type d'agios géré par ce calculateur.
     */
    TypeAgios getType();

    /**
     * Détermine si une banque est éligible à ce type d'agios.
     *
     * @param banque la banque à tester
     * @return true si cette banque doit être traitée par ce calculateur
     */
    boolean estEligible(Banque banque);

    /**
     * Calcule l'agios HT pour un jour donné.
     *
     * @param banque          la banque concernée
     * @param jour            la date de valeur
     * @param soldeJournalier le solde reconstitué du compte à ce jour
     * @return le résultat du calcul pour ce jour
     */
    ResultatJournalier calculerJour(Banque banque, LocalDate jour, BigDecimal soldeJournalier);

    /**
     * Résultat du calcul d'un jour par un calculateur.
     */
    record ResultatJournalier(
            /** Base de calcul (découvert pour FC, montant escompté pour escompte...) */
            BigDecimal baseCalcul,
            /** Taux appliqué (en %) */
            BigDecimal tauxApplique,
            /** Agios HT du jour */
            BigDecimal agiosHT,
            /** Informations complémentaires (ID de ligne, détail...) */
            String metadata
    ) {
        /** Résultat nul (pas d'agios ce jour) */
        public static final ResultatJournalier ZERO = new ResultatJournalier(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null);
    }
}