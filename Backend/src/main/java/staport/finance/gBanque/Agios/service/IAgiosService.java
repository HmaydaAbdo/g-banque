package staport.finance.gBanque.Agios.service;

import staport.finance.gBanque.Agios.dtos.AgiosRequestDto;
import staport.finance.gBanque.Agios.dtos.AgiosResponseDto;

import java.time.LocalDate;

/**
 * Façade minimale du module Agios exposée aux modules étrangers.
 *
 * <p>Périmètre volontairement réduit à ce qu'exploite {@code ChargesFixesScheduler}
 * (architecture.md, "Encapsulation des domaines") — un sweep complet du module Agios exposerait
 * une interface plus large le cas échéant.
 */
public interface IAgiosService {

    AgiosResponseDto createAgios(AgiosRequestDto dto);

    /**
     * Vérifie si un agios portant ce libellé a déjà été généré pour cette banque dans la fenêtre
     * de dates donnée. Utilisé par {@code ChargesFixesScheduler} pour l'idempotence de la
     * génération mensuelle.
     */
    boolean existsChargeFixeGeneree(Long banqueId, String libelle, LocalDate debut, LocalDate fin);
}
