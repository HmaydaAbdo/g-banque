package staport.finance.gBanque.cmt.echeances.dtos;


import staport.finance.gBanque.cmt.echeances.enums.StatutEcheanceCMT;

/**
 * DTO utilisé pour la recherche et le filtrage des échéances de crédits à moyen terme.
 * Tous les champs sont optionnels pour permettre des requêtes flexibles.
 */
public class EcheanceCMTSearchDto {

    private Long creditMoyenTermeId;
    private Integer numeroEcheance;
    private Integer month;              // Nouveau champ : mois de l'échéance (1-12)
    private Integer year;               // Nouveau champ : année de l'échéance
    private StatutEcheanceCMT statutPaiement;

    public EcheanceCMTSearchDto() {}

    public EcheanceCMTSearchDto(Integer month, Integer year, StatutEcheanceCMT statutPaiement) {
        this.month = month;
        this.year = year;
        this.statutPaiement = statutPaiement;
    }

    // Getters et Setters pour tous les champs
    public Long getCreditMoyenTermeId() { return creditMoyenTermeId; }
    public void setCreditMoyenTermeId(Long creditMoyenTermeId) { this.creditMoyenTermeId = creditMoyenTermeId; }
    public Integer getNumeroEcheance() { return numeroEcheance; }
    public void setNumeroEcheance(Integer numeroEcheance) { this.numeroEcheance = numeroEcheance; }
    public Integer getMonth() { return month; }
    public void setMonth(Integer month) { this.month = month; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public StatutEcheanceCMT getStatutPaiement() { return statutPaiement; }
    public void setStatutPaiement(StatutEcheanceCMT statutPaiement) { this.statutPaiement = statutPaiement; }

    @Override
    public String toString() {
        return "EcheanceCMTSearchDto{" +
                "creditMoyenTermeId=" + creditMoyenTermeId +
                ", numeroEcheance=" + numeroEcheance +
                ", month=" + month +
                ", year=" + year +
                ", statutPaiement=" + statutPaiement +
                '}';
    }
}