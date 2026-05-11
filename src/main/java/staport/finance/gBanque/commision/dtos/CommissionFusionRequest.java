package staport.finance.gBanque.commision.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Requête de fusion de commissions sur cautions.
 *
 * <p>Règles métier validées côté service :
 * <ul>
 *   <li>Minimum 2 IDs</li>
 *   <li>Toutes les commissions doivent avoir {@code operation = CAUTION}</li>
 *   <li>Toutes doivent avoir {@code status = PREVU}</li>
 *   <li>Toutes doivent appartenir à la même banque</li>
 *   <li>Toutes doivent avoir la même {@code dateEmission}</li>
 * </ul>
 */
public class CommissionFusionRequest {

    @NotEmpty(message = "La liste des IDs est obligatoire")
    @Size(min = 2, message = "La fusion nécessite au moins 2 commissions")
    private List<Long> ids;

    @NotBlank(message = "Le libellé de la commission fusionnée est obligatoire")
    private String libelle;

    public CommissionFusionRequest() {}

    public List<Long> getIds() { return ids; }
    public void setIds(List<Long> ids) { this.ids = ids; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }
}
