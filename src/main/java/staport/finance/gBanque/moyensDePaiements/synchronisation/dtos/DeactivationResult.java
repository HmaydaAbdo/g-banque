package staport.finance.gBanque.moyensDePaiements.synchronisation.dtos;

public class DeactivationResult {
    private final int chequesDeactivated;
    private final int virementsDeactivated;
    private final int miseADispositionsDeactivated;
    private final int lcnsDeactivated;

    public DeactivationResult(int chequesDeactivated, int virementsDeactivated,
                              int miseADispositionsDeactivated, int lcnsDeactivated) {
        this.chequesDeactivated = chequesDeactivated;
        this.virementsDeactivated = virementsDeactivated;
        this.miseADispositionsDeactivated = miseADispositionsDeactivated;
        this.lcnsDeactivated = lcnsDeactivated;
    }

    public int getChequesDeactivated() {
        return chequesDeactivated;
    }

    public int getVirementsDeactivated() {
        return virementsDeactivated;
    }

    public int getMiseADispositionsDeactivated() {
        return miseADispositionsDeactivated;
    }

    public int getLcnsDeactivated() {
        return lcnsDeactivated;
    }

    public int getTotalDeactivated() {
        return chequesDeactivated + virementsDeactivated + miseADispositionsDeactivated + lcnsDeactivated;
    }

    @Override
    public String toString() {
        return String.format("DeactivationResult{chèques=%d, virements=%d, mises à disposition=%d, LCN=%d, total=%d}",
                chequesDeactivated, virementsDeactivated, miseADispositionsDeactivated, lcnsDeactivated, getTotalDeactivated());
    }
}