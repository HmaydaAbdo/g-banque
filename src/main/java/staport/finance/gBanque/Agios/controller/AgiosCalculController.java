package staport.finance.gBanque.Agios.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.Agios.dtos.*;
import staport.finance.gBanque.Agios.enums.TypeAgios;
import staport.finance.gBanque.Agios.service.AgiosOrchestrateurService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/agios/calcul")
@Tag(name = "Agios — Calcul FC", description = "Calcul automatique des agios de facilité de caisse")
@SecurityRequirement(name = "bearerAuth")
public class AgiosCalculController {

    private static final String FINANCE_OR_ADMIN =
            "hasAnyAuthority('DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR', 'SYSTEM_ADMIN')";
    private static final String SYSTEM_ADMIN = "hasAuthority('SYSTEM_ADMIN')";

    private final AgiosOrchestrateurService orchestrateur;

    public AgiosCalculController(AgiosOrchestrateurService orchestrateur) {
        this.orchestrateur = orchestrateur;
    }

    // ═══════════════════════════════════════════════════════════════
    //  RÉFÉRENTIEL
    // ═══════════════════════════════════════════════════════════════

    @GetMapping("/types")
    @PreAuthorize(FINANCE_OR_ADMIN)
    @Operation(summary = "Liste les types d'agios actifs")
    public ResponseEntity<List<String>> getTypesActifs() {
        List<String> types = orchestrateur.getTypesActifs().stream()
                .map(t -> t.name() + " — " + t.getLibelle())
                .collect(Collectors.toList());
        return ResponseEntity.ok(types);
    }

    // ═══════════════════════════════════════════════════════════════
    //  CONSULTATION PAGINÉE
    // ═══════════════════════════════════════════════════════════════

    @GetMapping("/trimestres")
    @PreAuthorize(FINANCE_OR_ADMIN)
    @Operation(summary = "Recherche paginée des trimestres d'agios FC")
    public ResponseEntity<Page<AgiosTrimestrielResponseDto>> searchTrimestres(
            AgiosTrimestrielSearchDto searchDto, Pageable pageable) {

        return ResponseEntity.ok(orchestrateur.searchTrimestres(searchDto, pageable));
    }

    @GetMapping("/detail")
    @PreAuthorize(FINANCE_OR_ADMIN)
    @Operation(summary = "Détail journalier paginé d'un trimestre")
    public ResponseEntity<Page<AgiosJournalierResponseDto>> searchDetail(
            AgiosJournalierSearchDto searchDto, Pageable pageable) {

        return ResponseEntity.ok(orchestrateur.searchJournaliers(searchDto, pageable));
    }

    @GetMapping("/detail/aggregates")
    @PreAuthorize(FINANCE_OR_ADMIN)
    @Operation(summary = "Agrégats journaliers d'un trimestre")
    public ResponseEntity<AgiosDetailAggregateDto> getDetailAggregates(
            AgiosJournalierSearchDto searchDto) {
        return ResponseEntity.ok(orchestrateur.getAggregates(searchDto));
    }

    // ═══════════════════════════════════════════════════════════════
    //  ACTIONS
    // ═══════════════════════════════════════════════════════════════

    @PostMapping("/recalculer")
    @PreAuthorize(FINANCE_OR_ADMIN)
    @Operation(summary = "Recalcul manuel du trimestre courant")
    public ResponseEntity<String> recalculerTout() {
        orchestrateur.calculQuotidienAutomatique();
        return ResponseEntity.ok("Recalcul du trimestre courant terminé");
    }

    @PostMapping("/banque/{banqueId}/recalculer/{trim}/{annee}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    @Operation(summary = "Recalcul manuel d'un trimestre précis")
    public ResponseEntity<String> recalculerTrimestreManuel(
            @PathVariable Long banqueId,
            @PathVariable int trim,
            @PathVariable int annee) {
        orchestrateur.recalculerTrimestreManuel(banqueId, trim, annee);
        return ResponseEntity.ok(
                String.format("Recalcul T%d/%d terminé pour banque %d", trim, annee, banqueId));
    }

    @PostMapping("/banque/{banqueId}/type/{type}/cloturer/{trim}/{annee}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    @Operation(summary = "Clôture définitive d'un trimestre")
    public ResponseEntity<AgiosTrimestrielResponseDto> cloturer(
            @PathVariable Long banqueId, @PathVariable TypeAgios type,
            @PathVariable int trim, @PathVariable int annee) {
        return ResponseEntity.ok(orchestrateur.cloturerTrimestre(banqueId, type, trim, annee));
    }
}
