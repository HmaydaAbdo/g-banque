package staport.finance.gBanque.moyensDePaiements.miseADisposition.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.dtos.MiseADispositionResponseDto;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.dtos.MiseAdispositionRequestDto;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.entities.MiseADisposition;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;

/**
 * Mapper MapStruct du module Mise à Disposition.
 *
 * <p>{@code libelle}/{@code observation} sont ignorés par {@code toDto} : ce sont des champs
 * d'affichage recalculés à chaque lecture (jamais les valeurs persistées sur l'entité), donc une
 * règle métier de Service et non du Mapper — cf. architecture.md, même règle que
 * Cheque/LCN/Virement.
 *
 * <p>{@code observation} de l'entité est en revanche renseigné dès {@code toEntity} avec la même
 * convention (" MAD ...") que celle utilisée pour l'affichage, afin que le champ ne soit jamais
 * {@code null} lors d'une validation en lot (voir {@code BatchValidationService}).
 */
@Mapper(componentModel = "spring")
public interface MiseADispositionMapper {

    @Mapping(target = "banqueId", source = "banque.id")
    @Mapping(target = "banqueName", source = "banque.nom")
    @Mapping(target = "libelle", ignore = true)
    @Mapping(target = "observation", ignore = true)
    MiseADispositionResponseDto toDto(MiseADisposition miseADisposition);

    default MiseADisposition toEntity(MiseAdispositionRequestDto dto, Banque banque) {
        MiseADisposition miseADisposition = new MiseADisposition();
        miseADisposition.setDateEmission(dto.getDateEmission());
        miseADisposition.setBeneficiaire(dto.getBeneficiaire());
        miseADisposition.setMontant(dto.getMontant());
        miseADisposition.setBanque(banque);
        miseADisposition.setStatut(StatutMoyenPaiement.ENCOURS);
        miseADisposition.setType(dto.getType());
        miseADisposition.setEmetteur(dto.getEmetteur());
        miseADisposition.setObservation(" MAD " + miseADisposition.getNumero());
        miseADisposition.setLibelle(dto.getType() == staport.finance.gBanque.moyensDePaiements.virements.enums.Type.DECAISSEMENT
                ? "Bénéficiaire: " + (dto.getBeneficiaire() != null ? dto.getBeneficiaire() : "")
                : "Émetteur: " + (dto.getEmetteur() != null ? dto.getEmetteur() : ""));
        return miseADisposition;
    }
}
