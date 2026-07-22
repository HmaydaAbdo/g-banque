package staport.finance.gBanque.moyensDePaiements.lcn.services;

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
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.entities.LigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.services.LigneFonctionnelleReader;
import staport.finance.gBanque.audit.AuditOperation;
import staport.finance.gBanque.audit.AuditableAction;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.moyensDePaiements.lcn.dtos.*;
import staport.finance.gBanque.moyensDePaiements.lcn.entities.Lcn;
import staport.finance.gBanque.moyensDePaiements.lcn.exceptions.LcnBusinessException;
import staport.finance.gBanque.moyensDePaiements.lcn.mapper.LcnMapper;
import staport.finance.gBanque.moyensDePaiements.lcn.metrics.LcnMetrics;
import staport.finance.gBanque.moyensDePaiements.lcn.repositories.LcnRepository;
import staport.finance.gBanque.moyensDePaiements.lcn.specs.LcnSpecification;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
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

import static staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.services.NatureLigneFonctionnelleService.ESC_COM_CODE;

/**
 * Service métier du module LCN.
 *
 * <p>Façade publique du domaine {@code lcn} (architecture.md — Encapsulation des domaines) :
 * les autres modules doivent passer par ce service ou par {@link LcnReader}, jamais par
 * {@code LcnRepository} directement.
 */
@Service
public class LcnService {

    private static final Logger log = LoggerFactory.getLogger(LcnService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final LcnRepository lcnRepository;
    private final IBanqueService banqueService;
    private final LigneFonctionnelleReader ligneFonctionnelleReader;
    private final LcnMapper lcnMapper;
    private final LcnMetrics lcnMetrics;
    private final StaportPdfReportService staportPdfReportService;

    public LcnService(LcnRepository lcnRepository, IBanqueService banqueService,
                       LigneFonctionnelleReader ligneFonctionnelleReader, LcnMapper lcnMapper, LcnMetrics lcnMetrics,
                       StaportPdfReportService staportPdfReportService) {
        this.lcnRepository = lcnRepository;
        this.banqueService = banqueService;
        this.ligneFonctionnelleReader = ligneFonctionnelleReader;
        this.lcnMapper = lcnMapper;
        this.lcnMetrics = lcnMetrics;
        this.staportPdfReportService = staportPdfReportService;
    }


    @Transactional
    @AuditableAction(operation = AuditOperation.CREATE, entity = Lcn.class)
    public LcnResponseDTO createLcn(LcnRequestDto requestDto) {
        log.info("Création d'un nouveau LCN d'encaissement: {}", requestDto);

        validateEscompteBusinessRule(requestDto.getType(), requestDto.getNatureLcn());

        Banque banque = banqueService.findOrThrow(requestDto.getBanqueId());

        LigneFonctionnelle ligneFonctionnelle = null;

        //  Association automatique pour les LCN de type ESCOMPTE
        if (requestDto.getNatureLcn() == NatureLcn.ESCOMPTE) {
            log.debug("LCN de type ESCOMPTE détecté - recherche de la ligne fonctionnelle escompte commerciale pour la banque ID: {}",
                    banque.getId());

            ligneFonctionnelle = ligneFonctionnelleReader
                    .findByBanqueIdAndNatureLigneFonctionnelleCodeAndActifTrue(banque.getId(), ESC_COM_CODE)
                    .orElseThrow(() -> {
                        log.error("Ligne fonctionnelle escompte commerciale active introuvable pour la banque ID: {}",
                                banque.getId());
                        return new ResourceNotFoundException(
                                "Aucune ligne fonctionnelle escompte commerciale active n'a été trouvée pour la banque '" +
                                        banque.getNom() + "' (ID: " + banque.getId() + "). " +
                                        "Veuillez activer une ligne fonctionnelle escompte commerciale avant de créer un LCN de type ESCOMPTE.");
                    });

            log.info("Ligne fonctionnelle escompte commerciale trouvée: ID={}, Plafond={}",
                    ligneFonctionnelle.getId(),
                    ligneFonctionnelle.getPlafond());
        }

        // Création de l'entité LCN
        Lcn lcn = lcnMapper.toEntity(requestDto, banque);

        //  Association de la ligne fonctionnelle si type ESCOMPTE
        if (ligneFonctionnelle != null) {
            lcn.setLigneFonctionnelle(ligneFonctionnelle);
            log.debug("LCN associé à la ligne fonctionnelle ID={}", ligneFonctionnelle.getId());
        }

        lcn = lcnRepository.save(lcn);
        lcnMetrics.incrementerCreation(lcn.getType());

        log.info("LCN créé avec succès - ID: {}, Type: {}, Nature: {}, Ligne fonctionnelle: {}",
                lcn.getId(),
                lcn.getType(),
                lcn.getNatureLcn(),
                ligneFonctionnelle != null ? ligneFonctionnelle.getId() : "N/A");

        return toDto(lcn);
    }

    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = Lcn.class)
    public LcnResponseDTO editEncaissementLcn(Long id, LcnRequestDto dto) {
        log.info("Modification du LCN d'encaissement ID={} avec les données={}", id, dto);

        Lcn existingLcn = trouverOuLeverException(id);

        if (existingLcn.getType() != Type.ENCAISSEMENT) {
            log.warn("Tentative de modification d'un LCN qui n'est pas de type ENCAISSEMENT, ID={}", id);
            throw new LcnBusinessException(
                    "Modification interdite: le lcn with id " + id + " n'est pas de type ENCAISSEMENT.");
        }

        if (existingLcn.getStatut() == StatutMoyenPaiement.REGLE) {
            log.warn("Tentative de modification d'un LCN déjà réglé, ID={}", id);
            throw new LcnBusinessException(
                    "Modification interdite: le lcn with id " + id + " est déjà réglé.");
        }

        if (dto.getDateEmission() != null) {
            existingLcn.setDateEmission(dto.getDateEmission());
        }

        if (dto.getNumero() != null) {
            existingLcn.setNumero(dto.getNumero());
        }

        if (dto.getNatureLcn() != null) {
            //  Si on change la nature vers ESCOMPTE, il faut associer la ligne fonctionnelle
            if (dto.getNatureLcn() == NatureLcn.ESCOMPTE &&
                    existingLcn.getNatureLcn() != NatureLcn.ESCOMPTE) {

                log.debug("Changement de nature vers ESCOMPTE - recherche de la ligne fonctionnelle");
                LigneFonctionnelle ligneFonctionnelle = ligneFonctionnelleReader
                        .findByBanqueIdAndNatureLigneFonctionnelleCodeAndActifTrue(
                                existingLcn.getBanque().getId(), ESC_COM_CODE)
                        .orElseThrow(() -> {
                            log.error("Ligne fonctionnelle escompte commerciale active introuvable pour la banque ID: {}",
                                    dto.getBanqueId());
                            return new ResourceNotFoundException(
                                    "Aucune ligne fonctionnelle escompte commerciale active n'a été trouvée pour cette banque.");
                        });

                existingLcn.setLigneFonctionnelle(ligneFonctionnelle);
                log.debug("Ligne fonctionnelle ID={} associée au LCN", ligneFonctionnelle.getId());
            }

            // Si on change la nature depuis ESCOMPTE vers autre chose, on dissocie la ligne
            if (dto.getNatureLcn() != NatureLcn.ESCOMPTE &&
                    existingLcn.getNatureLcn() == NatureLcn.ESCOMPTE) {
                log.debug("Changement de nature depuis ESCOMPTE - dissociation de la ligne fonctionnelle");
                existingLcn.setLigneFonctionnelle(null);
            }

            existingLcn.setNatureLcn(dto.getNatureLcn());
        }

        if (dto.getDateEcheance() != null) {
            existingLcn.setDateEcheance(dto.getDateEcheance());
        }

        if (dto.getBanqueId() != null) {
            existingLcn.setBanque(banqueService.findOrThrow(dto.getBanqueId()));
        }

        if (dto.getMontant() != null) {
            existingLcn.setMontant(dto.getMontant());
        }

        if (dto.getEmetteur() != null) {
            existingLcn.setEmetteur(dto.getEmetteur());
        }

        existingLcn = lcnRepository.save(existingLcn);
        log.info("LCN ID={} mis à jour avec succès", id);
        return toDto(existingLcn);
    }

    @Transactional(readOnly = true)
    public LcnResponseDTO getLcnById(Long id) {
        log.debug("Récupération du LCN avec l'ID={}", id);
        return toDto(trouverOuLeverException(id));
    }

    @Transactional
    @AuditableAction(operation = AuditOperation.DELETE, entity = Lcn.class)
    public void deleteLcn(Long id) {
        log.info("Suppression du LCN avec l'ID={}", id);

        Lcn lcn = trouverOuLeverException(id);

        if (lcn.getStatut() == StatutMoyenPaiement.REGLE) {
            log.warn("Tentative de suppression d'un LCN déjà réglé, ID={}", id);
            throw new LcnBusinessException("Suppression interdite: le lcn with id " + id + " est déjà réglé.");
        }

        lcnRepository.deleteById(id);
        log.info("LCN ID={} supprimé avec succès", id);
    }

    @Transactional(readOnly = true)
    public Page<LcnResponseDTO> getDecaissements(LcnSearchDTO searchDto, Pageable pageable) {
        log.debug("Recherche des LCN de décaissement avec les filtres={} et pagination={}", searchDto, pageable);

        Specification<Lcn> spec = Specification.where(LcnSpecification.hasNumero(searchDto.getNumero()))
                .and(LcnSpecification.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(LcnSpecification.hasBanqueNom(searchDto.getNomBanque()))
                .and(LcnSpecification.hasStatut(searchDto.getStatut()))
                .and(LcnSpecification.hasType(Type.DECAISSEMENT));

        Page<Lcn> lcnPage = lcnRepository.findAll(spec, stablePageable(pageable));
        log.debug("{} LCN de décaissement trouvé(s)", lcnPage.getTotalElements());

        return lcnPage.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<LcnResponseDTO> getEncaissements(LcnSearchDTO searchDto, Pageable pageable) {
        log.debug("Recherche des LCN d'encaissement avec les filtres={} et pagination={}", searchDto, pageable);

        Specification<Lcn> spec = Specification.where(LcnSpecification.hasType(Type.ENCAISSEMENT))
                .and(LcnSpecification.hasDateEmission(searchDto.getDateEmission()))
                .and(LcnSpecification.hasDateEcheance(searchDto.getDateEcheance()))
                .and(LcnSpecification.hasEmetteur(searchDto.getEmetteur()))
                .and(LcnSpecification.hasMontant(searchDto.getMontant()))
                .and(LcnSpecification.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(LcnSpecification.hasBanqueNom(searchDto.getNomBanque()))
                .and(LcnSpecification.hasStatut(searchDto.getStatut()))
                .and(LcnSpecification.hasNumero(searchDto.getNumero()));

        Page<Lcn> lcnPage = lcnRepository.findAll(spec, stablePageable(pageable));
        log.debug("{} LCN d'encaissement trouvé(s)", lcnPage.getTotalElements());

        return lcnPage.map(this::toDto);
    }

    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = Lcn.class)
    public LcnResponseDTO toggleLcnStatus(Long id) {
        log.info("Basculement du statut pour le LCN ID={}", id);

        Lcn lcn = trouverOuLeverException(id);

        if (lcn.getStatut() == StatutMoyenPaiement.ANNULE) {
            log.warn("Tentative de basculement du statut d'un LCN annulé, ID={}", id);
            throw new LcnBusinessException("Impossible de basculer le statut d'un LCN annulé (ID=" + id + ").");
        }

        lcn.setStatut(lcn.getStatut() == StatutMoyenPaiement.ENCOURS ? StatutMoyenPaiement.REGLE : StatutMoyenPaiement.ENCOURS);
        lcn = lcnRepository.save(lcn);
        if (lcn.getStatut() == StatutMoyenPaiement.REGLE) {
            lcnMetrics.incrementerPassageRegle();
        }

        log.info("Statut basculé avec succès pour le LCN ID={} — nouveau statut={}", id, lcn.getStatut());
        return toDto(lcn);
    }

    @Transactional(readOnly = true)
    public EncoursLcnResponse getEncoursDecaissementsForLcn(EncoursLcnSearchDto searchDto, Long banqueId, Pageable pageable) {
        log.debug("Récupération des encours de décaissement pour les LCN de la banque ID={} avec les filtres={}", banqueId, searchDto);

        Specification<Lcn> spec = LcnSpecification.encoursDecaissement(banqueId, finDuMoisCourant())
                .and(LcnSpecification.hasNumero(searchDto.getNumero()))
                .and(LcnSpecification.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(LcnSpecification.hasMontant(searchDto.getMontant()));

        Page<Lcn> lcnPage = lcnRepository.findAll(spec, stablePageable(pageable));
        BigDecimal totalDesEncours = lcnRepository.findAll(spec).stream().map(Lcn::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
        Page<LcnResponseDTO> result = lcnPage.map(this::toDto);

        log.debug("Total des encours de décaissement pour la banque ID={}: {}", banqueId, totalDesEncours);
        return new EncoursLcnResponse(totalDesEncours, result);
    }

    @Transactional(readOnly = true)
    public EncoursLcnResponse getEncoursEncaissementsForLcn(EncoursLcnSearchDto searchDto, Long banqueId, Pageable pageable) {
        log.debug("Récupération des encours d'encaissement pour les LCN de la banque ID={} avec les filtres={}", banqueId, searchDto);

        Specification<Lcn> spec = LcnSpecification.encoursEncaissement(banqueId, finDuMoisCourant())
                .and(LcnSpecification.hasEmetteur(searchDto.getEmetteur()))
                .and(LcnSpecification.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(LcnSpecification.hasMontant(searchDto.getMontant()))
                .and(LcnSpecification.hasDateEcheance(searchDto.getDateEcheance()))
                .and(LcnSpecification.hasDateEmission(searchDto.getDateEmission()))
                .and(LcnSpecification.hasNumero(searchDto.getNumero()));

        Page<Lcn> lcnPage = lcnRepository.findAll(spec, stablePageable(pageable));
        BigDecimal totalDesEncours = lcnRepository.findAll(spec).stream().map(Lcn::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
        Page<LcnResponseDTO> result = lcnPage.map(this::toDto);

        log.debug("Total des encours d'encaissement pour la banque ID={}: {}", banqueId, totalDesEncours);
        return new EncoursLcnResponse(totalDesEncours, result);
    }

    /**
     * Nombre de LCN échus à la date donnée, groupés par banque — façade publique sur
     * {@link LcnRepository#countEchusGroupedByBanque}. Permet à d'autres domaines (ex.
     * l'agrégateur accueil de Bank) de lire cette information sans jamais injecter
     * {@code LcnRepository} directement (Backend/CLAUDE.md, "Encapsulation des domaines").
     */
    @Transactional(readOnly = true)
    public List<Object[]> countEchusGroupedByBanque(LocalDate today) {
        return lcnRepository.countEchusGroupedByBanque(today);
    }

    /**
     * Nombre de LCN échus pour une banque donnée — façade publique sur
     * {@link LcnRepository#countEchusByBanqueId}. Utilisé par l'agrégateur accueil (requête
     * ciblée sur une seule banque, plus efficace que {@link #countEchusGroupedByBanque}).
     */
    @Transactional(readOnly = true)
    public long countEchusByBanqueId(Long banqueId, LocalDate today) {
        return lcnRepository.countEchusByBanqueId(banqueId, today);
    }

    // ── Export PDF ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public byte[] exportEncaissementsPdf(LcnSearchDTO searchDto) {
        Specification<Lcn> spec = Specification.where(LcnSpecification.hasType(Type.ENCAISSEMENT))
                .and(LcnSpecification.hasDateEmission(searchDto.getDateEmission()))
                .and(LcnSpecification.hasDateEcheance(searchDto.getDateEcheance()))
                .and(LcnSpecification.hasEmetteur(searchDto.getEmetteur()))
                .and(LcnSpecification.hasMontant(searchDto.getMontant()))
                .and(LcnSpecification.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(LcnSpecification.hasBanqueNom(searchDto.getNomBanque()))
                .and(LcnSpecification.hasStatut(searchDto.getStatut()))
                .and(LcnSpecification.hasNumero(searchDto.getNumero()));

        List<Lcn> lcns = lcnRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "dateEmission"));
        BigDecimal total = sumMontant(spec);
        List<String> filterLines = buildFilterSummary(searchDto);

        return generateLcnPdf("État des LCN d'Encaissement", lcns, total, filterLines);
    }

    @Transactional(readOnly = true)
    public byte[] exportDecaissementsPdf(LcnSearchDTO searchDto) {
        Specification<Lcn> spec = Specification.where(LcnSpecification.hasNumero(searchDto.getNumero()))
                .and(LcnSpecification.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(LcnSpecification.hasBanqueNom(searchDto.getNomBanque()))
                .and(LcnSpecification.hasStatut(searchDto.getStatut()))
                .and(LcnSpecification.hasType(Type.DECAISSEMENT));

        List<Lcn> lcns = lcnRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "dateEmission"));
        BigDecimal total = sumMontant(spec);
        List<String> filterLines = buildFilterSummary(searchDto);

        return generateLcnPdf("État des LCN de Décaissement", lcns, total, filterLines);
    }

    @Transactional(readOnly = true)
    public byte[] exportEncoursEncaissementPdf(EncoursLcnSearchDto searchDto, Long banqueId) {
        Specification<Lcn> spec = LcnSpecification.encoursEncaissement(banqueId, finDuMoisCourant())
                .and(LcnSpecification.hasEmetteur(searchDto.getEmetteur()))
                .and(LcnSpecification.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(LcnSpecification.hasMontant(searchDto.getMontant()))
                .and(LcnSpecification.hasDateEcheance(searchDto.getDateEcheance()))
                .and(LcnSpecification.hasDateEmission(searchDto.getDateEmission()))
                .and(LcnSpecification.hasNumero(searchDto.getNumero()));

        List<Lcn> lcns = lcnRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "dateEcheance"));
        BigDecimal total = sumMontant(spec);
        List<String> filterLines = buildEncoursFilterSummary(searchDto, banqueId);

        return generateLcnPdf("Encours d'Encaissement — LCN", lcns, total, filterLines);
    }

    @Transactional(readOnly = true)
    public byte[] exportEncoursDecaissementPdf(EncoursLcnSearchDto searchDto, Long banqueId) {
        Specification<Lcn> spec = LcnSpecification.encoursDecaissement(banqueId, finDuMoisCourant())
                .and(LcnSpecification.hasNumero(searchDto.getNumero()))
                .and(LcnSpecification.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(LcnSpecification.hasMontant(searchDto.getMontant()));

        List<Lcn> lcns = lcnRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "dateEcheance"));
        BigDecimal total = sumMontant(spec);
        List<String> filterLines = buildEncoursFilterSummary(searchDto, banqueId);

        return generateLcnPdf("Encours de Décaissement — LCN", lcns, total, filterLines);
    }

    private BigDecimal sumMontant(Specification<Lcn> spec) {
        return lcnRepository.findAll(spec).stream()
                .map(Lcn::getMontant)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private byte[] generateLcnPdf(String title, List<Lcn> lcns, BigDecimal totalMontant, List<String> filterLines) {
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
                                    + "  •  " + lcns.size() + " LCN";
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

                            float[] colWidths = {10f, 13f, 12f, 12f, 11f, 11f, 11f, 10f, 10f};
                            PdfPTable table = new PdfPTable(colWidths);
                            table.setWidthPercentage(100f);
                            table.setSpacingBefore(8f);
                            table.setHeaderRows(1);

                            for (String h : new String[]{
                                    "N° LCN", "Banque", "Émetteur", "Bénéficiaire",
                                    "Montant (MAD)", "Date émission", "Date échéance", "Nature", "Statut"}) {
                                PdfPCell c = new PdfPCell(new Phrase(h, hdrFont));
                                c.setBackgroundColor(medBlue);
                                c.setPadding(5f);
                                c.setHorizontalAlignment(Element.ALIGN_CENTER);
                                c.setBorderColor(Color.WHITE);
                                c.setBorderWidth(0.5f);
                                table.addCell(c);
                            }

                            for (int i = 0; i < lcns.size(); i++) {
                                Lcn l = lcns.get(i);
                                Color rowBg = i % 2 == 0 ? Color.WHITE : rowAlt;

                                addPdfCell(table, l.getNumero(), cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                                addPdfCell(table, l.getBanque() != null ? l.getBanque().getNom() : "—", cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                                addPdfCell(table, l.getEmetteur(), cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                                addPdfCell(table, l.getBeneficiaire(), cellFont, rowBg, borderGray, Element.ALIGN_LEFT);
                                addPdfCell(table, formatMontantPdf(l.getMontant()), cellFont, rowBg, borderGray, Element.ALIGN_RIGHT);
                                addPdfCell(table, l.getDateEmission() != null ? l.getDateEmission().format(DATE_FMT) : "—", cellFont, rowBg, borderGray, Element.ALIGN_CENTER);
                                addPdfCell(table, l.getDateEcheance() != null ? l.getDateEcheance().format(DATE_FMT) : "—", cellFont, rowBg, borderGray, Element.ALIGN_CENTER);
                                addPdfCell(table, l.getNatureLcn() != null ? l.getNatureLcn().toString() : "—", cellFont, rowBg, borderGray, Element.ALIGN_CENTER);

                                boolean regle = l.getStatut() == StatutMoyenPaiement.REGLE;
                                boolean annule = l.getStatut() == StatutMoyenPaiement.ANNULE;
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

                            PdfPCell totalLabel = new PdfPCell(new Phrase("TOTAL — " + lcns.size() + " LCN", totalFont));
                            totalLabel.setColspan(4);
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

                            for (int j = 0; j < 4; j++) {
                                PdfPCell empty = new PdfPCell(new Phrase(""));
                                empty.setBackgroundColor(lightBlue);
                                empty.setBorderColor(medBlue);
                                table.addCell(empty);
                            }

                            doc.add(table);

                        } catch (DocumentException e) {
                            throw new RuntimeException("Erreur génération contenu PDF LCN", e);
                        }
                    }
            );
        } catch (DocumentException e) {
            throw new RuntimeException("Erreur lors de la génération du PDF des LCN", e);
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

    private List<String> buildFilterSummary(LcnSearchDTO dto) {
        List<String> lines = new ArrayList<>();
        if (dto == null) return lines;
        if (dto.getNumero() != null && !dto.getNumero().isBlank())
            lines.add("N° LCN : " + dto.getNumero());
        if (dto.getNomBanque() != null && !dto.getNomBanque().isBlank())
            lines.add("Banque : " + dto.getNomBanque());
        if (dto.getBeneficiaire() != null && !dto.getBeneficiaire().isBlank())
            lines.add("Bénéficiaire : " + dto.getBeneficiaire());
        if (dto.getEmetteur() != null && !dto.getEmetteur().isBlank())
            lines.add("Émetteur : " + dto.getEmetteur());
        if (dto.getStatut() != null)
            lines.add("Statut : " + dto.getStatut());
        if (dto.getMontant() != null)
            lines.add("Montant : " + formatMontantPdf(dto.getMontant()) + " MAD");
        if (dto.getDateEmission() != null)
            lines.add("Date émission : " + dto.getDateEmission().format(DATE_FMT));
        if (dto.getDateEcheance() != null)
            lines.add("Date échéance : " + dto.getDateEcheance().format(DATE_FMT));
        return lines;
    }

    private List<String> buildEncoursFilterSummary(EncoursLcnSearchDto dto, Long banqueId) {
        List<String> lines = new ArrayList<>();
        try {
            lines.add("Banque : " + banqueService.findOrThrow(banqueId).getNom());
        } catch (ResourceNotFoundException ignored) {
            // Filtre obsolète (banque supprimée depuis) — ignoré silencieusement dans le résumé PDF.
        }
        if (dto == null) return lines;
        if (dto.getNumero() != null && !dto.getNumero().isBlank())
            lines.add("N° LCN : " + dto.getNumero());
        if (dto.getBeneficiaire() != null && !dto.getBeneficiaire().isBlank())
            lines.add("Bénéficiaire : " + dto.getBeneficiaire());
        if (dto.getEmetteur() != null && !dto.getEmetteur().isBlank())
            lines.add("Émetteur : " + dto.getEmetteur());
        if (dto.getMontant() != null)
            lines.add("Montant : " + formatMontantPdf(dto.getMontant()) + " MAD");
        if (dto.getDateEmission() != null)
            lines.add("Date émission : " + dto.getDateEmission().format(DATE_FMT));
        if (dto.getDateEcheance() != null)
            lines.add("Date échéance : " + dto.getDateEcheance().format(DATE_FMT));
        return lines;
    }

    // ── Helpers internes ──────────────────────────────────────────────────────

    private void validateEscompteBusinessRule(Type type, NatureLcn nature) {
        if (nature == NatureLcn.ESCOMPTE && type != Type.ENCAISSEMENT) {
            log.warn("Règle métier violée: tentative de création d'un LCN ESCOMPTE avec le type {}", type);
            throw new LcnBusinessException(
                    "Règle métier violée: Un LCN de nature ESCOMPTE ne peut être créé que pour des opérations d'ENCAISSEMENT. " +
                            "Type fourni: " + type + ". Veuillez corriger le type ou la nature du LCN.");
        }
    }

    private Lcn trouverOuLeverException(Long id) {
        return lcnRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lcn with id " + id + " not found."));
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
     * Mappe l'entité vers le DTO puis applique les libellés d'affichage calculés
     * (règle métier propre au service — jamais dans le Mapper, cf. architecture.md).
     */
    private LcnResponseDTO toDto(Lcn lcn) {
        LcnResponseDTO dto = lcnMapper.toDto(lcn);
        dto.setObservation(" LCN N° " + lcn.getNumero());
        dto.setLibelle(lcn.getType() == Type.DECAISSEMENT ? lcn.getBeneficiaire() : lcn.getEmetteur());
        return dto;
    }
}
