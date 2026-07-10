package staport.finance.gBanque.decomptes.dto;

import staport.finance.gBanque.prefinancement.enums.Status;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DecompteResponseDto {

    private Long id;

    // Informations du marché
    private Long marcheId;
    private String marcheName;
    private String marcheNumero;

    // Banque du marché
    private Long marcheBanqueId;
    private String marcheBanqueName;

    // Banque d'encaissement (celle où le décompte sera encaissé)
    private Long banqueEncaissementId;
    private String banqueEncaissementName;

    private LocalDate dateEmission ;
    private BigDecimal montant;
    private String observation;
    private LocalDate dateOperation ;
    private LocalDate dateValeur ;
    private Status status;
    private String libelle;

    public DecompteResponseDto() {
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMarcheId() {
        return marcheId;
    }

    public void setMarcheId(Long marcheId) {
        this.marcheId = marcheId;
    }

    public String getMarcheName() {
        return marcheName;
    }

    public void setMarcheName(String marcheName) {
        this.marcheName = marcheName;
    }

    public String getMarcheNumero() {
        return marcheNumero;
    }

    public void setMarcheNumero(String marcheNumero) {
        this.marcheNumero = marcheNumero;
    }

    public Long getMarcheBanqueId() {
        return marcheBanqueId;
    }

    public void setMarcheBanqueId(Long marcheBanqueId) {
        this.marcheBanqueId = marcheBanqueId;
    }

    public String getMarcheBanqueName() {
        return marcheBanqueName;
    }

    public void setMarcheBanqueName(String marcheBanqueName) {
        this.marcheBanqueName = marcheBanqueName;
    }

    public Long getBanqueEncaissementId() {
        return banqueEncaissementId;
    }

    public void setBanqueEncaissementId(Long banqueEncaissementId) {
        this.banqueEncaissementId = banqueEncaissementId;
    }

    public String getBanqueEncaissementName() {
        return banqueEncaissementName;
    }

    public void setBanqueEncaissementName(String banqueEncaissementName) {
        this.banqueEncaissementName = banqueEncaissementName;
    }

    public LocalDate getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(LocalDate dateEmission) {
        this.dateEmission = dateEmission;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }

    public LocalDate getDateOperation() {
        return dateOperation;
    }

    public void setDateOperation(LocalDate dateOperation) {
        this.dateOperation = dateOperation;
    }

    public LocalDate getDateValeur() {
        return dateValeur;
    }

    public void setDateValeur(LocalDate dateValeur) {
        this.dateValeur = dateValeur;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }
}