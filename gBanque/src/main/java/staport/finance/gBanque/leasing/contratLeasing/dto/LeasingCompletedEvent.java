package staport.finance.gBanque.leasing.contratLeasing.dto;

/**
 * Événement publié lorsqu'un contrat de leasing est considéré comme entièrement payé.
 */
public class LeasingCompletedEvent {

    private final Long contratId;

    public LeasingCompletedEvent(Long contratId) {
        this.contratId = contratId;
    }

    public Long getContratId() {
        return contratId;
    }
}