package staport.finance.gBanque.impot_taxes.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Requête de fusion de TVA (impôts/taxes) sur cautions.
 *
 * <p>Règles métier validées côté service :
 * <ul>
 *   <li>Minimum 2 IDs</li>
 *   <li>Toutes les TVA doivent avoir {@code status = PREVU}</li>
 *   <li>Toutes doivent appartenir à la même banque</li>
 *   <li>Toutes doivent avoir la même {@code dateEmission}</li>
 *   <li>Toutes doivent avoir le même {@code motif}</li>
 * </ul>
 */
public class ImpotEtTaxeFusionRequest {

    @NotEmpty(message = "La liste des IDs est obligatoire")
    @Size(min = 2, message = "La fusion nécessite au moins 2 TVA")
    private List<Long> ids;

    @NotBlank(message = "Le libellé de la TVA fusionnée est obligatoire")
    private String libelle;

    public ImpotEtTaxeFusionRequest() {}

    public List<Long> getIds() { return ids; }
    public void setIds(List<Long> ids) { this.ids = ids; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }
}
