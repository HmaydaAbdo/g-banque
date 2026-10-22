package staport.finance.gBanque.commision.service;

import org.springframework.data.domain.Pageable;
import staport.finance.gBanque.commision.dtos.CommissionFusionRequest;
import staport.finance.gBanque.commision.dtos.CommissionFusionResponse;
import staport.finance.gBanque.commision.dtos.CommissionRequestDto;
import staport.finance.gBanque.commision.dtos.CommissionResponseDto;
import staport.finance.gBanque.commision.dtos.CommissionSearchDto;
import staport.finance.gBanque.commision.dtos.EncoursCommissionDto;
import staport.finance.gBanque.commision.enums.TypeCommission;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Interface publique du module commission.
 * Exposée aux modules tiers (ex : leasing, cautions) pour éviter toute dépendance directe
 * sur la classe concrète {@link CommissionService} ou sur {@code CommisionRepository}
 * (encapsulation des domaines, architecture.md). Couvre également l'intégralité du contrat
 * utilisé par {@code CommissionController} (mirroring {@code IImpotEtTaxeService}/
 * {@code ICautionService}) — le controller n'injecte plus la classe concrète.
 */
public interface ICommissionService {

    /**
     * Crée une nouvelle commission.
     *
     * @param requestDto Les données de la commission à créer
     * @return Le DTO de la commission créée
     */
    CommissionResponseDto createCommission(CommissionRequestDto requestDto);

    /** Récupère une commission par son ID. */
    CommissionResponseDto getById(Long id);

    /** Recherche paginée des commissions, avec le montant total agrégé correspondant aux critères. */
    EncoursCommissionDto searchCommissions(CommissionSearchDto searchDto, Pageable pageable);

    /** Met à jour une commission existante. */
    CommissionResponseDto updateCommission(Long id, CommissionRequestDto requestDto);

    /** Exporte en PDF l'intégralité des commissions correspondant aux critères fournis. */
    byte[] exporterPdf(CommissionSearchDto searchDto);

    /** Bascule le statut d'une commission entre PREVU et PAYE. */
    void toggleCommisionStatus(Long commissionId);

    /** Fusionne plusieurs commissions sur cautions en une seule (voir docs/modules/commission.md, §9). */
    CommissionFusionResponse fusionner(CommissionFusionRequest request);

    /**
     * Supprime toutes les commissions dont le libellé correspond au pattern
     * "Commission Prelevement Leasing contrat N°{numeroContrat}%".
     * Appelée lors de la suppression ou régénération des échéances d'un contrat de leasing.
     *
     * @param numeroContrat Le numéro du contrat de leasing
     */
    void deleteCommissionsByContratLeasing(String numeroContrat);

    /** Garde d'idempotence : une commission de cette opération existe déjà avec ce libellé à cette date d'émission. */
    boolean existsByOperationAndLibelleAndDateEmission(TransactionSource operation, String libelle, LocalDate dateEmission);

    // ── Façade dédiée aux commissions générées automatiquement sur caution ─────

    /**
     * Crée et persiste une commission rattachée à une caution (prorata d'entrée, mensuelle,
     * ou de clôture) — façade pour le module {@code cautions}, qui ne doit jamais injecter
     * {@code CommisionRepository} directement.
     *
     * @param type catégorie de la commission ({@link TypeCommission}) — permet de la retrouver
     *             ensuite de façon fiable (notamment {@code CLOTURE}, recalculée si la caution
     *             est corrigée après sa clôture), indépendamment du libellé ou de la date
     *             d'émission qui peuvent coïncider avec une autre commission du même mois.
     */
    CommissionResponseDto creerCommissionCaution(Long cautionId, Long banqueId, BigDecimal montant,
            LocalDate dateEmission, String libelle, String observation, TypeCommission type);

    /** Garde d'idempotence : une commission existe déjà pour cette caution à cette date exacte. */
    boolean existsByCautionAndDateEmission(Long cautionId, LocalDate dateEmission);

    /** Garde d'idempotence : une commission existe déjà pour cette caution dans cette période. */
    boolean existsByCautionAndDateEmissionBetween(Long cautionId, LocalDate debutPeriode, LocalDate finPeriode);

    /**
     * Récupère la commission {@code PREVU} d'une caution à une date d'émission exacte —
     * utilisé à la clôture pour retrouver le prorata d'entrée non encore validé.
     */
    Optional<CommissionResponseDto> findPrevuByCautionAndDateEmission(Long cautionId, LocalDate dateEmission);

    /**
     * Récupère la ou les commissions d'une caution d'une catégorie donnée — utilisé pour
     * retrouver la commission de clôture à recalculer lorsqu'une caution déjà clôturée est
     * corrigée ({@code CautionServiceImpl.modifier}). Normalement au plus un élément pour
     * {@link TypeCommission#CLOTURE} ; l'appelant doit signaler toute anomalie (0 ou plusieurs
     * résultats) plutôt que d'en choisir un arbitrairement.
     */
    List<CommissionResponseDto> findByCautionAndType(Long cautionId, TypeCommission type);

    /**
     * Supprime une commission par ID. Si la commission est liée à une caution
     * ({@code operation = CAUTION}), la TVA éventuellement rattachée est supprimée en cascade.
     */
    void deleteCommission(Long id);

    // ── Façade dédiée aux commissions générées automatiquement sur impôt/taxe ──

    /**
     * Crée et persiste une commission rattachée à un impôt/taxe (règle métier SAHAM BANK) —
     * façade pour le module {@code impot_taxes}, qui ne doit jamais injecter
     * {@code CommisionRepository} directement (encapsulation des domaines, architecture.md).
     * L'association est résolue en interne à partir de son identifiant — aucune entité d'un
     * autre domaine ne transite par cette interface.
     */
    CommissionResponseDto creerCommissionSurImpotEtTaxe(Long impotEtTaxeId, Long banqueId, BigDecimal montant,
            LocalDate dateEmission, String libelle, String observation);
}
