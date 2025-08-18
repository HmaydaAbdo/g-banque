package staport.finance.gBanque.cmt.echeances.mappers;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.cmt.echeances.dtos.EcheanceCMTResponseDto;
import staport.finance.gBanque.cmt.echeances.entities.EcheanceCMT;
import staport.finance.gBanque.cmt.entities.CreditMoyenTerme;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Composant Spring pour le mappage entre l'entité EcheanceCMT et ses DTOs.
 */
@Component
public class EcheanceCMTMapper {

    /**
     * Convertit une entité EcheanceCMT en un DTO de réponse.
     *
     * @param entity L'entité EcheanceCMT à convertir.
     * @return Un DTO de réponse.
     */
    public EcheanceCMTResponseDto toResponseDto(EcheanceCMT entity) {
        if (entity == null) {
            return null;
        }

        EcheanceCMTResponseDto responseDto = new EcheanceCMTResponseDto();
        responseDto.setId(entity.getId());
        responseDto.setNumeroEcheance(entity.getNumeroEcheance());
        responseDto.setDateEcheance(entity.getDateEcheance());
        responseDto.setMontant(entity.getMontant());
        responseDto.setStatutPaiement(entity.getStatutPaiement());

        // *** CORRECTION : Ajout du mappage des dates de cycle de vie ***
        responseDto.setDateCreation(entity.getDateCreation());
        responseDto.setDateModification(entity.getDateModification());

        if (entity.getCreditMoyenTerme() != null) {
            responseDto.setCredit(toCreditDto(entity.getCreditMoyenTerme()));
        }

        return responseDto;
    }

    /**
     * Convertit une liste d'entités EcheanceCMT en une liste de DTOs de réponse.
     *
     * @param entities La liste d'entités à convertir.
     * @return La liste correspondante de DTOs.
     */
    public List<EcheanceCMTResponseDto> toResponseDtoList(List<EcheanceCMT> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Convertit une page d'entités EcheanceCMT en une page de DTOs.
     *
     * @param page La page d'entités.
     * @return Une page de DTOs.
     */
    public Page<EcheanceCMTResponseDto> toResponseDtoPage(Page<EcheanceCMT> page) {
        if (page == null) {
            return Page.empty();
        }
        return page.map(this::toResponseDto);
    }

    /**
     * Convertit une entité CreditMoyenTerme en son DTO imbriqué pour la réponse.
     *
     * @param credit L'entité CreditMoyenTerme à convertir.
     * @return Le DTO du crédit.
     */
    private EcheanceCMTResponseDto.CreditMoyenTermeDto toCreditDto(CreditMoyenTerme credit) {
        if (credit == null) {
            return null;
        }
        EcheanceCMTResponseDto.CreditMoyenTermeDto creditDto = new EcheanceCMTResponseDto.CreditMoyenTermeDto();
        creditDto.setId(credit.getId());
        creditDto.setDesignation(credit.getDesignation());
        return creditDto;
    }
}