package staport.finance.gBanque.leasing.contratLeasing.dto;

import java.time.LocalDate;

/**
 * DTO pour transférer les données d'un fichier PDF
 */
public class PdfFileDTO {

    private byte[] contenu;
    private String nomFichier;
    private Long tailleFichier;
    private LocalDate dateUpload ;

    // Constructeurs
    public PdfFileDTO() {}

    // Getters et Setters
    public byte[] getContenu() { return contenu; }
    public void setContenu(byte[] contenu) { this.contenu = contenu; }

    public String getNomFichier() { return nomFichier; }
    public void setNomFichier(String nomFichier) { this.nomFichier = nomFichier; }

    public Long getTailleFichier() { return tailleFichier; }
    public void setTailleFichier(Long tailleFichier) { this.tailleFichier = tailleFichier; }

    public LocalDate getDateUpload() { return dateUpload; }
    public void setDateUpload(LocalDate dateUpload) { this.dateUpload = dateUpload; }
}