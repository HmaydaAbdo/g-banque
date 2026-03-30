package staport.finance.gBanque.cautions.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import staport.finance.gBanque.cautions.dtos.CautionCreateDto;
import staport.finance.gBanque.cautions.dtos.CautionResponseDto;
import staport.finance.gBanque.cautions.dtos.CautionSearchCriteria;
import staport.finance.gBanque.cautions.dtos.CautionUpdateDto;
import staport.finance.gBanque.cautions.entities.Caution;

/**
 * Contrat de service pour le module Caution.
 *
 * <p>Le contrôleur injecte uniquement cette interface (DIP — Dependency Inversion Principle).
 * La classe concrète {@link CautionServiceImpl} n'est jamais référencée depuis l'extérieur.
 */
public interface ICautionService {

    /**
     * Crée une nouvelle caution.
     * Vérifie l'unicité du {@code numeroCaution} avant persistance.
     *
     * @param dto données de création
     * @return DTO de réponse avec toutes les associations résolues
     */
    CautionResponseDto creer(CautionCreateDto dto);

    /**
     * Modifie une caution existante.
     * Tous les champs sont modifiables, y compris {@code numeroCaution},
     * {@code dateDePrise} et {@code banqueId}.
     *
     * <p>Règle métier : la modification est interdite lorsque la caution est inactive
     * ({@code actif = false}).
     *
     * @param id  identifiant de la caution
     * @param dto données de modification
     * @return DTO de réponse mis à jour
     */
    CautionResponseDto modifier(Long id, CautionUpdateDto dto);

    /**
     * Récupère une caution par son identifiant avec toutes ses associations.
     *
     * @param id identifiant de la caution
     * @return DTO de réponse complet
     */
    CautionResponseDto getById(Long id);

    /**
     * Recherche paginée avec filtres dynamiques.
     *
     * @param criteres critères de filtrage (tous optionnels)
     * @param pageable pagination et tri
     * @return page de DTOs correspondant aux critères
     */
    Page<CautionResponseDto> rechercher(CautionSearchCriteria criteres, Pageable pageable);

    /**
     * Bascule l'état {@code actif} d'une caution (activation / désactivation manuelle).
     *
     * <p>Règles métier appliquées :
     * <ul>
     *   <li>Une caution déjà inactive ne peut pas être désactivée à nouveau</li>
     *   <li>Une caution déjà active ne peut pas être activée à nouveau</li>
     * </ul>
     *
     * @param id identifiant de la caution
     * @return DTO de réponse avec le nouvel état
     */
    CautionResponseDto toggleActif(Long id);

    /**
     * Suppression physique (hard delete) de la caution.
     *
     * <p>Règle métier : la suppression est interdite lorsque la caution est inactive
     * ({@code actif = false}). Lève une
     * {@link staport.finance.gBanque.cautions.exceptions.CautionBusinessException} dans ce cas.
     *
     * @param id identifiant de la caution
     */
    void supprimer(Long id);

    /**
     * Méthode interne — récupère l'entité brute ou lève une exception.
     * Exposée dans l'interface pour permettre son usage inter-services si nécessaire.
     *
     * @param id identifiant de la caution
     * @return entité {@link Caution}
     */
    Caution trouverOuLeverException(Long id);
}
