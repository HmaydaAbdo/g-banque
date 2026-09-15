package staport.finance.gBanque.cmt.echeances.services.interfaces;

import org.springframework.data.domain.Pageable;
import staport.finance.gBanque.cmt.echeances.dtos.EcheanceCMTResponseDto;
import staport.finance.gBanque.cmt.echeances.dtos.EcheanceCMTSearchDto;
import staport.finance.gBanque.cmt.echeances.dtos.EcheanceCMTUpdateRequestDto;
import staport.finance.gBanque.cmt.echeances.dtos.TableauAmortissementCMTResponseDto;
import staport.finance.gBanque.cmt.entities.CreditMoyenTerme;

/**
 * Contrat du service de gestion des échéances de crédit à moyen terme (CMT).
 * <p>
 * Centralise la génération, la consultation et la mise à jour des échéances CMT.
 */
public interface IEcheanceCMTService {

    void generateEcheances(CreditMoyenTerme credit);

    /**
     * Régénère les échéances d'un crédit à moyen terme existant.
     * Prend l'entité directement (et non un simple ID) pour éviter que ce service
     * n'ait besoin d'injecter le Repository du module {@code cmt} parent — l'appelant
     * ({@code CreditMoyenTermeService}) possède déjà l'entité au moment de l'appel.
     */
    void regenerateEcheances(CreditMoyenTerme credit);

    EcheanceCMTResponseDto toggleStatus(Long echeanceId);

    /**
     * Met à jour les champs d'une échéance dont le statut est {@code PREVU}.
     * Toute tentative de modification d'une échéance {@code PAYE} lève une {@code ValidationException}.
     *
     * @param id  L'identifiant de l'échéance à modifier
     * @param dto Les nouvelles valeurs des champs
     * @return Le DTO de l'échéance mise à jour
     */
    EcheanceCMTResponseDto updateEcheance(Long id, EcheanceCMTUpdateRequestDto dto);

    /**
     * @param credit    crédit déjà résolu par l'appelant (voir {@code CreditMoyenTermeService.findOrThrow})
     * @param searchDto critères de recherche optionnels
     * @param pageable  pagination
     */
    TableauAmortissementCMTResponseDto getTableauAmortissementForCMT(CreditMoyenTerme credit,
                                                                       EcheanceCMTSearchDto searchDto,
                                                                       Pageable pageable);

    /**
     * Exporte en PDF le tableau d'amortissement complet d'un crédit (mêmes critères que
     * {@link #getTableauAmortissementForCMT}, sans pagination), avec les totaux
     * Endettement / Payé / Restant.
     */
    byte[] exportTableauAmortissementPdf(CreditMoyenTerme credit, EcheanceCMTSearchDto searchDto);

    boolean hasPaidEcheances(Long creditId);

    /**
     * Supprime toutes les échéances d'un crédit à moyen terme.
     * Utilisé lors de la suppression complète d'un crédit CMT.
     *
     * @param credit Le crédit dont les échéances doivent être supprimées
     * @return Le nombre d'échéances supprimées
     */
    int deleteEcheancesByCreditMoyenTerme(CreditMoyenTerme credit);

    /**
     * Récupère une échéance de crédit à moyen terme par son identifiant.
     * Utilisé par l'écran de récapitulatif unifié (validation) pour résoudre le crédit CMT
     * (creditMoyenTermeId, désignation, montant TTC) avant l'ouverture du formulaire de
     * validation, sans dupliquer cette logique côté frontend.
     *
     * @param id L'identifiant de l'échéance CMT
     * @return Le DTO de l'échéance correspondante
     */
    EcheanceCMTResponseDto getEcheanceCMTById(Long id);
}
