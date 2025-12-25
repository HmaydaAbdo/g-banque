package staport.finance.gBanque.cmt.echeances.mappers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.cmt.echeances.dtos.EcheanceCMTResponseDto;
import staport.finance.gBanque.cmt.echeances.entities.EcheanceCMT;
import staport.finance.gBanque.cmt.entities.CreditMoyenTerme;




/**
 * Composant Spring pour le mappage entre l'entité EcheanceCMT et ses DTOs.
 */
@Component
public class EcheanceCMTMapper {

    private static final Logger log = LoggerFactory.getLogger(EcheanceCMTMapper.class);

    /**
     * Convertit une entité EcheanceCMT en un DTO de réponse.
     *
     * @param entity L'entité EcheanceCMT à convertir.
     * @return Un DTO de réponse.
     */
    public EcheanceCMTResponseDto toResponseDto(EcheanceCMT entity) {
        log.debug("Début du mappage EcheanceCMT Entity vers ResponseDto");

        if (entity == null) {
            log.warn("Tentative de mappage avec une entité EcheanceCMT null");
            return null;
        }

        log.debug("Conversion de l'échéance CMT ID: {}, Numéro: {}", entity.getId(), entity.getNumeroEcheance());

        EcheanceCMTResponseDto responseDto = new EcheanceCMTResponseDto();
        responseDto.setId(entity.getId());
        responseDto.setNumeroEcheance(entity.getNumeroEcheance());
        responseDto.setDateEcheance(entity.getDateEcheance());
        responseDto.setMontantTtc(entity.getMontant());
        responseDto.setObservation("Prélèvement CMT");
        responseDto.setLibelle("échéance CMT");

        responseDto.setStatutPaiement(entity.getStatutPaiement());


        responseDto.setDateCreation(entity.getDateCreation());
        responseDto.setDateModification(entity.getDateModification());

        if (entity.getCreditMoyenTerme() != null) {
            log.debug("Mappage du crédit associé - ID: {}", entity.getCreditMoyenTerme().getId());
            responseDto.setCredit(toCreditDto(entity.getCreditMoyenTerme()));
        } else {
            log.debug("Aucun crédit associé à l'échéance ID: {}", entity.getId());
        }

        responseDto.setDateOperation(entity.getDateOperation()!=null ? entity.getDateOperation(): null);
        responseDto.setDateValuer(entity.getDateValeur()!=null ? entity.getDateValeur(): null);

        log.debug("ResponseDto créé avec succès pour l'échéance ID: {}, Statut: {}",
                entity.getId(), entity.getStatutPaiement());
        return responseDto;
    }


    /**
     * Convertit une page d'entités EcheanceCMT en une page de DTOs.
     *
     * @param page La page d'entités.
     * @return Une page de DTOs.
     */
    public Page<EcheanceCMTResponseDto> toResponseDtoPage(Page<EcheanceCMT> page) {
        log.debug("Début du mappage d'une page d'échéances CMT");

        if (page == null) {
            log.warn("Tentative de mappage avec une page null - retour d'une page vide");
            return Page.empty();
        }

        log.debug("Mappage de {} échéances CMT - Page: {}/{}",
                page.getNumberOfElements(), page.getNumber() + 1, page.getTotalPages());

        Page<EcheanceCMTResponseDto> resultPage = page.map(this::toResponseDto);

        log.debug("Page d'échéances CMT mappée avec succès - {} éléments convertis",
                resultPage.getNumberOfElements());

        return resultPage;
    }

    /**
     * Convertit une entité CreditMoyenTerme en son DTO imbriqué pour la réponse.
     *
     * @param credit L'entité CreditMoyenTerme à convertir.
     * @return Le DTO du crédit.
     */
    private EcheanceCMTResponseDto.CreditMoyenTermeDto toCreditDto(CreditMoyenTerme credit) {
        log.trace("Conversion de l'entité CreditMoyenTerme vers CreditMoyenTermeDto");

        if (credit == null) {
            log.warn("Tentative de conversion d'un CreditMoyenTerme null vers CreditMoyenTermeDto");
            return null;
        }

        log.trace("Création du CreditMoyenTermeDto pour le crédit ID: {}, Désignation: {}",
                credit.getId(), credit.getDesignation());

        EcheanceCMTResponseDto.CreditMoyenTermeDto creditDto = new EcheanceCMTResponseDto.CreditMoyenTermeDto();
        creditDto.setId(credit.getId());
        creditDto.setDesignation(credit.getDesignation());

        log.trace("CreditMoyenTermeDto créé avec succès");
        return creditDto;
    }
}