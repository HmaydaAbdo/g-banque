package staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.dtos;

import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.enums.PorteeNature;

public record NatureLigneFonctionnelleSearchCriteria(
        String code,
        String label,
        String description,
        PorteeNature portee
) {}