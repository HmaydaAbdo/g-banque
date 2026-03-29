package staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NatureLigneFonctionnelleRequest(

        @NotBlank(message = "Le code de la nature de ligne fonctionnelle est obligatoire.")
        @Size(max = 20, message = "Le code ne doit pas dépasser 20 caractères.")
        String code,

        @NotBlank(message = "Le label de la nature de ligne fonctionnelle est obligatoire.")
        String label,

        @NotBlank(message = "La description de la nature de ligne fonctionnelle est obligatoire.")
        String description

) {}