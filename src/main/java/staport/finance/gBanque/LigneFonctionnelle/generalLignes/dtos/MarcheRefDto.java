package staport.finance.gBanque.LigneFonctionnelle.generalLignes.dtos;

public class MarcheRefDto {

    private Long id;
    private String numero;
    private String nom;

    public MarcheRefDto() {}

    public MarcheRefDto(Long id, String numero, String nom) {
        this.id = id;
        this.numero = numero;
        this.nom = nom;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
}
