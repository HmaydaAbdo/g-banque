package staport.finance.gBanque.moyensDePaiements.virements.service;

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
import staport.finance.gBanque.audit.AuditOperation;
import staport.finance.gBanque.audit.AuditableAction;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.moyensDePaiements.virements.dtos.*;
import staport.finance.gBanque.moyensDePaiements.virements.entities.Virement;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.moyensDePaiements.virements.exceptions.VirementBusinessException;
import staport.finance.gBanque.moyensDePaiements.virements.mapper.VirementMapper;
import staport.finance.gBanque.moyensDePaiements.virements.metrics.VirementMetrics;
import staport.finance.gBanque.moyensDePaiements.virements.repositories.VirementRepository;
import staport.finance.gBanque.moyensDePaiements.virements.specs.VirementSpec;
import staport.finance.gBanque.reports.service.StaportPdfReportService;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import java.awt.Color;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;

/**
 * Service métier du module Virement.
 *
 * <p>Façade publique du domaine {@code virements} (architecture.md — Encapsulation des domaines) :
 * les autres modules doivent passer par ce service, jamais par {@code VirementRepository} directement.
 */
@Service
public class VirementService {

    private static final Logger log = LoggerFactory.getLogger(VirementService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final VirementRepository virementRepository;
    private final IBanqueService banqueService;
    private final VirementMapper virementMapper;
    private final VirementMetrics virementMetrics;
    private final StaportPdfReportService staportPdfReportService;

    public VirementService(VirementRepository virementRepository, IBanqueService banqueService,
                            VirementMapper virementMapper, VirementMetrics virementMetrics,
                            StaportPdfReportService staportPdfReportService) {
        this.virementRepository = virementRepository;
        this.banqueService = banqueService;
        this.virementMapper = virementMapper;
        this.virementMetrics = virementMetrics;
        this.staportPdfReportService = staportPdfReportService;
    }

    @Transactional
    @AuditableAction(operation = AuditOperation.CREATE, entity = Virement.class)
    public VirementResponseDto createEncaissementVirement(VirementRequestDto dto) {
        log.info("Création d'un nouveau virement d'encaissement: {}", dto);

        Banque banque = banqueService.findOrThrow(dto.getBanqueBeneficiaireId());
        Virement virement = virementMapper.toEntity(dto, banque);
        virement = virementRepository.save(virement);
        virementMetrics.incrementerCreation(virement.getType());

        log.info("Virement créé avec succès avec l'ID: {}", virement.getId());
        return toDto(virement);
    }

    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = Virement.class)
    public VirementResponseDto editEncaissementVirement(Long id, VirementRequestDto dto) {
        log.info("Modification du virement d'encaissement ID={} avec les données={}", id, dto);

        Virement existingVirement = trouverOuLeverException(id);

        if (existingVirement.getType() != Type.ENCAISSEMENT) {
            log.warn("Tentative de modification d'un virement qui n'est pas de type ENCAISSEMENT, ID={}", id);
            throw new VirementBusinessException("Modification interdite: le virement with id " + id + " n'est pas de type ENCAISSEMENT.");
        }

        if (existingVirement.getStatut() == StatutMoyenPaiement.REGLE) {
            log.warn("Tentative de modification d'un virement déjà réglé, ID={}", id);
            throw new VirementBusinessException("Modification interdite: le virement with id " + id + " est déjà réglé.");
        }

        if (dto.getMontant() != null) {
            existingVirement.setMontant(dto.getMontant());
        }
        if (dto.getDateReception() != null) {
            existingVirement.setDateEmission(dto.getDateReception());
        }
        if (dto.getEmetteur() != null) {
            existingVirement.setEmetteur(dto.getEmetteur());
        }
        if (dto.getBanqueBeneficiaireId() != null) {
            existingVirement.setBanqueBeneficiare(banqueService.findOrThrow(dto.getBanqueBeneficiaireId()));
        }

        existingVirement = virementRepository.save(existingVirement);
        log.info("Virement ID={} mis à jour avec succès", id);
        return toDto(existingVirement);
    }

    @Transactional(readOnly = true)
    public VirementResponseDto getVirementById(Long id) {
        log.debug("Récupération du virement avec l'ID={}", id);
        return toDto(trouverOuLeverException(id));
    }

    @Transactional(readOnly = true)
    public Page<VirementResponseDto> getEncaissementVirements(VirementSearchDto searchDto, Pageable pageable) {
        log.debug("Recherche des virements d'encaissement avec les filtres={} et pagination={}", searchDto, pageable);

        Specification<Virement> spec = VirementSpec.bySearchDto(searchDto)
                .and(VirementSpec.hasType(Type.ENCAISSEMENT));

        Page<Virement> virementPage = virementRepository.findAll(spec, stablePageable(pageable));
        log.debug("{} virement(s) d'encaissement trouvé(s)", virementPage.getTotalElements());
        return virementPage.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<VirementResponseDto> getDecaissementVirements(VirementSearchDto searchDto, Pageable pageable) {
        log.debug("Recherche des virements de décaissement avec les filtres={} et pagination={}", searchDto, pageable);

        Specification<Virement> spec = VirementSpec.bySearchDto(searchDto)
                .and(VirementSpec.hasType(Type.DECAISSEMENT));

        Page<Virement> virementPage = virementRepository.findAll(spec, stablePageable(pageable));
        log.debug("{} virement(s) de décaissement trouvé(s)", virementPage.getTotalElements());
        return virementPage.map(this::toDto);
    }

    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = Virement.class)
    public VirementResponseDto toggleVirementStatus(Long id) {
        log.info("Basculement du statut pour le virement ID={}", id);

        Virement virement = trouverOuLeverException(id);

        if (virement.getStatut() == StatutMoyenPaiement.ANNULE) {
            log.warn("Tentative de basculement du statut d'un virement annulé, ID={}", id);
            throw new VirementBusinessException("Impossible de basculer le statut d'un virement annulé (ID=" + id + ").");
        }
        virement.setStatut(virement.getStatut() == StatutMoyenPaiement.ENCOURS ? StatutMoyenPaiement.REGLE : StatutMoyenPaiement.ENCOURS);
        virement = virementRepository.save(virement);
        if (virement.getStatut() == StatutMoyenPaiement.REGLE) {
            virementMetrics.incrementerPassageRegle();
        }

        log.info("Statut basculé avec succès pour le virement ID={} — nouveau statut={}", id, virement.getStatut());
        return toDto(virement);
    }

    @Transactional(readOnly = true)
    public EncoursVirementResponseDto getEncoursDecaissementForVirements(EncoursVirementsSearchDto searchDto, Long banqueId, Pageable pageable) {
        log.debug("Récupération des encours de décaissement pour la banque ID={} avec les filtres={}", banqueId, searchDto);

        Specification<Virement> spec = VirementSpec.encoursDecaissement(banqueId, finDuMoisCourant())
                .and(VirementSpec.hasDateEmission(searchDto.getDateEmission()))
                .and(VirementSpec.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(VirementSpec.hasMontant(searchDto.getMontant()));

        Page<Virement> virementPage = virementRepository.findAll(spec, stablePageable(pageable));

        BigDecimal totalDesEncours = virementRepository.findAll(spec).stream()
                .map(Virement::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);

        log.debug("Total des encours de décaissement pour la banque ID={}: {}", banqueId, totalDesEncours);
        return new EncoursVirementResponseDto(totalDesEncours, virementPage.map(this::toDto));
    }

    @Transactional(readOnly = true)
    public EncoursVirementResponseDto getEncoursEncaissementForVirements(EncoursVirementsSearchDto searchDto, Long banqueId, Pageable pageable) {
        log.debug("Récupération des encours d'encaissement pour la banque ID={} avec les filtres={}", banqueId, searchDto);

        Banque banqueBeneficiaire = banqueService.findOrThrow(banqueId);

        Specification<Virement> spec = VirementSpec.encoursEncaissement(banqueBeneficiaire.getNom(), finDuMoisCourant())
                .and(VirementSpec.hasDateEmission(searchDto.getDateEmission()))
                .and(VirementSpec.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(VirementSpec.hasMontant(searchDto.getMontant()))
                .and(VirementSpec.hasEmetteur(searchDto.getEmetteur()));

        Page<Virement> virementPage = virementRepository.findAll(spec, stablePageable(pageable));

        BigDecimal totalDesEncours = virementRepository.findAll(spec).stream()
                .map(Virement::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);

        log.debug("Total des encours d'encaissement pour la banque ID={}: {}", banqueId, totalDesEncours);
        return new EncoursVirementResponseDto(totalDesEncours, virementPage.map(this::toDto));
    }

    /**
     * Supprime un virement saisi manuellement.
     *
     * <p>Un virement issu de la synchronisation FunPaiement ({@code idPaiement} renseigné) ne peut
     * pas être supprimé : le lecteur de synchro relit tout l'historique à chaque exécution
     * ({@code PaymentItemReader.open()} → {@code fetchAll()}) et la seule clé d'idempotence est
     * {@code findByIdPaiement}. Supprimer la ligne efface cette clé, et la synchro suivante la
     * recrée à l'identique — boucle observée en production du 13 au 17/07/2026 sur les
     * encaissements de virements internes du 18/03/2026. Pour retirer une telle ligne des encours,
     * l'annuler plutôt que la supprimer : une ligne existante n'est jamais recréée ni écrasée par
     * la synchro.
     */
    @Transactional
    @AuditableAction(operation = AuditOperation.DELETE, entity = Virement.class)
    public void deleteVirement(Long id) {
        log.info("Suppression du virement ID={}", id);

        Virement virement = trouverOuLeverException(id);

        if (virement.getStatut() == StatutMoyenPaiement.REGLE) {
            log.warn("Tentative de suppression d'un virement déjà réglé, ID={}", id);
            throw new VirementBusinessException("Suppression interdite: le virement with id " + id + " est déjà réglé.");
        }

        if (virement.getIdPaiement() != null) {
            log.warn("Tentative de suppression d'un virement synchronisé, ID={}, idPaiement={}",
                    id, virement.getIdPaiement());
            throw new VirementBusinessException(
                    "Suppression interdite: le virement " + id + " provient de la synchronisation FunPaiement "
                            + "et serait recréé à la prochaine synchronisation. Annulez-le plutôt que de le supprimer.");
        }

        virementRepository.delete(virement);
        log.info("Virement ID={} supprimé avec succès", id);
    }

    /**
     * Nombre de virements échus à la date donnée, groupés par banque — façade publique sur
     * {@link VirementRepository#countEchusGroupedByBanque}. Permet à d'autres domaines (ex.
     * l'agrégateur accueil de Bank) de lire cette information sans jamais injecter
     * {@code VirementRepository} directement (Backend/CLAUDE.md, "Encapsulation des domaines").
     */
    @Transactional(readOnly = true)
    public List<Object[]> countEchusGroupedByBanque(LocalDate today) {
        return virementRepository.countEchusGroupedByBanque(today);
    }

    /**
     * Nombre de virements échus pour une banque donnée — façade publique sur
     * {@link VirementRepository#countEchusByBanqueId}. Utilisé par l'agrégateur accueil (requête
     * ciblée sur une seule banque, plus efficace que {@link #countEchusGroupedByBanque}).
     */
    @Transactional(readOnly = true)
    public long countEchusByBanqueId(Long banqueId, LocalDate today) {
        return virementRepository.countEchusByBanqueId(banqueId, today);
    }

    // ── Export PDF ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public byte[] exportEncaissementsPdf(VirementSearchDto searchDto) {
        Specification<Virement> spec = VirementSpec.bySearchDto(searchDto).and(VirementSpec.hasType(Type.ENCAISSEMENT));

        List<Virement> virements = virementRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "dateEmission"));
        BigDecimal total = sumMontant(spec);
        List<String> filterLines = buildFilterSummary(searchDto);

        return generateVirementPdf("État des Virements d'Encaissement", virements, total, filterLines);
    }

    @Transactional(readOnly = true)
    public byte[] exportDecaissementsPdf(VirementSearchDto searchDto) {
        Specification<Virement> spec = VirementSpec.bySearchDto(searchDto).and(VirementSpec.hasType(Type.DECAISSEMENT));

        List<Virement> virements = virementRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "dateEmission"));
        BigDecimal total = sumMontant(spec);
        List<String> filterLines = buildFilterSummary(searchDto);

        return generateVirementPdf("État des Virements de Décaissement", virements, total, filterLines);
    }

    @Transactional(readOnly = true)
    public byte[] exportEncoursEncaissementPdf(EncoursVirementsSearchDto searchDto, Long banqueId) {
        Banque banqueBeneficiaire = banqueService.findOrThrow(banqueId);

        Specification<Virement> spec = VirementSpec.encoursEncaissement(banqueBeneficiaire.getNom(), finDuMoisCourant())
                .and(VirementSpec.hasDateEmission(searchDto.getDateEmission()))
                .and(VirementSpec.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(VirementSpec.hasMontant(searchDto.getMontant()))
                .and(VirementSpec.hasEmetteur(searchDto.getEmetteur()));

        List<Virement> virements = virementRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "dateEmission"));
        BigDecimal total = sumMontant(spec);
        List<String> filterLines = buildEncoursFilterSummary(searchDto, banqueBeneficiaire.getNom());

        return generateVirementPdf("Encours d'Encaissement — Virements", virements, total, filterLines);
    }

    @Transactional(readOnly = true)
    public byte[] exportEncoursDecaissementPdf(EncoursVirementsSearchDto searchDto, Long banqueId) {
        Specification<Virement> spec = VirementSpec.encoursDecaissement(banqueId, finDuMoisCourant())
                .and(VirementSpec.hasDateEmission(searchDto.getDateEmission()))
                .and(VirementSpec.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(VirementSpec.hasMontant(searchDto.getMontant()));

        List<Virement> virements = virementRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "dateEmission"));
        BigDecimal total = sumMontant(spec);
        List<String> filterLines = buildEncoursFilterSummary(searchDto, banqueService.findOrThrow(banqueId).getNom());

        return generateVirementPdf("Encours de Décaissement — Virements", virements, total, filterLines);
    }

    private BigDecimal sumMontant(Specification<Virement> spec) {
        return virementRepository.findAll(spec).stream()
                .map(Virement::getMontant)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String banqueBeneficiaireNom(Virement v) {
        return v.getBanqueBeneficiare() != null ? v.getBanqueBeneficiare().getNom() : v.getNomBanqueBeneficiaireLibre();
    }

    private byte[] generateVirementPdf(String title, List<Virement> virements, BigDecimal totalMontant, List<String> filterLines) {
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
                                    + "  •  " + virements.size() + " virement(s)";
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

                            float[] colWidths = {10f, 12f, 12f, 13f, 13f, 11f, 11f, 9f};
                            PdfPTable table = new PdfPTable(colWidths);
                            table.setWidthPercentage(100f);
                            table.setSpacingBefore(8f);
                            table.setHeaderRows(1);

                            for (String h : new String[]{
                                    "N° Virement", "Banque émettrice", "Banque bénéficiaire", "Émetteur", "Bénéficiaire",
                                    "Montant (MAD)", "Date émission", "Statut"}) {
                                PdfPCell c = new PdfPCell(new Phrase(h, hdrFont));
                                c.setBackgroundColor(medBlue);
                                c.setPadding(5f);
                                c.setHorizontalAlignment(Element.ALIGN_CENTER);
                                c.setBorderColor(Color.WHITE);
                                c.setBorderWidth(0.5f);
                                table.addCell(c);
                            }

                            for (int i = 0; i < virements.size(); i++) {
                                Virement v = virements.get(i);
                                Color rowBg = i % 2 == 0 ? Color.WHITE : rowAlt;

                                addPdfCell(table, v.getNumero(), cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                                addPdfCell(table, v.getBanqueEmettrice() != null ? v.getBanqueEmettrice().getNom() : "—", cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                                addPdfCell(table, banqueBeneficiaireNom(v), cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                                addPdfCell(table, v.getEmetteur(), cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                                addPdfCell(table, v.getBeneficiaire(), cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                                addPdfCell(table, formatMontantPdf(v.getMontant()), cellFont, rowBg, borderGray, Element.ALIGN_RIGHT);
                                addPdfCell(table, v.getDateEmission() != null ? v.getDateEmission().format(DATE_FMT) : "—", cellFont, rowBg, borderGray, Element.ALIGN_CENTER);

                                boolean regle = v.getStatut() == StatutMoyenPaiement.REGLE;
                                boolean annule = v.getStatut() == StatutMoyenPaiement.ANNULE;
                                PdfPCell statusCell = new PdfPCell(new Phrase(
                                        regle ? "RÉGLÉ" : (annule ? "ANNULÉ" : "ENCOURS"),
                                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE)));
                                statusCell.setBackgroundColor(regle ? green : (annule ? Color.GRAY : orange));
                                statusCell.setPadding(4f);
                                statusCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                                statusCell.setBorderColor(borderGray);
                                statusCell.setBorderWidth(0.5f);
                                table.addCell(statusCell);
                            }

                            PdfPCell totalLabel = new PdfPCell(new Phrase("TOTAL — " + virements.size() + " virement(s)", totalFont));
                            totalLabel.setColspan(5);
                            totalLabel.setBackgroundColor(lightBlue);
                            totalLabel.setPadding(5f);
                            totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
                            totalLabel.setBorderColor(medBlue);
                            table.addCell(totalLabel);

                            PdfPCell totalVal = new PdfPCell(new Phrase(formatMontantPdf(totalMontant) + " MAD", totalFont));
                            totalVal.setBackgroundColor(lightBlue);
                            totalVal.setPadding(5f);
                            totalVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
                            totalVal.setBorderColor(medBlue);
                            table.addCell(totalVal);

                            for (int j = 0; j < 2; j++) {
                                PdfPCell empty = new PdfPCell(new Phrase(""));
                                empty.setBackgroundColor(lightBlue);
                                empty.setBorderColor(medBlue);
                                table.addCell(empty);
                            }

                            doc.add(table);

                        } catch (DocumentException e) {
                            throw new RuntimeException("Erreur génération contenu PDF virements", e);
                        }
                    }
            );
        } catch (DocumentException e) {
            throw new RuntimeException("Erreur lors de la génération du PDF des virements", e);
        }
    }

    private void addPdfCell(PdfPTable table, String text, Font font, Color bg, Color border, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "—", font));
        cell.setBackgroundColor(bg);
        cell.setPadding(4f);
        cell.setHorizontalAlignment(align);
        cell.setBorderColor(border);
        cell.setBorderWidth(0.5f);
        table.addCell(cell);
    }

    private String formatMontantPdf(BigDecimal montant) {
        if (montant == null) return "—";
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.FRANCE);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(montant);
    }

    private List<String> buildFilterSummary(VirementSearchDto dto) {
        List<String> lines = new ArrayList<>();
        if (dto == null) return lines;
        if (dto.numero() != null && !dto.numero().isBlank())
            lines.add("N° Virement : " + dto.numero());
        if (dto.banqueBeneficiaire() != null && !dto.banqueBeneficiaire().isBlank())
            lines.add("Banque bénéficiaire : " + dto.banqueBeneficiaire());
        if (dto.nomBanqueEmettrice() != null && !dto.nomBanqueEmettrice().isBlank())
            lines.add("Banque émettrice : " + dto.nomBanqueEmettrice());
        if (dto.beneficiaire() != null && !dto.beneficiaire().isBlank())
            lines.add("Bénéficiaire : " + dto.beneficiaire());
        if (dto.emetteur() != null && !dto.emetteur().isBlank())
            lines.add("Émetteur : " + dto.emetteur());
        if (dto.ribBeneficiaire() != null && !dto.ribBeneficiaire().isBlank())
            lines.add("RIB bénéficiaire : " + dto.ribBeneficiaire());
        if (dto.statut() != null)
            lines.add("Statut : " + dto.statut());
        if (dto.montant() != null)
            lines.add("Montant : " + formatMontantPdf(dto.montant()) + " MAD");
        if (dto.dateEmissionFrom() != null || dto.dateEmissionTo() != null) {
            String from = dto.dateEmissionFrom() != null ? dto.dateEmissionFrom().format(DATE_FMT) : "…";
            String to   = dto.dateEmissionTo()   != null ? dto.dateEmissionTo().format(DATE_FMT)   : "…";
            lines.add("Date émission : " + from + " → " + to);
        }
        if (dto.dateOperationFrom() != null || dto.dateOperationTo() != null) {
            String from = dto.dateOperationFrom() != null ? dto.dateOperationFrom().format(DATE_FMT) : "…";
            String to   = dto.dateOperationTo()   != null ? dto.dateOperationTo().format(DATE_FMT)   : "…";
            lines.add("Date opération : " + from + " → " + to);
        }
        if (dto.dateValeurFrom() != null || dto.dateValeurTo() != null) {
            String from = dto.dateValeurFrom() != null ? dto.dateValeurFrom().format(DATE_FMT) : "…";
            String to   = dto.dateValeurTo()   != null ? dto.dateValeurTo().format(DATE_FMT)   : "…";
            lines.add("Date valeur : " + from + " → " + to);
        }
        return lines;
    }

    private List<String> buildEncoursFilterSummary(EncoursVirementsSearchDto dto, String banqueNom) {
        List<String> lines = new ArrayList<>();
        if (banqueNom != null) {
            lines.add("Banque : " + banqueNom);
        }
        if (dto == null) return lines;
        if (dto.getBeneficiaire() != null && !dto.getBeneficiaire().isBlank())
            lines.add("Bénéficiaire : " + dto.getBeneficiaire());
        if (dto.getEmetteur() != null && !dto.getEmetteur().isBlank())
            lines.add("Émetteur : " + dto.getEmetteur());
        if (dto.getMontant() != null)
            lines.add("Montant : " + formatMontantPdf(dto.getMontant()) + " MAD");
        if (dto.getDateEmission() != null)
            lines.add("Date émission : " + dto.getDateEmission().format(DATE_FMT));
        return lines;
    }

    // ── Helpers internes ──────────────────────────────────────────────────────

    private Virement trouverOuLeverException(Long id) {
        return virementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Virement with id " + id + " not found."));
    }

    /**
     * Fin du mois courant — recalculée à chaque appel (et non mise en cache) pour que
     * les calculs d'encours restent corrects après le premier jour suivant le déploiement.
     */
    private LocalDate finDuMoisCourant() {
        return LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());
    }

    private Pageable stablePageable(Pageable pageable) {
        return pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id"));
    }

    /**
     * Mappe l'entité vers le DTO puis applique les champs d'affichage calculés
     * (règle métier propre au service — jamais dans le Mapper, cf. architecture.md) :
     * libellé, observation, et le nom de la banque bénéficiaire (référencée ou libre).
     */
    private VirementResponseDto toDto(Virement virement) {
        VirementResponseDto dto = virementMapper.toDto(virement);
        dto.setObservation(" VIREMENT N° " + virement.getNumero());
        dto.setLibelle(virement.getType() == Type.DECAISSEMENT ? virement.getBeneficiaire() : virement.getEmetteur());
        dto.setBanqueBeneficiaire(virement.getBanqueBeneficiare() != null
                ? virement.getBanqueBeneficiare().getNom()
                : virement.getNomBanqueBeneficiaireLibre());
        return dto;
    }
}
