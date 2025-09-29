package staport.finance.gBanque.leasing.echeancesLeasing.dto;

import staport.finance.gBanque.leasing.echeancesLeasing.enums.StatutEcheanceLeasing;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * DTO de réponse pour l'entité EcheanceLeasing.
 * Inclut les montants TTC, HT et TVA pour chaque échéance.
 */
public class EcheanceLeasingResponseDTO {

    private Long id;
    private Long contratLeasingId; // ID du contrat parent pour référence
    private String numeroContrat;
    private Integer numeroEcheance;
    private OffsetDateTime dateEcheance;
    private OffsetDateTime dateOperation ;
    private OffsetDateTime dateValeur;
    private BigDecimal montantTTC;
    private BigDecimal montantHT;  // Nouveau champ
    private BigDecimal montantTVA; // Nouveau champ
    private StatutEcheanceLeasing statutPaiement;
    private OffsetDateTime dateCreation;
    private OffsetDateTime dateModification;
    private String libelle;
    private String observation;

    // --- Getters et Setters ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getContratLeasingId() {
        return contratLeasingId;
    }

    public void setContratLeasingId(Long contratLeasingId) {
        this.contratLeasingId = contratLeasingId;
    }

    public Integer getNumeroEcheance() {
        return numeroEcheance;
    }

    public void setNumeroEcheance(Integer numeroEcheance) {
        this.numeroEcheance = numeroEcheance;
    }

    public OffsetDateTime getDateEcheance() {
        return dateEcheance;
    }

    public void setDateEcheance(OffsetDateTime dateEcheance) {
        this.dateEcheance = dateEcheance;
    }

    public BigDecimal getMontantTTC() {
        return montantTTC;
    }

    public void setMontantTTC(BigDecimal montantTTC) {
        this.montantTTC = montantTTC;
    }

    public BigDecimal getMontantHT() {
        return montantHT;
    }

    public void setMontantHT(BigDecimal montantHT) {
        this.montantHT = montantHT;
    }

    public BigDecimal getMontantTVA() {
        return montantTVA;
    }

    public void setMontantTVA(BigDecimal montantTVA) {
        this.montantTVA = montantTVA;
    }

    public StatutEcheanceLeasing getStatutPaiement() {
        return statutPaiement;
    }

    public void setStatutPaiement(StatutEcheanceLeasing statutPaiement) {
        this.statutPaiement = statutPaiement;
    }

    public OffsetDateTime getDateOperation() {
        return dateOperation;
    }

    public void setDateOperation(OffsetDateTime dateOperation) {
        this.dateOperation = dateOperation;
    }

    public OffsetDateTime getDateValeur() {
        return dateValeur;
    }

    public void setDateValeur(OffsetDateTime dateValeur) {
        this.dateValeur = dateValeur;
    }

    public OffsetDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(OffsetDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public OffsetDateTime getDateModification() {
        return dateModification;
    }

    public void setDateModification(OffsetDateTime dateModification) {
        this.dateModification = dateModification;
    }

    public String getNumeroContrat() {
        return numeroContrat;
    }

    public void setNumeroContrat(String numeroContrat) {
        this.numeroContrat = numeroContrat;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }
}