package staport.finance.gBanque.leasing.contratLeasing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
@ConfigurationProperties(prefix = "leasing.contract")
public class LeasingProperties {

    private BigDecimal valeurResiduelle;
    private BigDecimal tauxTva;

    public BigDecimal getValeurResiduelle() {
        return valeurResiduelle;
    }

    public void setValeurResiduelle(BigDecimal valeurResiduelle) {
        this.valeurResiduelle = valeurResiduelle;
    }

    public BigDecimal getTauxTva() {
        return tauxTva;
    }

    public void setTauxTva(BigDecimal tauxTva) {
        this.tauxTva = tauxTva;
    }
}
