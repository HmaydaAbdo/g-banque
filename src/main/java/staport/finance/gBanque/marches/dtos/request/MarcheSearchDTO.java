package staport.finance.gBanque.marches.dtos.request;

import jakarta.validation.constraints.Size;

public record MarcheSearchDTO(

        @Size(max = 255, message = "Le nom doit avoir au maximum 255 caracteres")
        String nom,

        @Size(max = 50, message = "Le numero doit avoir au maximum 50 caracteres")
        String numero,

        @Size(max = 255, message = "Le maitre d'ouvrage doit avoir au maximum 255 caracteres")
        String maitreDouvrage,

        Long banqueId,

        /** null = tous statuts, true = actifs uniquement, false = archivés uniquement. */
        Boolean actif
) {
}
