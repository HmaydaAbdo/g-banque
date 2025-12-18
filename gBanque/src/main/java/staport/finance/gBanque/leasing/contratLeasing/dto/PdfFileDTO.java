package staport.finance.gBanque.leasing.contratLeasing.dto;

import java.time.OffsetDateTime;

/**
 * DTO pour transférer les données d'un fichier PDF
 */
public class PdfFileDTO {

    private byte[] contenu;
    private String nomFichier;
    private Long tailleFichier;
    private OffsetDateTime dateUpload;

    // Constructeurs
    public PdfFileDTO() {}

    // Getters et Setters
    public byte[] getContenu() { return contenu; }
    public void setContenu(byte[] contenu) { this.contenu = contenu; }

    public String getNomFichier() { return nomFichier; }
    public void setNomFichier(String nomFichier) { this.nomFichier = nomFichier; }

    public Long getTailleFichier() { return tailleFichier; }
    public void setTailleFichier(Long tailleFichier) { this.tailleFichier = tailleFichier; }

    public OffsetDateTime getDateUpload() { return dateUpload; }
    public void setDateUpload(OffsetDateTime dateUpload) { this.dateUpload = dateUpload; }
}