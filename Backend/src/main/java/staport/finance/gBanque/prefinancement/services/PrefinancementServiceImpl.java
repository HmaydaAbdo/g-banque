package staport.finance.gBanque.prefinancement.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.services.INatureLigneFonctionnelleService;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.entities.LigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.services.LigneFonctionnelleReader;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities.LigneFonctionnelleSpecifique;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.services.LigneFonctionnelleSpecifiqueReader;
import staport.finance.gBanque.Bank.services.IBanqueService;
import staport.finance.gBanque.audit.AuditOperation;
import staport.finance.gBanque.audit.AuditableAction;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.marches.service.IMarcheService;
import staport.finance.gBanque.prefinancement.dtos.request.PrefinancementRequestDTO;
import staport.finance.gBanque.prefinancement.dtos.request.PrefinancementSearchDTO;
import staport.finance.gBanque.prefinancement.dtos.response.PrefinancementResponseDTO;
import staport.finance.gBanque.prefinancement.entities.Prefinancement;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prefinancement.enums.Type;
import staport.finance.gBanque.prefinancement.mapper.PrefinancementMapper;
import staport.finance.gBanque.prefinancement.repository.PrefinancementRepository;
import staport.finance.gBanque.prefinancement.spec.PrefinancementSpecification;
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
 * Implémentation du service Préfinancement.
 *
 * <p>Encapsulation des domaines (architecture.md) : la résolution du {@link Marche} passe par
 * {@link IMarcheService}, la résolution de la nature de ligne fonctionnelle "PREFINANCEMENT" par
 * {@link INatureLigneFonctionnelleService} — jamais par injection directe d'un {@code Repository}
 * étranger. {@code LigneFonctionnelleReader}/{@code LigneFonctionnelleSpecifiqueReader} restent
 * inchangés (déjà des façades de lecture cross-domaine conformes).
 *
 * <p>Pas de risque de cycle de beans : {@code ILigneFonctionnelleService}/
 * {@code ILigneFonctionnelleSpecifiqueService} (injectés par {@code NatureLigneFonctionnelleService})
 * n'injectent que {@link PrefinancementReader} (bean distinct), jamais {@link IPrefinancementService}.
 *
 * <p>Anti-N+1 : {@code PrefinancementRepository.findAll} porte l'{@code @EntityGraph}
 * {@code "Prefinancement.avecAssociations"} (marché + banque du marché).
 *
 * <p>{@link IBanqueService} est injecté uniquement pour résoudre le nom de banque affiché dans le
 * résumé des filtres de {@link #exportPdf} — sans risque de cycle de bean : {@code BanqueServiceImpl}
 * ne dépend d'aucune classe du module Préfinancement (vérifié par recherche globale).
 *
 * <p>19/09/2026 : les encours de remboursement/déblocage par banque (écrans dédiés
 * {@code prefinancement-remboursements}/{@code prefinancement-deblocage}) ont été retirés — le
 * récapitulatif unifié couvre désormais la validation de cette source. {@code getRemboursementEncoursForBanque}/
 * {@code getDeblocageEncoursForBanque}/leurs exports PDF et le calcul de somme par
 * {@code EntityManager} qu'ils utilisaient seuls ont été retirés avec eux.
 */
@Service
public class PrefinancementServiceImpl implements IPrefinancementService {

    private static final Logger log = LoggerFactory.getLogger(PrefinancementServiceImpl.class);
    private static final String PREFINANCEMENT_NATURE_CODE = "PREFINANCEMENT";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PrefinancementRepository prefinancementRepository;
    private final IMarcheService marcheService;
    private final IBanqueService banqueService;
    private final PrefinancementMapper prefinancementMapper;
    private final LigneFonctionnelleSpecifiqueReader ligneFonctionnelleSpecifiqueReader;
    private final INatureLigneFonctionnelleService natureLigneFonctionnelleService;
    private final LigneFonctionnelleReader ligneFonctionnelleReader;
    private final StaportPdfReportService staportPdfReportService;

    public PrefinancementServiceImpl(
            PrefinancementRepository prefinancementRepository,
            IMarcheService marcheService,
            IBanqueService banqueService,
            PrefinancementMapper prefinancementMapper,
            LigneFonctionnelleSpecifiqueReader ligneFonctionnelleSpecifiqueReader,
            INatureLigneFonctionnelleService natureLigneFonctionnelleService,
            LigneFonctionnelleReader ligneFonctionnelleReader,
            StaportPdfReportService staportPdfReportService) {
        this.prefinancementRepository = prefinancementRepository;
        this.marcheService = marcheService;
        this.banqueService = banqueService;
        this.prefinancementMapper = prefinancementMapper;
        this.ligneFonctionnelleSpecifiqueReader = ligneFonctionnelleSpecifiqueReader;
        this.natureLigneFonctionnelleService = natureLigneFonctionnelleService;
        this.ligneFonctionnelleReader = ligneFonctionnelleReader;
        this.staportPdfReportService = staportPdfReportService;
        log.info("PrefinancementServiceImpl initialisé avec succès");
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.CREATE, entity = Prefinancement.class)
    public PrefinancementResponseDTO createPrefinancement(PrefinancementRequestDTO requestDTO) throws Exception {
        log.info("Début de création d'un préfinancement pour le marché ID: {}", requestDTO.marcheId());

        Marche marche = marcheService.findOrThrow(requestDTO.marcheId());
        log.debug("Marché trouvé - Nom: {}, Numéro: {}", marche.getNom(), marche.getNumero());

        NatureLigneFonctionnelle prefiNature = getPrefinancementNature();
        LigneResult ligneResult = findActivePrefinancementLine(marche, prefiNature);

        Prefinancement prefinancement = prefinancementMapper.toEntity(requestDTO, marche);
        prefinancement.setLigneFonctionnelleSpecifique(ligneResult.specifique());
        prefinancement.setLigneFonctionnelle(ligneResult.generale());
        prefinancement.setObservation(marche.getNumero());
        prefinancement.setLibelle(marche.getNom() + " - " + prefinancement.getType());

        prefinancement = prefinancementRepository.save(prefinancement);
        log.info("Préfinancement créé avec succès - ID: {}", prefinancement.getId());

        return prefinancementMapper.toDto(prefinancement);
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = Prefinancement.class)
    public PrefinancementResponseDTO updatePrefinancement(Long id, PrefinancementRequestDTO requestDTO) throws Exception {
        log.info("Début de mise à jour du préfinancement ID: {}", id);

        Prefinancement existingPrefinancement = prefinancementRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Préfinancement non trouvé avec l'ID: {}", id);
                    return new ResourceNotFoundException("Prefinancement not found with ID: " + id);
                });

        Marche marche = marcheService.findOrThrow(requestDTO.marcheId());
        log.debug("Marché trouvé pour mise à jour - Nom: {}, Numéro: {}", marche.getNom(), marche.getNumero());

        if (!existingPrefinancement.getMarche().getId().equals(marche.getId())) {
            log.warn("Changement de marché détecté (ancien: {}, nouveau: {}), re-validation nécessaire",
                    existingPrefinancement.getMarche().getNom(), marche.getNom());

            NatureLigneFonctionnelle prefiNature = getPrefinancementNature();
            LigneResult ligneResult = findActivePrefinancementLine(marche, prefiNature);
            existingPrefinancement.setLigneFonctionnelleSpecifique(ligneResult.specifique());
            existingPrefinancement.setLigneFonctionnelle(ligneResult.generale());
        }

        existingPrefinancement.setMarche(marche);
        existingPrefinancement.setDateEmission(requestDTO.dateEmission());
        existingPrefinancement.setMontant(requestDTO.montant());
        existingPrefinancement.setType(requestDTO.type());
        existingPrefinancement.setObservation(marche.getNumero());
        existingPrefinancement.setLibelle(marche.getNom() + " - " + existingPrefinancement.getType());

        Prefinancement updatedPrefinancement = prefinancementRepository.save(existingPrefinancement);
        log.info("Préfinancement mis à jour avec succès - ID: {}", updatedPrefinancement.getId());

        return prefinancementMapper.toDto(updatedPrefinancement);
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.DELETE, entity = Prefinancement.class)
    public void deletePrefinancement(Long id) {
        log.info("Début de suppression du préfinancement ID: {}", id);

        Prefinancement prefinancement = prefinancementRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Impossible de supprimer - Préfinancement non trouvé avec l'ID: {}", id);
                    return new ResourceNotFoundException("Prefinancement not found with ID: " + id);
                });

        if (prefinancement.getStatus() != Status.PREVU) {
            log.warn("Tentative de suppression d'un préfinancement déjà réglé — id: {}", id);
            throw new ValidationException("Le préfinancement id: " + id + " est déjà réglé et ne peut plus être supprimé.");
        }

        prefinancementRepository.deleteById(id);
        log.info("Préfinancement supprimé avec succès - ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public PrefinancementResponseDTO findById(Long id) {
        Prefinancement prefinancement = prefinancementRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Préfinancement non trouvé avec l'ID: {}", id);
                    return new ResourceNotFoundException("Prefinancement not found with ID: " + id);
                });

        return prefinancementMapper.toDto(prefinancement);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PrefinancementResponseDTO> findAll(PrefinancementSearchDTO searchDTO, Pageable pageable) {
        log.info("Recherche de préfinancement avec critères - Marché ID: {}, Banque ID: {}, Type: {}, Statut: {}",
                searchDTO.marcheId(), searchDTO.banqueId(), searchDTO.type(), searchDTO.status());

        Specification<Prefinancement> spec = PrefinancementSpecification.searchByCriteria(searchDTO);

        Page<Prefinancement> prefinancementPage = prefinancementRepository.findAll(spec,
                pageable.getSort().isSorted() ? pageable
                        : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));

        log.info("Recherche terminée - {} enregistrement(s) trouvé(s) sur {} page(s)",
                prefinancementPage.getTotalElements(), prefinancementPage.getTotalPages());

        return prefinancementPage.map(prefinancementMapper::toDto);
    }

    @Override
    @Transactional
    public PrefinancementResponseDTO toggleStatus(Long id) {
        log.info("Début de basculement du statut pour le préfinancement ID: {}", id);

        Prefinancement prefinancement = prefinancementRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Préfinancement non trouvé avec l'ID: {}", id);
                    return new ResourceNotFoundException("Prefinancement not found with ID: " + id);
                });

        Status existingStatus = prefinancement.getStatus();
        Status newStatus = existingStatus == Status.PAYE ? Status.PREVU : Status.PAYE;

        log.debug("Changement de statut - Ancien: {}, Nouveau: {}", existingStatus, newStatus);
        prefinancement.setStatus(newStatus);

        Prefinancement updatedPrefinancement = prefinancementRepository.save(prefinancement);
        log.info("Statut du préfinancement basculé avec succès - ID: {}, Nouveau statut: {}",
                updatedPrefinancement.getId(), updatedPrefinancement.getStatus());

        return prefinancementMapper.toDto(updatedPrefinancement);
    }

    // ── Export PDF ────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public byte[] exportPdf(PrefinancementSearchDTO searchDTO) {
        log.info("Export PDF préfinancements — critères: {}", searchDTO);

        List<Prefinancement> prefinancements = prefinancementRepository.findAll(
                PrefinancementSpecification.searchByCriteria(searchDTO),
                Sort.by(Sort.Direction.DESC, "dateEmission"));

        BigDecimal totalMontant = prefinancements.stream()
                .map(Prefinancement::getMontant)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<String> filterLines = buildFilterSummary(searchDTO);

        return generatePrefinancementPdf("État des Préfinancements", prefinancements, totalMontant, filterLines);
    }

    /**
     * Génère le contenu PDF utilisé par {@link #exportPdf}.
     */
    private byte[] generatePrefinancementPdf(String title, List<Prefinancement> prefinancements,
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
                                    + "  •  " + prefinancements.size() + " ligne(s)";
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

                            for (int i = 0; i < prefinancements.size(); i++) {
                                Prefinancement p = prefinancements.get(i);
                                Marche marche = p.getMarche();
                                Color rowBg = i % 2 == 0 ? Color.WHITE : rowAlt;

                                addPdfCell(table, marche != null ? marche.getNumero() : "—", cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                                addPdfCell(table, marche != null ? marche.getNom() : "—", cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                                addPdfCell(table, marche != null && marche.getBanque() != null ? marche.getBanque().getNom() : "—", cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                                addPdfCell(table, p.getType() == Type.DEBLOCAGE ? "Déblocage" : "Remboursement", cellFont, rowBg, borderGray, Element.ALIGN_CENTER);
                                addPdfCell(table, MontantFormatter.format(p.getMontant()), cellFont, rowBg, borderGray, Element.ALIGN_RIGHT);
                                addPdfCell(table, p.getDateEmission() != null ? p.getDateEmission().format(DATE_FMT) : "—", cellFont, rowBg, borderGray, Element.ALIGN_CENTER);
                                addPdfCell(table, p.getDateValeur() != null ? p.getDateValeur().format(DATE_FMT) : "—", cellFont, rowBg, borderGray, Element.ALIGN_CENTER);

                                boolean paye = p.getStatus() == Status.PAYE;
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
                                    new Phrase("TOTAL — " + prefinancements.size() + " ligne(s)", totalFont));
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
                            throw new RuntimeException("Erreur génération contenu PDF préfinancements", e);
                        }
                    }
            );
        } catch (DocumentException e) {
            throw new RuntimeException("Erreur lors de la génération du PDF des préfinancements", e);
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

    private List<String> buildFilterSummary(PrefinancementSearchDTO criteria) {
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

    private NatureLigneFonctionnelle getPrefinancementNature() {
        log.debug("Recherche de la nature de ligne fonctionnelle Préfinancement (code={})", PREFINANCEMENT_NATURE_CODE);
        return natureLigneFonctionnelleService.findByCode(PREFINANCEMENT_NATURE_CODE)
                .orElseThrow(() -> {
                    log.error("Nature de ligne fonctionnelle 'PREFINANCEMENT' introuvable en base");
                    return new ResourceNotFoundException(
                            "Nature de ligne fonctionnelle PREFINANCEMENT introuvable. " +
                            "Vérifiez la table nature_ligne_fonctionnelle."
                    );
                });
    }

    /**
     * Recherche et retourne la ligne fonctionnelle Préfinancement active pour un marché.
     * Priorité : spécifique > générale liée au marché.
     * Lève une exception si aucune ligne n'est trouvée.
     */
    private LigneResult findActivePrefinancementLine(Marche marche, NatureLigneFonctionnelle prefiNature) throws Exception {
        log.debug("Recherche d'une ligne Préfinancement active pour le marché ID: {}", marche.getId());

        Optional<LigneFonctionnelleSpecifique> ligneSpecifique =
                ligneFonctionnelleSpecifiqueReader.findByMarcheAndNatureLigneFonctionnelleAndActifTrue(marche, prefiNature);
        if (ligneSpecifique.isPresent()) {
            log.info("Ligne spécifique Préfinancement active trouvée pour le marché '{}' (ID: {})",
                    marche.getNom(), ligneSpecifique.get().getId());
            return new LigneResult(ligneSpecifique.get(), null);
        }

        Optional<LigneFonctionnelle> ligneGenerale =
                ligneFonctionnelleReader.findActiveLineForMarche(marche.getId(), prefiNature.getId());
        if (ligneGenerale.isPresent()) {
            log.info("Ligne générale Préfinancement active trouvée pour le marché '{}' (ID: {})",
                    marche.getNom(), ligneGenerale.get().getId());
            return new LigneResult(null, ligneGenerale.get());
        }

        log.error("Aucune ligne Préfinancement active pour le marché '{}'", marche.getNom());
        throw new Exception(
                "Impossible de créer un Préfinancement. Le marché '" + marche.getNom() +
                "' n'est associé à aucune ligne fonctionnelle Préfinancement active " +
                "(ni spécifique, ni générale liée à ce marché)."
        );
    }

    /** Résultat de la recherche de ligne : exactement l'une des deux est non-null. */
    private record LigneResult(LigneFonctionnelleSpecifique specifique, LigneFonctionnelle generale) {}
}
