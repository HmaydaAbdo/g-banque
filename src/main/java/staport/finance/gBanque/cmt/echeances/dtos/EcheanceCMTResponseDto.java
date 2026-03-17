package staport.finance.gBanque.cmt.echeances.dtos;

import staport.finance.gBanque.cmt.echeances.enums.StatutEcheanceCMT;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * DTO utilisé pour renvoyer les informations d'une échéance de crédit au client.
 */
public class EcheanceCMTResponseDto {

    private Long id;
    private Integer numeroEcheance;
    private LocalDate dateEcheance ;
    private LocalDate dateValuer ;
    private LocalDate dateOperation ;
    private BigDecimal montantTtc;

    private String observation;
    private String libelle;
    private StatutEcheanceCMT statutPaiement;
    private OffsetDateTime dateCreation;
    private OffsetDateTime dateModification;
    private CreditMoyenTermeDto credit;

    public EcheanceCMTResponseDto() {}

    // Getters et Setters pour tous les champs
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getNumeroEcheance() { return numeroEcheance; }
    public void setNumeroEcheance(Integer numeroEcheance) { this.numeroEcheance = numeroEcheance; }
    public LocalDate getDateEcheance() { return dateEcheance; }
    public void setDateEcheance(LocalDate dateEcheance) { this.dateEcheance = dateEcheance; }
    public StatutEcheanceCMT getStatutPaiement() { return statutPaiement; }
    public void setStatutPaiement(StatutEcheanceCMT statutPaiement) { this.statutPaiement = statutPaiement; }
    public OffsetDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(OffsetDateTime dateCreation) { this.dateCreation = dateCreation; }
    public OffsetDateTime getDateModification() { return dateModification; }
    public void setDateModification(OffsetDateTime dateModification) { this.dateModification = dateModification; }
    public CreditMoyenTermeDto getCredit() { return credit; }
    public void setCredit(CreditMoyenTermeDto credit) { this.credit = credit; }

    public LocalDate getDateValuer() {
        return dateValuer;
    }

    public void setDateValuer(LocalDate dateValuer) {
        this.dateValuer = dateValuer;
    }

    public LocalDate getDateOperation() {
        return dateOperation;
    }

    public void setDateOperation(LocalDate dateOperation) {
        this.dateOperation = dateOperation;
    }

    public BigDecimal getMontantTtc() {
        return montantTtc;
    }

    public void setMontantTtc(BigDecimal montantTtc) {
        this.montantTtc = montantTtc;
    }



    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    /**
     * DTO imbriqué pour exposer les informations minimales du crédit parent.
     */
    public static class CreditMoyenTermeDto {
        private Long id;
        private String designation;

        public CreditMoyenTermeDto() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getDesignation() { return designation; }
        public void setDesignation(String designation) { this.designation = designation; }


    }
}