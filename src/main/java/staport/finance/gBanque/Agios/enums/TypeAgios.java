package staport.finance.gBanque.Agios.enums;


public enum TypeAgios {

    FACILITE_CAISSE("Facilité de Caisse"),
    PREFINANCEMENT("Préfinancement"),
    DROIT_CONSTATE("Droits Constatés ");

    private final String libelle;

    TypeAgios(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() { return libelle; }
}