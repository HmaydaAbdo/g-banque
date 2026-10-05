package staport.finance.gBanque.droitConstate.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.services.IBanqueService;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.services.INatureLigneFonctionnelleService;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.entities.LigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.services.LigneFonctionnelleReader;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities.LigneFonctionnelleSpecifique;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.services.LigneFonctionnelleSpecifiqueReader;
import staport.finance.gBanque.audit.AuditOperation;
import staport.finance.gBanque.audit.AuditableAction;
import staport.finance.gBanque.droitConstate.dtos.request.DroitConstateRequestDto;
import staport.finance.gBanque.droitConstate.dtos.request.DroitConstateSearchDto;
import staport.finance.gBanque.droitConstate.dtos.response.DroitConstateResponseDto;
import staport.finance.gBanque.droitConstate.entities.DroitConstate;
import staport.finance.gBanque.droitConstate.mapper.DroitConstateMapper;
import staport.finance.gBanque.droitConstate.repositories.DroitConstateRepository;
import staport.finance.gBanque.droitConstate.spec.DroitConstateSpec;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.marches.service.IMarcheService;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prefinancement.enums.Type;
import staport.finance.gBanque.reports.service.StaportPdfReportService;
import staport.finance.gBanque.reports.utils.MontantFormatter;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import java.awt.Color;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implémentation du service Droit Constaté.
 *
 * <p>Encapsulation des domaines (architecture.md) : la résolution du {@link Marche} passe par
 * {@link IMarcheService}, celle de la {@link Banque} (champ direct, indépendant de
 * {@code marche.banque}) par {@link IBanqueService}, et la résolution de la nature de ligne
 * fonctionnelle "DROIT_CONSTATE" par {@link INatureLigneFonctionnelleService} — jamais par
 * injection directe d'un {@code Repository} étranger. {@code LigneFonctionnelleReader}/
 * {@code LigneFonctionnelleSpecifiqueReader} restent inchangés (déjà des façades de lecture
 * cross-domaine conformes).
 *
 * <p>Anti-N+1 : {@code DroitConstateRepository.findAll} porte l'{@code @EntityGraph}
 * {@code "DroitConstate.avecAssociations"} (marché + banque directe).
 *
 * <p>19/09/2026 : les encours de remboursement/déblocage par banque (écrans dédiés
 * {@code droit-constate-remboursements}/{@code droit-constate-deblocage}) ont été retirés — le
 * récapitulatif unifié couvre désormais la validation de cette source. {@code getRemboursementEncoursForBanque}/
 * {@code getDeblocageEncoursForBanque}/leurs exports PDF et le calcul de somme par
 * {@code EntityManager} qu'ils utilisaient seuls ont été retirés avec eux.
 */
@Service
public class DroitConstateServiceImpl implements IDroitConstateService {

    private static final Logger log = LoggerFactory.getLogger(DroitConstateServiceImpl.class);
    private static final String DROIT_CONSTATE_NATURE_CODE = "DROIT_CONSTATE";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final DroitConstateRepository droitConstateRepository;
    private final DroitConstateMapper droitConstateMapper;
    private final IMarcheService marcheService;
    private final IBanqueService banqueService;
    private final INatureLigneFonctionnelleService natureLigneFonctionnelleService;
    private final LigneFonctionnelleSpecifiqueReader ligneFonctionnelleSpecifiqueReader;
    private final LigneFonctionnelleReader ligneFonctionnelleReader;
    private final StaportPdfReportService staportPdfReportService;

    public DroitConstateServiceImpl(DroitConstateRepository droitConstateRepository,
                                     DroitConstateMapper droitConstateMapper,
                                     IMarcheService marcheService,
                                     IBanqueService banqueService,
                                     INatureLigneFonctionnelleService natureLigneFonctionnelleService,
                                     LigneFonctionnelleSpecifiqueReader ligneFonctionnelleSpecifiqueReader,
                                     LigneFonctionnelleReader ligneFonctionnelleReader,
                                     StaportPdfReportService staportPdfReportService) {
        this.droitConstateRepository = droitConstateRepository;
        this.droitConstateMapper = droitConstateMapper;
        this.marcheService = marcheService;
        this.banqueService = banqueService;
        this.natureLigneFonctionnelleService = natureLigneFonctionnelleService;
        this.ligneFonctionnelleSpecifiqueReader = ligneFonctionnelleSpecifiqueReader;
        this.ligneFonctionnelleReader = ligneFonctionnelleReader;
        this.staportPdfReportService = staportPdfReportService;
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.CREATE, entity = DroitConstate.class)
    public DroitConstateResponseDto createDroitConstate(DroitConstateRequestDto requestDTO) throws Exception {
        log.info("Début de la création d'un droit constaté pour le marché ID: {}", requestDTO.marcheId());

        Marche marche = marcheService.findOrThrow(requestDTO.marcheId());
        log.debug("Marché récupéré: {} (Numéro: {})", marche.getNom(), marche.getNumero());

        NatureLigneFonctionnelle dcNature = getDroitConstateNature();
        LigneResult ligneResult = findActiveDroitConstateLine(marche, dcNature);

        Banque banque = banqueService.findOrThrow(requestDTO.banqueId());
        log.debug("Banque résolue pour le DC: {} (ID: {})", banque.getNom(), banque.getId());

        DroitConstate droitConstate = droitConstateMapper.toEntity(requestDTO, marche, banque);
        droitConstate.setLigneFonctionnelleSpecifique(ligneResult.specifique());
        droitConstate.setLigneFonctionnelle(ligneResult.generale());
        droitConstate.setObservation(marche.getNumero());
        droitConstate.setLibelle(marche.getNom() + " - " + droitConstate.getType());

        droitConstate = droitConstateRepository.save(droitConstate);
        log.info("Droit constaté créé avec succès - ID: {}", droitConstate.getId());

        return droitConstateMapper.toDto(droitConstate);
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = DroitConstate.class)
    public DroitConstateResponseDto updateDroitConstate(Long id, DroitConstateRequestDto requestDTO) throws Exception {
        log.info("Début de la modification du droit constaté ID: {}", id);

        DroitConstate existingDroitConstate = droitConstateRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Droit constaté non trouvé avec l'ID: {}", id);
                    return new ResourceNotFoundException("Droit constaté not found with ID: " + id);
                });

        Marche marche = marcheService.findOrThrow(requestDTO.marcheId());
        log.debug("Marché récupéré pour la mise à jour: {}", marche.getNom());

        if (!existingDroitConstate.getMarche().getId().equals(marche.getId())) {
            log.warn("Changement de marché détecté (ancien: {}, nouveau: {}), re-validation nécessaire",
                    existingDroitConstate.getMarche().getNom(), marche.getNom());

            LigneResult ligneResult = findActiveDroitConstateLine(marche, getDroitConstateNature());
            existingDroitConstate.setLigneFonctionnelleSpecifique(ligneResult.specifique());
            existingDroitConstate.setLigneFonctionnelle(ligneResult.generale());
        }

        Banque banque = banqueService.findOrThrow(requestDTO.banqueId());
        log.debug("Banque résolue pour la mise à jour du DC: {} (ID: {})", banque.getNom(), banque.getId());

        existingDroitConstate.setMarche(marche);
        existingDroitConstate.setBanque(banque);
        existingDroitConstate.setDateEmission(requestDTO.dateEmission());
        existingDroitConstate.setMontant(requestDTO.montant());
        existingDroitConstate.setType(requestDTO.type());
        existingDroitConstate.setObservation(marche.getNumero());
        existingDroitConstate.setLibelle(marche.getNom() + " - " + existingDroitConstate.getType());

        DroitConstate updatedDroitConstate = droitConstateRepository.save(existingDroitConstate);
        log.info("Droit constaté modifié avec succès - ID: {}", id);

        return droitConstateMapper.toDto(updatedDroitConstate);
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.DELETE, entity = DroitConstate.class)
    public void deleteDroitConstate(Long id) {
        log.info("Tentative de suppression du droit constaté ID: {}", id);

        DroitConstate droitConstate = droitConstateRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Impossible de supprimer - Droit constaté non trouvé avec l'ID: {}", id);
                    return new ResourceNotFoundException("Droit constate not found with ID: " + id);
                });

        if (droitConstate.getStatus() != Status.PREVU) {
            log.warn("Tentative de suppression d'un droit constaté déjà réglé — id: {}", id);
            throw new ValidationException("Le droit constaté id: " + id + " est déjà réglé et ne peut plus être supprimé.");
        }

        droitConstateRepository.deleteById(id);
        log.info("Droit constaté ID: {} supprimé avec succès", id);
    }

    @Override
    @Transactional(readOnly = true)
    public DroitConstateResponseDto findById(Long id) {
        DroitConstate droitConstate = droitConstateRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Droit constaté non trouvé avec l'ID: {}", id);
                    return new ResourceNotFoundException("Droit constaté not found with ID: " + id);
                });

        return droitConstateMapper.toDto(droitConstate);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DroitConstateResponseDto> findAll(DroitConstateSearchDto searchDTO, Pageable pageable) {
        log.info("Recherche de droits constatés avec critères: {}", searchDTO);

        Specification<DroitConstate> spec = DroitConstateSpec.searchByCriteria(searchDTO);
        Page<DroitConstate> droitConstatePage = droitConstateRepository.findAll(spec,
                pageable.getSort().isSorted() ? pageable
                        : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));

        log.info("Recherche terminée - {} résultat(s) trouvé(s) sur {} élément(s) au total",
                droitConstatePage.getNumberOfElements(), droitConstatePage.getTotalElements());

        return droitConstatePage.map(droitConstateMapper::toDto);
    }

    @Override
    @Transactional
    public DroitConstateResponseDto toggleStatus(Long id) {
        log.info("Changement du statut du droit constaté ID: {}", id);

        DroitConstate droitConstate = droitConstateRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Droit constaté non trouvé avec l'ID: {}", id);
                    return new ResourceNotFoundException("Droit constate not found with ID: " + id);
                });

        Status existingStatus = droitConstate.getStatus();
        Status newStatus = existingStatus == Status.PAYE ? Status.PREVU : Status.PAYE;

        log.debug("Changement de statut - Ancien: {}, Nouveau: {}", existingStatus, newStatus);
        droitConstate.setStatus(newStatus);
        DroitConstate updatedDroitConstate = droitConstateRepository.save(droitConstate);

        log.info("Statut du droit constaté ID: {} modifié avec succès de {} vers {}", id, existingStatus, newStatus);

        return droitConstateMapper.toDto(updatedDroitConstate);
    }

    // ── Export PDF ────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public byte[] exportPdf(DroitConstateSearchDto searchDTO) {
        log.info("Export PDF droits constatés — critères: {}", searchDTO);

        List<DroitConstate> droitConstates = droitConstateRepository.findAll(
                DroitConstateSpec.searchByCriteria(searchDTO),
                Sort.by(Sort.Direction.DESC, "dateEmission"));

        BigDecimal totalMontant = droitConstates.stream()
                .map(DroitConstate::getMontant)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<String> filterLines = buildFilterSummary(searchDTO);

        return generateDroitConstatePdf("État des Droits Constatés", droitConstates, totalMontant, filterLines);
    }

    /**
     * Génère le contenu PDF utilisé par {@link #exportPdf}. Contrairement à Préfinancement/
     * Factoring, la colonne "Banque" utilise le champ direct {@code DroitConstate.banque} (pas
     * {@code marche.banque}).
     */
    private byte[] generateDroitConstatePdf(String title, List<DroitConstate> droitConstates,
                                             BigDecimal totalMontant, List<String> filterLines) {
        try {
            return staportPdfReportService.generateReport(
                    title,
                    PageSize.A4.rotate(),
                    (doc, writer) -> {
                        try {
                            doc.add(Chunk.NEWLINE);
                            doc.add(Chunk.NEWLINE);

                            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
                            Font subFont   = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.decode("#374151"));
                            Font hdrFont   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
                            Font cellFont  = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
                            Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.decode("#1e3a8a"));

                            Paragraph titlePara = new Paragraph(title, titleFont);
                            titlePara.setAlignment(Element.ALIGN_CENTER);
                            doc.add(titlePara);

                            String exportInfo = "Exporté le " + LocalDate.now().format(DATE_FMT)
                                    + "  •  " + droitConstates.size() + " ligne(s)";
                            if (!filterLines.isEmpty()) {
                                exportInfo += "  •  Filtres : " + String.join("  |  ", filterLines);
                            }
                            Paragraph sub = new Paragraph(exportInfo, subFont);
                            sub.setAlignment(Element.ALIGN_CENTER);
                            sub.setSpacingAfter(8f);
                            doc.add(sub);

                            Color medBlue    = Color.decode("#2563eb");
                            Color lightBlue  = Color.decode("#dbeafe");
                            Color rowAlt     = Color.decode("#f8fafc");
                            Color green      = Color.decode("#16a34a");
                            Color orange     = Color.decode("#ea580c");
                            Color borderGray = Color.decode("#e5e7eb");

                            float[] colWidths = {10f, 16f, 12f, 11f, 13f, 11f, 11f, 8f};
                            PdfPTable table = new PdfPTable(colWidths);
                            table.setWidthPercentage(100f);
                            table.setSpacingBefore(8f);
                            table.setHeaderRows(1);

                            for (String h : new String[]{
                                    "N° Marché", "Nom Marché", "Banque", "Type",
                                    "Montant (MAD)", "Date Émission", "Date Valeur", "Statut"}) {
                                PdfPCell c = new PdfPCell(new Phrase(h, hdrFont));
                                c.setBackgroundColor(medBlue);
                                c.setPadding(5f);
                                c.setHorizontalAlignment(Element.ALIGN_CENTER);
                                c.setBorderColor(Color.WHITE);
                                c.setBorderWidth(0.5f);
                                table.addCell(c);
                            }

                            for (int i = 0; i < droitConstates.size(); i++) {
                                DroitConstate d = droitConstates.get(i);
                                Marche marche = d.getMarche();
                                Color rowBg = i % 2 == 0 ? Color.WHITE : rowAlt;

                                addPdfCell(table, marche != null ? marche.getNumero() : "—", cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                                addPdfCell(table, marche != null ? marche.getNom() : "—", cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                                addPdfCell(table, d.getBanque() != null ? d.getBanque().getNom() : "—", cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                                addPdfCell(table, d.getType() == Type.DEBLOCAGE ? "Déblocage" : "Remboursement", cellFont, rowBg, borderGray, Element.ALIGN_CENTER);
                                addPdfCell(table, MontantFormatter.format(d.getMontant()), cellFont, rowBg, borderGray, Element.ALIGN_RIGHT);
                                addPdfCell(table, d.getDateEmission() != null ? d.getDateEmission().format(DATE_FMT) : "—", cellFont, rowBg, borderGray, Element.ALIGN_CENTER);
                                addPdfCell(table, d.getDateValeur() != null ? d.getDateValeur().format(DATE_FMT) : "—", cellFont, rowBg, borderGray, Element.ALIGN_CENTER);

                                boolean paye = d.getStatus() == Status.PAYE;
                                PdfPCell statusCell = new PdfPCell(new Phrase(
                                        paye ? "PAYÉ" : "PRÉVU",
                                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE)));
                                statusCell.setBackgroundColor(paye ? green : orange);
                                statusCell.setPadding(4f);
                                statusCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                                statusCell.setBorderColor(borderGray);
                                statusCell.setBorderWidth(0.5f);
                                table.addCell(statusCell);
                            }

                            // Ligne total
                            PdfPCell totalLabel = new PdfPCell(
                                    new Phrase("TOTAL — " + droitConstates.size() + " ligne(s)", totalFont));
                            totalLabel.setColspan(4);
                            totalLabel.setBackgroundColor(lightBlue);
                            totalLabel.setPadding(5f);
                            totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
                            totalLabel.setBorderColor(medBlue);
                            table.addCell(totalLabel);

                            PdfPCell totalVal = new PdfPCell(
                                    new Phrase(MontantFormatter.format(totalMontant) + " MAD", totalFont));
                            totalVal.setBackgroundColor(lightBlue);
                            totalVal.setPadding(5f);
                            totalVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
                            totalVal.setBorderColor(medBlue);
                            table.addCell(totalVal);

                            for (int j = 0; j < 3; j++) {
                                PdfPCell empty = new PdfPCell(new Phrase(""));
                                empty.setBackgroundColor(lightBlue);
                                empty.setBorderColor(medBlue);
                                table.addCell(empty);
                            }

                            doc.add(table);

                        } catch (DocumentException e) {
                            throw new RuntimeException("Erreur génération contenu PDF droits constatés", e);
                        }
                    }
            );
        } catch (DocumentException e) {
            throw new RuntimeException("Erreur lors de la génération du PDF des droits constatés", e);
        }
    }

    private void addPdfCell(PdfPTable table, String text, Font font,
                             Color bg, Color border, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "—", font));
        cell.setBackgroundColor(bg);
        cell.setPadding(4f);
        cell.setHorizontalAlignment(align);
        cell.setBorderColor(border);
        cell.setBorderWidth(0.5f);
        table.addCell(cell);
    }

    private List<String> buildFilterSummary(DroitConstateSearchDto criteria) {
        List<String> lines = new ArrayList<>();
        if (criteria == null) return lines;

        if (criteria.marcheId() != null) {
            try {
                lines.add("Marché : " + marcheService.findOrThrow(criteria.marcheId()).getNumero());
            } catch (ResourceNotFoundException ignored) { /* filtre obsolète — ignoré pour l'affichage */ }
        }
        if (criteria.banqueId() != null) {
            try {
                lines.add("Banque : " + banqueService.findOrThrow(criteria.banqueId()).getNom());
            } catch (ResourceNotFoundException ignored) { /* filtre obsolète — ignoré pour l'affichage */ }
        }
        if (criteria.type() != null)
            lines.add("Type : " + (criteria.type() == Type.DEBLOCAGE ? "Déblocage" : "Remboursement"));
        if (criteria.status() != null)
            lines.add("Statut : " + (criteria.status() == Status.PAYE ? "Payé" : "Prévu"));

        return lines;
    }

    private NatureLigneFonctionnelle getDroitConstateNature() {
        log.debug("Recherche de la nature de ligne fonctionnelle Droit Constaté (code={})", DROIT_CONSTATE_NATURE_CODE);
        return natureLigneFonctionnelleService.findByCode(DROIT_CONSTATE_NATURE_CODE)
                .orElseThrow(() -> {
                    log.error("Nature de ligne fonctionnelle 'DROIT_CONSTATE' introuvable en base");
                    return new ResourceNotFoundException(
                            "Nature de ligne fonctionnelle DROIT_CONSTATE introuvable. " +
                            "Vérifiez la table nature_ligne_fonctionnelle."
                    );
                });
    }

    /**
     * Recherche et retourne la ligne fonctionnelle 'Droit constaté' active pour un marché.
     * Priorité : spécifique > générale liée au marché.
     * Lève une exception si aucune ligne n'est trouvée.
     */
    private LigneResult findActiveDroitConstateLine(Marche marche, NatureLigneFonctionnelle dcNature) throws Exception {
        log.debug("Recherche d'une ligne 'Droit constaté' active pour le marché '{}' (ID: {})", marche.getNom(), marche.getId());

        Optional<LigneFonctionnelleSpecifique> ligneSpecifique =
                ligneFonctionnelleSpecifiqueReader.findByMarcheAndNatureLigneFonctionnelleAndActifTrue(marche, dcNature);
        if (ligneSpecifique.isPresent()) {
            log.info("Ligne spécifique 'Droit constaté' active trouvée pour le marché '{}' (ID: {})",
                    marche.getNom(), ligneSpecifique.get().getId());
            return new LigneResult(ligneSpecifique.get(), null);
        }

        Optional<LigneFonctionnelle> ligneGenerale =
                ligneFonctionnelleReader.findActiveLineForMarche(marche.getId(), dcNature.getId());
        if (ligneGenerale.isPresent()) {
            log.info("Ligne générale 'Droit constaté' active trouvée pour le marché '{}' (ID: {})",
                    marche.getNom(), ligneGenerale.get().getId());
            return new LigneResult(null, ligneGenerale.get());
        }

        log.error("Aucune ligne 'Droit constaté' active pour le marché '{}'", marche.getNom());
        throw new Exception(
                "Impossible de créer un droit constaté. Le marché '" + marche.getNom() +
                "' n'est associé à aucune ligne fonctionnelle 'Droit Constaté' active " +
                "(ni spécifique, ni générale liée à ce marché)."
        );
    }

    /** Résultat de la recherche de ligne : exactement l'une des deux est non-null. */
    private record LigneResult(LigneFonctionnelleSpecifique specifique, LigneFonctionnelle generale) {}
}
