package staport.finance.gBanque.moyensDePaiements.cheques.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.moyensDePaiements.cheques.dtos.ChequeRequestDto;
import staport.finance.gBanque.moyensDePaiements.cheques.dtos.ChequeResponseDto;
import staport.finance.gBanque.moyensDePaiements.cheques.entities.Cheque;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;

/**
 * Mapper MapStruct pour le module Chèque — conversion uniquement, aucune logique métier
 * (architecture.md, section Mapper).
 *
 * <p>{@code libelle}/{@code observation} du DTO sont des libellés d'affichage calculés
 * (différents des champs {@code libelle}/{@code observation} de l'entité, alimentés par la
 * validation/rapprochement) — cette règle d'affichage est appliquée par {@code ChequeService}
 * après le mapping, pas ici.
 *
 * <p>{@code observation} de l'entité est en revanche renseigné dès {@link #toEntity} avec la même
 * convention ("CHQ N°...") que celle utilisée pour l'affichage, afin que le champ ne soit jamais
 * {@code null} lors d'une validation en lot (la validation en lot ne fournit pas d'observation
 * unitaire et retombe sur celle de l'entité — voir {@code BatchValidationService}).
 */
@Mapper(componentModel = "spring")
public interface ChequeMapper {

    @Mapping(target = "nomBanque",   source = "banque.nom")
    @Mapping(target = "banqueId",    source = "banque.id")
    @Mapping(target = "libelle",     ignore = true)
    @Mapping(target = "observation", ignore = true)
    ChequeResponseDto toDto(Cheque cheque);

    /**
     * Crée une nouvelle entité chèque à partir du DTO de requête.
     * La banque est résolue et injectée par le service ; le statut initial est ENCOURS.
     */
    default Cheque toEntity(ChequeRequestDto dto, Banque banque) {
        if (dto == null || banque == null) {
            return null;
        }
        Cheque cheque = new Cheque();
        cheque.setNumero(dto.getNumero());
        cheque.setDateEmission(dto.getDateEmission());
        if (dto.getDateEcheance() != null) {
            cheque.setDateEcheance(dto.getDateEcheance());
        }
        cheque.setBeneficiaire(dto.getBeneficiaire());
        cheque.setMontant(dto.getMontant());
        cheque.setBanque(banque);
        cheque.setStatut(StatutMoyenPaiement.ENCOURS);
        cheque.setType(dto.getType());
        cheque.setEmetteur(dto.getEmetteur());
        cheque.setObservation("CHQ N°" + dto.getNumero());
        return cheque;
    }
}
