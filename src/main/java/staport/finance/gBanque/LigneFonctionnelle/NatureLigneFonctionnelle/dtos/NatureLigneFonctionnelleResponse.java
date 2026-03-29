package staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.dtos;

import java.time.LocalDate;

public record NatureLigneFonctionnelleResponse(

        Long id,
        String code,
        String label,
        String description,
        LocalDate dateCreation,
        LocalDate dateModification

) {}