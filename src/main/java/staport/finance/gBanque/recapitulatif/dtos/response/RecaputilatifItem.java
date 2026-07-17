package staport.finance.gBanque.recapitulatif.dtos.response;

import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Ligne unifiée du récapitulatif (décaissement ou encaissement), construite en agrégeant
 * ~17 entités source différentes.
 *
 * <p><b>Transitoire :</b> ce DTO reste une classe mutable (pas un record) et continue d'être
 * peuplé via {@code RecapitulatifDecaissementMapper}/{@code RecapitulatifEncaissementMapper}
 * (mappers manuels, pas MapStruct). Il est destiné à être remplacé par l'entité JPA en lecture
 * seule mappée sur la vue SQL de récapitulatif (voir Phase 2 du sweep États des encours,
 * TODO.md) — convertir ce DTO en record maintenant serait un travail jetable.</p>
 */
public class RecaputilatifItem {
    private Long id;
    private LocalDate dateEmission;
    private LocalDate dateEcheance;
    private String libelle;
    private BigDecimal debit;
    private BigDecimal credit;
    private String observations;
    private TransactionSource source;

    public RecaputilatifItem() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(LocalDate dateEmission) {
        this.dateEmission = dateEmission;
    }

    public LocalDate getDateEcheance() {
        return dateEcheance;
    }

    public void setDateEcheance(LocalDate dateEcheance) {
        this.dateEcheance = dateEcheance;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public BigDecimal getDebit() {
        return debit;
    }

    public void setDebit(BigDecimal debit) {
        this.debit = debit;
    }

    public BigDecimal getCredit() {
        return credit;
    }

    public void setCredit(BigDecimal credit) {
        this.credit = credit;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }

    public TransactionSource getSource() {
        return source;
    }

    public void setSource(TransactionSource source) {
        this.source = source;
    }
}
