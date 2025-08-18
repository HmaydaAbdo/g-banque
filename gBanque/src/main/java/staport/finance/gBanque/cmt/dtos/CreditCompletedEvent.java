package staport.finance.gBanque.cmt.dtos;

/**
 * Événement publié lorsqu'un crédit moyen terme est considéré comme entièrement payé.
 */
public class CreditCompletedEvent {

    private final Long creditId;

    public CreditCompletedEvent(Long creditId) {
        this.creditId = creditId;
    }

    public Long getCreditId() {
        return creditId;
    }
}
