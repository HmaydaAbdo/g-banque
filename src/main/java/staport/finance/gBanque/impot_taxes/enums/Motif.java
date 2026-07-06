package staport.finance.gBanque.impot_taxes.enums;

public enum Motif {

    TVA("TVA"),
    IR("IR"),
    RETENUE_A_LA_SOURCE("Retenue à la source"),
    PENALITE_DE_RETARD("Pénalité de retard"),
    DGI("Direction Générale des Impôts"),
    ADII("Administration des Douanes et Impôts Indirects"),
    IS("Impôts sur les sociétés"),
    COMMUNAL("COMMUNAL"),
    TVA_COMMISSION_CAUTION("TVA sur commission sur caution"),
    TAXE_PROFESSIONNELLE("Taxe Professionnelle");


    private  final String motif;

    Motif(String motif) {
        this.motif = motif;
    }

    public String getMotif(){
        return this.motif;
    }
}
