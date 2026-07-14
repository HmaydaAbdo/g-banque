package staport.finance.gBanque.prelevements.service;

import staport.finance.gBanque.prelevements.dtos.PrelevementRequestDto;
import staport.finance.gBanque.prelevements.dtos.PrelevementResponseDto;

import java.time.LocalDate;

/**
 * Façade minimale du module Prélèvements exposée aux modules étrangers.
 *
 * <p>Périmètre volontairement réduit à ce qu'exploite {@code ChargesFixesScheduler}
 * (architecture.md, "Encapsulation des domaines") — un sweep complet du module Prélèvements
 * exposerait une interface plus large le cas échéant.
 */
public interface IPrelevementService {

    PrelevementResponseDto createPrelevement(PrelevementRequestDto requestDto);

    /**
     * Vérifie si un prélèvement portant ce libellé a déjà été généré pour cette banque dans la
     * fenêtre de dates donnée. Utilisé par {@code ChargesFixesScheduler} pour l'idempotence de
     * la génération mensuelle.
     */
    boolean existsChargeFixeGeneree(Long banqueId, String libelle, LocalDate debut, LocalDate fin);
}
