package staport.finance.gBanque.leasing.lesOrganismsLeasing.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OrganismeLeasingResponseDTO(
        Long id,
        String nom,
        BigDecimal plafond,
        LocalDate dateDebut,
        LocalDate dateFin
) {}
