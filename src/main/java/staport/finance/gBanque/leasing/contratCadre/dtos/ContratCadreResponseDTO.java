package staport.finance.gBanque.leasing.contratCadre.dtos;

import staport.finance.gBanque.leasing.contratCadre.enums.StatutContratCadre;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ContratCadreResponseDTO(

        Long id,
        Long organismeId,
        String organismeNom,
        BigDecimal plafond,
        LocalDate dateDebut,
        LocalDate dateFin,
        StatutContratCadre statut,
        OffsetDateTime dateCreation,
        OffsetDateTime dateModification

) {}
