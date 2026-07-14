package staport.finance.gBanque.fraisBancaires.service;

import staport.finance.gBanque.fraisBancaires.dtos.FraisBancaireRequestDto;
import staport.finance.gBanque.fraisBancaires.dtos.FraisBancaireResposneDto;

import java.time.LocalDate;

/**
 * Façade minimale du module Frais bancaires exposée aux modules étrangers.
 *
 * <p>Périmètre volontairement réduit à ce qu'exploite {@code ChargesFixesScheduler}
 * (architecture.md, "Encapsulation des domaines") — un sweep complet du module Frais bancaires
 * exposerait une interface plus large le cas échéant.
 */
public interface IFraisBancaireService {

    FraisBancaireResposneDto createFraisBancaire(FraisBancaireRequestDto requestDto);

    /**
     * Vérifie si un frais bancaire portant ce libellé a déjà été généré pour cette banque dans la
     * fenêtre de dates donnée. Utilisé par {@code ChargesFixesScheduler} pour l'idempotence de
     * la génération mensuelle.
     */
    boolean existsChargeFixeGeneree(Long banqueId, String libelle, LocalDate debut, LocalDate fin);
}
