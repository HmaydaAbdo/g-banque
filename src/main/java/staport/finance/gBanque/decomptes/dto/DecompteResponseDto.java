package staport.finance.gBanque.decomptes.dto;

import staport.finance.gBanque.prefinancement.enums.Status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class DecompteResponseDto {

    private Long id;

    // Informations du marché
    private Long marcheId;
    private String marcheName;
    private String marcheNumero;
    private boolean marcheNanti;  // ← AJOUTER pour savoir si le marché est nanti

    // Banque du marché (celle associée au nantissement)
    private Long marcheBanqueId;
    private String marcheBanqueName;

    // Banque d'encaissement (celle où le décompte sera encaissé)
    private Long banqueEncaissementId;
    private String banqueEncaissementName;

    private OffsetDateTime dateEmission;
    private BigDecimal montant;
    private String observation;
    private OffsetDateTime dateOperation;
    private OffsetDateTime dateValeur;
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

    public boolean isMarcheNanti() {
        return marcheNanti;
    }

    public void setMarcheNanti(boolean marcheNanti) {
        this.marcheNanti = marcheNanti;
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

    public OffsetDateTime getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(OffsetDateTime dateEmission) {
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