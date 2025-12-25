package staport.finance.gBanque.Bank.enums;



public enum Devise {
    MAD("Dirham Marocain"),
    EUR("Euro"),
    USD("US Dollar");

    private final String description;

    Devise(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}