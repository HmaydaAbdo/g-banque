package staport.finance.gBanque.chargesFixes;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.impot_taxes.enums.Motif;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Registre des charges fixes par banque (identifiée par son nom).
 *
 * Convention jourEmission :
 *   n > 0  → jour n du mois courant
 *  -1     → dernier jour du mois courant
 *  -2     → dernier jour du trimestre (mars, juin, sept, déc)
 *
 * Convention appendPeriode :
 *   true  → le scheduler ajoute automatiquement la période au libellé
 *            mensuel  → "LIBELLE 06/2026"
 *            trimestriel → "LIBELLE T2/2026"
 */
@Component
public class ChargesFixesRegistry {

    public Map<String, List<ChargeFixeDefinition>> getChargesParBanque() {
        return Map.of(
                "BMCE", chargesBmce(),
                "AWB",  chargesAwb(),
                "BMCI", chargesBmci(),
                "CDM",  chargesCdm(),
                "BP",   chargesBp(),
                "SGMB", chargesSgmb()
        );
    }

    // ------------------------------------------------------------------ BMCE
    private List<ChargeFixeDefinition> chargesBmce() {
        return List.of(
                new ChargeFixeDefinition("FRAIS BAD E-BANK",
                        new BigDecimal("100.00"), 1),

                new ChargeFixeDefinition("TVA BAD E-BANK",
                        new BigDecimal("10.00"), Motif.TVA, 1)
        );
    }

    // ------------------------------------------------------------------- AWB
    private List<ChargeFixeDefinition> chargesAwb() {
        return List.of(
                new ChargeFixeDefinition("PRLVT WANA B2C INWI",
                        new BigDecimal("32000.00"), 10),

                // appendPeriode=true → libellé final : "IAM 06/2026"
                new ChargeFixeDefinition("IAM",
                        new BigDecimal("6000.00"), 25, true),

                new ChargeFixeDefinition("FRAIS RELEVE CAUTION",
                        new BigDecimal("22.00"), -1),

                new ChargeFixeDefinition("FRAIS MODULE DOCNET 5ANS",
                        new BigDecimal("107.25"), -1),

                new ChargeFixeDefinition("FRAIS ATTIJARI NET",
                        new BigDecimal("71.50"), -1),

                new ChargeFixeDefinition("FRAIS PME++ EXONERATION ILLIMITEE DES OPERATIONS BANCAIRES",
                        new BigDecimal("327.80"), -1),

                new ChargeFixeDefinition("FRAIS SMS AVIZO",
                        new BigDecimal("49.50"), -1),

                new ChargeFixeDefinition("OPERATION SECCART",
                        new BigDecimal("36.36"), -1),

                new ChargeFixeDefinition("FRAIS TIMBRE FISCAUX",
                        new BigDecimal("5.00"), -1)
        );
    }

    // ------------------------------------------------------------------ BMCI
    private List<ChargeFixeDefinition> chargesBmci() {
        return List.of(
                new ChargeFixeDefinition("COMMISSION BMCI E-DOC",
                        new BigDecimal("242.00"), 1),

                new ChargeFixeDefinition("COMMISSION BMCI NET",
                        new BigDecimal("110.00"), 1),

                new ChargeFixeDefinition("ARVAL MAROC / LOCATION 14 VEHICULES",
                        new BigDecimal("60708.00"), 15),

                // AGIOS — appendPeriode=true → "AGIOS UTILISATION LIGNE ASMN FACTORING 06/2026"
                // Montant estimatif en attendant le calcul automatique des agios factoring
                new ChargeFixeDefinition("AGIOS UTILISATION LIGNE ASMN FACTORING",
                        new BigDecimal("300000.00"), TypeChargeFixe.AGIOS, 1, true)
        );
    }

    // ------------------------------------------------------------------- CDM
    private List<ChargeFixeDefinition> chargesCdm() {
        return List.of(
                new ChargeFixeDefinition("FRAIS PACK CDM ENTREPRISE",
                        new BigDecimal("440.00"), 1)
        );
    }

    // -------------------------------------------------------------------- BP
    private List<ChargeFixeDefinition> chargesBp() {
        return List.of(
                new ChargeFixeDefinition("COMMISSION CHAABI NET",
                        new BigDecimal("118.80"), 5)
        );
    }

    // ------------------------------------------------------------------ SGMB
    private List<ChargeFixeDefinition> chargesSgmb() {
        return List.of(

                // ---- Trimestriels ----
                new ChargeFixeDefinition("FRAIS TENUE DE COMPTE",
                        new BigDecimal("82.50"), -2),

                // appendPeriode=true → "IS ACOMPTE T2/2026"
                new ChargeFixeDefinition("IS ACOMPTE",
                        new BigDecimal("436730.00"), Motif.IS, -2, true),

                new ChargeFixeDefinition("COMM/PRLVT DGI TRIMESTRIEL",
                        new BigDecimal("5.50"), -2),

                // ---- Mensuels au 10 ----
                // PRELEVEMENT — appendPeriode=true → "CNSS 06/2026"
                new ChargeFixeDefinition("CNSS",
                        new BigDecimal("900000.00"), TypeChargeFixe.PRELEVEMENT, 10, true),

                new ChargeFixeDefinition("FRAIS PRLVT CNSS",
                        new BigDecimal("5.50"), 10),

                // ---- Mensuels fin de mois ----
                new ChargeFixeDefinition("FRAIS ABONNEMENT SOGECASH NET",
                        new BigDecimal("330.00"), -1),

                // 3 commissions DGI distinctes : une par prélèvement fiscal (TVA, IR, Retenue)
                new ChargeFixeDefinition("COMM/PRLVT DGI TVA",
                        new BigDecimal("5.50"), -1),

                new ChargeFixeDefinition("COMM/PRLVT DGI IR",
                        new BigDecimal("5.50"), -1),

                new ChargeFixeDefinition("COMM/PRLVT DGI RETENUE A LA SOURCE",
                        new BigDecimal("5.50"), -1),

                // appendPeriode=true, moisPrecedent=true → "TVA 05/2026" si tourne en juin
                new ChargeFixeDefinition("TVA",
                        new BigDecimal("1000000.00"), Motif.TVA, -1, true, true),

                // appendPeriode=true, moisPrecedent=true → "IR 05/2026" si tourne en juin
                new ChargeFixeDefinition("IR",
                        new BigDecimal("69399.00"), Motif.IR, -1, true, true),

                new ChargeFixeDefinition("RETENUE A LA SOURCE SUR LES LOYERS",
                        new BigDecimal("1100.00"), Motif.RETENUE_A_LA_SOURCE, -1)
        );
    }
}
