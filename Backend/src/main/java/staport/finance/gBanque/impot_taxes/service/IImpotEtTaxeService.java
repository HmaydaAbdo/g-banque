package staport.finance.gBanque.impot_taxes.service;

import org.springframework.data.domain.Pageable;
import staport.finance.gBanque.impot_taxes.dtos.request.EncoursImpotEtTaxeSearchDto;
import staport.finance.gBanque.impot_taxes.dtos.request.ImpotEtTaxeFusionRequest;
import staport.finance.gBanque.impot_taxes.dtos.request.ImpotEtTaxeRequestDto;
import staport.finance.gBanque.impot_taxes.dtos.request.ImpotEtTaxeSearchDto;
import staport.finance.gBanque.impot_taxes.dtos.response.EncoursImpotEtTaxeDto;
import staport.finance.gBanque.impot_taxes.dtos.response.ImpotEtTaxeResponseDto;
import staport.finance.gBanque.impot_taxes.enums.Motif;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Contrat de service pour le module Impôts et Taxes.
 *
 * <p>Le contrôleur, et tout autre module consommateur, injectent uniquement cette interface
 * (DIP). La classe concrète {@link ImpotEtTaxeServiceImpl} n'est jamais référencée depuis
 * l'extérieur du module — voir {@code architecture.md}, "Encapsulation des domaines".
 */
public interface IImpotEtTaxeService {

    ImpotEtTaxeResponseDto createImpotEtTaxe(ImpotEtTaxeRequestDto requestDto);

    ImpotEtTaxeResponseDto getById(Long id);

    EncoursImpotEtTaxeDto searchImpotEtTaxes(ImpotEtTaxeSearchDto searchDto, Pageable pageable);

    /**
     * Exporte en PDF l'intégralité des impôts/taxes correspondant aux critères fournis (mêmes
     * filtres que {@link #searchImpotEtTaxes}) — sans pagination : le fichier contient tous les
     * résultats filtrés, pas seulement la page actuellement affichée à l'écran. Réutilisé par les
     * écrans qui filtrent sur un {@code motif} donné (ex. TVA sur cautions).
     */
    byte[] exporterPdf(ImpotEtTaxeSearchDto searchDto);

    /**
     * Exporte en PDF l'intégralité de l'encours d'impôts/taxes d'une banque (mêmes filtres que
     * {@link #getOutstandingImpotEtTaxes}) — sans pagination : le fichier contient tous les
     * résultats filtrés, pas seulement la page actuellement affichée à l'écran.
     */
    byte[] exporterEncoursPdf(EncoursImpotEtTaxeSearchDto searchDto, Long banqueId);

    ImpotEtTaxeResponseDto updateImpotEtTaxe(Long id, ImpotEtTaxeRequestDto requestDto);

    void deleteImpotEtTaxe(Long id);

    ImpotEtTaxeResponseDto toggleStatus(Long id);

    ImpotEtTaxeResponseDto fusionner(ImpotEtTaxeFusionRequest request);

    EncoursImpotEtTaxeDto getOutstandingImpotEtTaxes(EncoursImpotEtTaxeSearchDto searchDto, Long banqueId, Pageable pageable);

    /**
     * Crée et persiste une TVA rattachée à une commission — façade pour les modules qui génèrent
     * une TVA sur une commission produite en dehors du module impôts/taxes (ex. commissions sur
     * cautions). Jamais via {@code ImpotEtTaxeRepository} directement (encapsulation des
     * domaines, architecture.md). Les associations sont résolues en interne à partir de leurs
     * identifiants — aucune entité d'un autre module ne transite par cette interface.
     *
     * @param cautionId optionnel — renseigné uniquement lorsque la TVA provient d'une commission
     *                  sur caution
     */
    ImpotEtTaxeResponseDto creerTvaSurCommission(Long banqueId, BigDecimal montant, Motif motif,
            LocalDate dateEmission, String libelle, String observation,
            Long commissionId, Long cautionId);

    /**
     * Supprime la TVA rattachée à une commission, si elle existe — utilisé lorsqu'une commission
     * de prorata est supprimée (ex. clôture d'une caution le même mois que sa prise).
     *
     * @return {@code true} si une TVA a été trouvée et supprimée, {@code false} sinon
     */
    boolean deleteByCommissionIdIfExists(Long commissionId);

    /**
     * Met à jour le montant de la TVA rattachée à une commission, si elle existe — utilisé
     * lorsque le montant d'une commission sur caution est modifié (le montant TVA suit, 10 %).
     * Ne fait rien si aucune TVA n'est rattachée à cette commission.
     */
    void updateMontantByCommissionId(Long commissionId, BigDecimal nouveauMontant);

    /**
     * Supprime en lot les TVA rattachées à une liste de commissions — utilisé lors d'une fusion
     * de commissions sur caution (les commissions sources sont supprimées, leurs TVA aussi).
     */
    void deleteByCommissionIdIn(List<Long> commissionIds);

    /**
     * Vérifie si un impôt/taxe portant ce libellé a déjà été généré pour cette banque dans la
     * fenêtre de dates donnée. Utilisé par {@code ChargesFixesScheduler} pour l'idempotence de
     * la génération mensuelle — remplace l'accès direct à {@code ImpotEtTaxeRepository} depuis un
     * module étranger (architecture.md, "Encapsulation des domaines").
     */
    boolean existsImpotTaxeGenere(Long banqueId, String libelle, LocalDate debut, LocalDate fin);
}
