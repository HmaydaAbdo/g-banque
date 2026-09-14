package staport.finance.gBanque.department.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de création/mise à jour d'un département.
 */
public record DepartmentRequest(

        @NotBlank(message = "Le nom du département est obligatoire.")
        @Size(min = 2, max = 100, message = "Le nom du département doit avoir entre 2 et 100 caractères.")
        String name,

        @Size(max = 255, message = "La description ne doit pas dépasser 255 caractères.")
        String description

) {}
