package staport.finance.gBanque.fraisBancaires.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import staport.finance.gBanque.fraisBancaires.dtos.EncoursFraisBancaireDto;
import staport.finance.gBanque.fraisBancaires.dtos.EncoursFraisBancaireSearchDto;
import staport.finance.gBanque.fraisBancaires.dtos.FraisBancaireRequestDto;
import staport.finance.gBanque.fraisBancaires.dtos.FraisBancaireResponseDto;
import staport.finance.gBanque.fraisBancaires.dtos.FraisBancaireSearchDto;

import java.time.LocalDate;

/**
 * Contrat de service pour le module Frais bancaires.
 *
 * <p>Le contrôleur, et tout autre module consommateur ({@code ChargesFixesScheduler}), injectent
 * uniquement cette interface (DIP). La classe concrète {@link FraisBancaireServiceImpl} n'est
 * jamais référencée depuis l'extérieur du module — voir {@code architecture.md}, "Encapsulation
 * des domaines".
 *
 * <p>Interface élargie lors du sweep de conformité (couvrait auparavant seulement
 * {@code createFraisBancaire}/{@code existsChargeFixeGeneree}, le strict nécessaire pour
 * {@code ChargesFixesScheduler}) au contrat complet du contrôleur, mirroring
 * {@code IImpayeeService}/{@code IImpotEtTaxeService}.
 */
public interface IFraisBancaireService {

    FraisBancaireResponseDto createFraisBancaire(FraisBancaireRequestDto requestDto);

    FraisBancaireResponseDto getById(Long id);

    Page<FraisBancaireResponseDto> searchFraisBancaire(FraisBancaireSearchDto searchDto, Pageable pageable);

    /**
     * Exporte en PDF l'intégralité des frais bancaires correspondant aux critères fournis (mêmes
     * critères que {@link #searchFraisBancaire}, sans pagination).
     */
    byte[] exportFraisBancairePdf(FraisBancaireSearchDto searchDto);

    FraisBancaireResponseDto updateFraisBancaire(Long id, FraisBancaireRequestDto requestDto);

    void deleteFraisBancaire(Long id);

    EncoursFraisBancaireDto getOutstandingFraisBancaire(EncoursFraisBancaireSearchDto searchDto, Long banqueId, Pageable pageable);

    /**
     * Exporte en PDF l'encours de frais bancaires d'une banque (mêmes critères que
     * {@link #getOutstandingFraisBancaire}, sans pagination).
     */
    byte[] exportOutstandingFraisBancairePdf(EncoursFraisBancaireSearchDto searchDto, Long banqueId);

    FraisBancaireResponseDto toggleStatus(Long id);

    /**
     * Vérifie si un frais bancaire portant ce libellé a déjà été généré pour cette banque dans la
     * fenêtre de dates donnée. Utilisé par {@code ChargesFixesScheduler} pour l'idempotence de
     * la génération mensuelle.
     */
    boolean existsChargeFixeGeneree(Long banqueId, String libelle, LocalDate debut, LocalDate fin);
}
