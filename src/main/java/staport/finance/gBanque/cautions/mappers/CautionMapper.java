package staport.finance.gBanque.cautions.mappers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.cautions.dtos.CautionCreateDto;
import staport.finance.gBanque.cautions.dtos.CautionResponseDto;
import staport.finance.gBanque.cautions.dtos.CautionUpdateDto;
import staport.finance.gBanque.cautions.entities.Caution;
import staport.finance.gBanque.marches.entities.Marche;

/**
 * Mapper manuel pour le module Caution.
 *
 * <p>Cohérent avec la convention du projet : {@code @Component} Spring, pas de MapStruct.
 * Toutes les associations sont mappées à partir des entités déjà chargées par l'EntityGraph
 * — aucun appel lazy supplémentaire ici.
 *
 * <p>Responsabilités :
 * <ul>
 *   <li>{@code toEntity}  — {@link CautionCreateDto} → {@link Caution} (sans les associations)</li>
 *   <li>{@code toDto}     — {@link Caution} → {@link CautionResponseDto}</li>
 *   <li>{@code applyUpdate} — applique les champs modifiables du DTO sur l'entité existante</li>
 * </ul>
 *
 * <p>Note : les associations ({@link Banque}, {@link NatureLigneFonctionnelle}, {@link Marche})
 * sont settées par le service après résolution des IDs — pas par le mapper.
 */
@Component
public class CautionMapper {

    private static final Logger logger = LoggerFactory.getLogger(CautionMapper.class);

    // ── Create DTO → Entity ───────────────────────────────────────────────────

    /**
     * Crée une nouvelle entité {@link Caution} à partir du DTO de création.
     * Les associations (banque, nature, marche) sont laissées null — le service
     * les résout et les injecte après appel de cette méthode.
     */
    public Caution toEntity(CautionCreateDto dto) {
        if (dto == null) {
            logger.warn("Tentative de conversion d'un CautionCreateDto null");
            return null;
        }

        Caution caution = new Caution();
        caution.setDateDePrise(dto.dateDePrise());
        caution.setDateDeMainLevee(dto.dateDeMainLevee());
        caution.setNumeroCaution(dto.numeroCaution());
        caution.setDesignation(dto.designation());
        caution.setOrganisme(dto.organisme());
        caution.setMontant(dto.montant());
        caution.setTaux(dto.taux());
        caution.setActif(true); // Toute nouvelle caution est active par défaut

        logger.debug("Entité Caution créée depuis DTO — numeroCaution: {}", dto.numeroCaution());
        return caution;
    }

    // ── Entity → Response DTO ─────────────────────────────────────────────────

    /**
     * Mappe une entité {@link Caution} vers le DTO de réponse.
     * Appelé uniquement après chargement complet des associations via EntityGraph.
     */
    public CautionResponseDto toDto(Caution caution) {
        if (caution == null) {
            logger.warn("Tentative de conversion d'une entité Caution null");
            return null;
        }

        Banque banque = caution.getBanque();
        NatureLigneFonctionnelle nature = caution.getNature();
        Marche marche = caution.getMarche(); // nullable

        return new CautionResponseDto(
            caution.getId(),

            // Banque — toujours présent (nullable = false sur l'entité)
            banque  != null ? banque.getId()  : null,
            banque  != null ? banque.getNom() : null,

            // Nature — toujours présent
            nature  != null ? nature.getId()    : null,
            nature  != null ? nature.getLabel() : null,
            nature  != null ? nature.getCode()  : null,

            // Marché — optionnel
            marche  != null ? marche.getId()     : null,
            marche  != null ? marche.getNumero() : null,
            marche  != null ? marche.getNom()    : null,

            // Données caution
            caution.getDateDePrise(),
            caution.getDateDeMainLevee(),
            caution.getNumeroCaution(),
            caution.getDesignation(),
            caution.getOrganisme(),
            caution.getMontant(),
            caution.getTaux(),
            caution.getActif(),

            // Audit
            caution.getCreatedAt(),
            caution.getUpdatedAt()
        );
    }

    // ── Apply Update ──────────────────────────────────────────────────────────

    /**
     * Applique tous les champs modifiables sur une entité existante.
     *
     * <p>Tous les champs sont modifiables : {@code numeroCaution}, {@code dateDePrise},
     * {@code designation}, {@code organisme}, {@code montant}, {@code taux}.
     *
     * <p>Les associations ({@code banque}, {@code nature}, {@code marche}) sont settées
     * directement par le service après résolution des IDs — pas par le mapper.
     */
    public void applyUpdate(CautionUpdateDto dto, Caution caution) {
        if (dto == null || caution == null) {
            logger.warn("applyUpdate appelé avec dto ou caution null — opération ignorée");
            return;
        }

        caution.setNumeroCaution(dto.numeroCaution());
        caution.setDateDePrise(dto.dateDePrise());
        caution.setDateDeMainLevee(dto.dateDeMainLevee());
        caution.setDesignation(dto.designation());
        caution.setOrganisme(dto.organisme());
        caution.setMontant(dto.montant());
        caution.setTaux(dto.taux());
        // banque, nature et marche sont settés par le service après résolution des IDs

        logger.debug("Mise à jour complète appliquée sur Caution id: {}", caution.getId());
    }
}
