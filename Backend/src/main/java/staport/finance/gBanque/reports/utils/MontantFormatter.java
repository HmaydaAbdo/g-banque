package staport.finance.gBanque.reports.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Format comptable français des montants, commun à tous les exports (PDF et Excel) : milliers séparés
 * par une espace, virgule décimale, deux décimales — {@code 1 234 567,89}.
 *
 * <p>Le séparateur de milliers est une espace normale (et non l'espace fine insécable U+202F du format
 * français de Java), que la police Helvetica des PDF ne sait pas afficher.
 */
public final class MontantFormatter {

    /** Valeur affichée quand le montant est absent. */
    public static final String MONTANT_ABSENT = "—";

    /**
     * Format de cellule Excel « comptable » pour les montants numériques (les cellules restent des
     * nombres : sommables et triables). Excel en affiche les séparateurs selon les paramètres régionaux
     * du poste : {@code 1 234 567,89} sur un poste en français.
     */
    public static final String EXCEL_FORMAT = "_-* #,##0.00_-;-* #,##0.00_-;_-* 0.00_-;_-@_-";

    private MontantFormatter() {
    }

    /** Formate un montant en {@code 1 234 567,89} ; {@value #MONTANT_ABSENT} si le montant est absent. */
    public static String format(BigDecimal montant) {
        if (montant == null) {
            return MONTANT_ABSENT;
        }
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRANCE);
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
        // DecimalFormat n'est pas thread-safe : une instance par appel.
        DecimalFormat formatter = new DecimalFormat("#,##0.00", symbols);
        formatter.setRoundingMode(RoundingMode.HALF_UP);
        return formatter.format(montant);
    }
}
