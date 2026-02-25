package staport.finance.gBanque.Agios.controller;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.Agios.dtos.*;
import staport.finance.gBanque.Agios.entities.AgiosJournalier;
import staport.finance.gBanque.Agios.entities.AgiosTrimestriel;
import staport.finance.gBanque.Agios.enums.TypeAgios;
import staport.finance.gBanque.Agios.mapper.AgiosCalculatorMapper;
import staport.finance.gBanque.Agios.service.AgiosOrchestrateurService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/agios/calcul")
public class AgiosCalculController {

    private static final Logger log = LoggerFactory.getLogger(AgiosCalculController.class);
    private final AgiosOrchestrateurService orchestrateur;
    private final AgiosCalculatorMapper agiosCalculatorMapper;

    public AgiosCalculController(AgiosOrchestrateurService orchestrateur, AgiosCalculatorMapper agiosCalculatorMapper) {
        this.orchestrateur = orchestrateur;
        this.agiosCalculatorMapper = agiosCalculatorMapper;
    }

    // ═══════════════════════════════════════════════════════════════
    //  RÉFÉRENTIEL
    // ═══════════════════════════════════════════════════════════════

    /**
     * Liste les types d'agios actuellement actifs.
     * GET /agios/calcul/types
     */
    @GetMapping("/types")
    public ResponseEntity<List<String>> getTypesActifs() {
        List<String> types = orchestrateur.getTypesActifs().stream()
                .map(t -> t.name() + " — " + t.getLibelle())
                .collect(Collectors.toList());
        return ResponseEntity.ok(types);
    }


        // ═══════════════════════════════════════════════════════════════
        //  CONSULTATION PAGINÉE
        // ═══════════════════════════════════════════════════════════════

    /**
     * Recherche paginée des trimestres avec filtres.
     * GET /agios/calcul/trimestres?banqueId=&type=&annee=&trimestre=&statut=&search=&page=0&size=10&sort=annee,desc
     */
    @GetMapping("/trimestres")
    public ResponseEntity<Page<AgiosTrimestrielResponseDto>> searchTrimestres(
            AgiosTrimestrielSearchDto searchDto, Pageable pageable) {

        Page<AgiosTrimestriel> page = orchestrateur.searchTrimestres(searchDto, pageable);
        return ResponseEntity.ok(page.map(agiosCalculatorMapper::toTrimDto));
    }

    /**
     * Détail journalier paginé d'un trimestre.
     * GET /agios/calcul/detail?banqueId=&type=&trimestre=&annee=&page=0&size=31&sort=dateValeur,asc
     */
    @GetMapping("/detail")
    public ResponseEntity<Page<AgiosJournalierResponseDto>> searchDetail(
            AgiosJournalierSearchDto searchDto, Pageable pageable) {

        Page<AgiosJournalier> page = orchestrateur.searchJournaliers(searchDto, pageable);
        return ResponseEntity.ok(page.map(agiosCalculatorMapper::toJourDto));
    }

    /**
     * Agrégats journaliers avec date limite optionnelle.
     * GET /agios/calcul/detail/aggregates?banqueId=&type=&trimestre=&annee=&dateLimite=2026-02-01
     */
    @GetMapping("/detail/aggregates")
    public ResponseEntity<AgiosDetailAggregateDto> getDetailAggregates(
            AgiosJournalierSearchDto searchDto) {
        return ResponseEntity.ok(orchestrateur.getAggregates(searchDto));
    }



    // ═══════════════════════════════════════════════════════════════
    //  ACTIONS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Déclenche manuellement le recalcul du trimestre courant pour toutes les banques.
     * POST /agios/calcul/recalculer
     */
    @PostMapping("/recalculer")
    public ResponseEntity<String> recalculerTout() {
        log.info("Recalcul manuel déclenché (trimestre courant, toutes banques)");
        orchestrateur.calculQuotidienAutomatique();
        return ResponseEntity.ok("Recalcul du trimestre courant terminé");
    }

    /**
     * Recalcul manuel d'un trimestre précis pour une banque donnée.
     * Utilisable pour les trimestres passés non encore clôturés.
     * POST /agios/calcul/banque/{banqueId}/recalculer/{trim}/{annee}
     */
    @PostMapping("/banque/{banqueId}/recalculer/{trim}/{annee}")
    public ResponseEntity<String> recalculerTrimestreManuel(
            @PathVariable Long banqueId,
            @PathVariable int trim,
            @PathVariable int annee) {
        log.info("Recalcul manuel T{}/{} banque {}", trim, annee, banqueId);
        orchestrateur.recalculerTrimestreManuel(banqueId, trim, annee);
        return ResponseEntity.ok(
                String.format("Recalcul T%d/%d terminé pour banque %d", trim, annee, banqueId));
    }

    /**
     * Clôture un trimestre pour un type et une banque.
     * POST /agios/calcul/banque/{banqueId}/type/{type}/cloturer/{trim}/{annee}
     */
    @PostMapping("/banque/{banqueId}/type/{type}/cloturer/{trim}/{annee}")
    public ResponseEntity<AgiosTrimestrielResponseDto> cloturer(
            @PathVariable Long banqueId, @PathVariable TypeAgios type,
            @PathVariable int trim, @PathVariable int annee) {
        AgiosTrimestriel result = orchestrateur.cloturerTrimestre(banqueId, type, trim, annee);
        return ResponseEntity.ok(agiosCalculatorMapper.toTrimDto(result));
    }



}