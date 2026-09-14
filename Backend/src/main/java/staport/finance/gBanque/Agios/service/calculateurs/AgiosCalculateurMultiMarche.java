package staport.finance.gBanque.Agios.service.calculateurs;

import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.marches.entities.Marche;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Extension de {@link AgiosCalculateur} pour les types d'agios calculés
 * par marché (ex : Préfinancement).
 *
 * <p>Un calculateur multi-marché produit, pour un jour donné, un résultat
 * distinct par marché actif — contrairement à la Facilité de Caisse qui
 * produit un seul résultat par (jour, banque).</p>
 *
 * <p>L'orchestrateur détecte cette interface et appelle
 * {@link #calculerJourParMarche} au lieu de {@link AgiosCalculateur#calculerJour}.</p>
 */
public interface AgiosCalculateurMultiMarche extends AgiosCalculateur {

    /**
     * Calcule les agios HT pour un jour donné, sur l'ensemble des marchés
     * actifs de la banque qui disposent d'une ligne de nature compatible.
     *
     * @param banque la banque traitée
     * @param jour   la date de valeur
     * @return liste de résultats (un par marché ayant un encours > 0 ou une ligne active)
     */
    List<ResultatParMarche> calculerJourParMarche(Banque banque, LocalDate jour);

    /**
     * Résultat journalier rattaché à un marché précis.
     *
     * @param marche  le marché auquel se rapporte le calcul
     * @param resultat le résultat HT/taux/base pour ce marché ce jour-là
     */
    record ResultatParMarche(Marche marche, AgiosCalculateur.ResultatJournalier resultat) {}

    /**
     * Non utilisé dans le flux multi-marché.
     * L'orchestrateur appelle {@link #calculerJourParMarche} à la place.
     */
    @Override
    default AgiosCalculateur.ResultatJournalier calculerJour(
            Banque banque, LocalDate jour, BigDecimal soldeJournalier) {
        return AgiosCalculateur.ResultatJournalier.ZERO;
    }
}
