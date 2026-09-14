package staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.dtos;

import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.enums.PorteeNature;
import java.time.LocalDate;

public record NatureLigneFonctionnelleResponse(

        Long id,
        String code,
        String label,
        String description,
        PorteeNature portee,
        LocalDate dateCreation,
        LocalDate dateModification

) {}