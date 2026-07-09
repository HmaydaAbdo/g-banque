package staport.finance.gBanque.cautions.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import staport.finance.gBanque.cautions.dtos.CautionCreateDto;
import staport.finance.gBanque.cautions.dtos.CautionResponseDto;
import staport.finance.gBanque.cautions.dtos.CautionSearchCriteria;
import staport.finance.gBanque.cautions.dtos.CautionUpdateDto;
import staport.finance.gBanque.cautions.dtos.CloturerCautionRequest;
import staport.finance.gBanque.cautions.dtos.CloturerCautionResponse;
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
     * Clôture une caution active : génère la commission de clôture selon la stratégie
     * de la banque (avec prorata temporis pour AWB, mois complet pour les autres),
     * génère la TVA si la stratégie l'exige, puis marque la caution inactive avec
     * sa date de main levée.
     *
     * <p>Contraintes :
     * <ul>
     *   <li>La caution doit être active ({@code actif = true})</li>
     *   <li>{@code dateDeMainLevee} ≥ {@code dateDePrise}</li>
     *   <li>Une stratégie doit exister pour la banque de la caution</li>
     * </ul>
     *
     * @param id      identifiant de la caution
     * @param request contient la date de main levée obligatoire
     * @return caution clôturée + commission générée
     */
    CloturerCautionResponse cloturer(Long id, CloturerCautionRequest request);

    /**
     * Méthode interne — récupère l'entité brute ou lève une exception.
     * Exposée dans l'interface pour permettre son usage inter-services si nécessaire.
     *
     * @param id identifiant de la caution
     * @return entité {@link Caution}
     */
    Caution trouverOuLeverException(Long id);

    /**
     * Génère un export PDF de l'intégralité des cautions correspondant aux critères fournis.
     *
     * <p>Contrairement à {@link #rechercher}, cette méthode ignore toute pagination — elle
     * imprime la totalité des résultats filtrés, pas seulement la page actuellement affichée
     * à l'écran.
     *
     * @param criteres critères de filtrage (tous optionnels — mêmes filtres que la recherche paginée)
     * @return contenu binaire du PDF généré
     */
    byte[] exporterPdf(CautionSearchCriteria criteres);
}
