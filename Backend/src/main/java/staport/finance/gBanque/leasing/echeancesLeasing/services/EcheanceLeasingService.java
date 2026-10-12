    package staport.finance.gBanque.leasing.echeancesLeasing.services;

import java.util.Set;
import staport.finance.gBanque.validation.services.ReverseTransactionService;
import staport.finance.gBanque.reports.utils.MontantFormatter;

    import com.lowagie.text.Chunk;
    import com.lowagie.text.DocumentException;
    import com.lowagie.text.Element;
    import com.lowagie.text.Font;
    import com.lowagie.text.FontFactory;
    import com.lowagie.text.Paragraph;
    import com.lowagie.text.Phrase;
    import com.lowagie.text.pdf.PdfPCell;
    import com.lowagie.text.pdf.PdfPTable;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;
    import org.springframework.data.domain.Page;
    import org.springframework.data.domain.PageRequest;
    import org.springframework.data.domain.Pageable;
    import org.springframework.data.domain.Sort;
    import org.springframework.data.jpa.domain.Specification;
    import org.springframework.stereotype.Service;
    import org.springframework.transaction.annotation.Transactional;
    import staport.finance.gBanque.audit.AuditOperation;
    import staport.finance.gBanque.audit.AuditableAction;
    import staport.finance.gBanque.commision.dtos.CommissionRequestDto;
    import staport.finance.gBanque.commision.service.ICommissionConfigService;
    import staport.finance.gBanque.commision.service.ICommissionService;
    import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
    import staport.finance.gBanque.exception.customExceptions.ValidationException;
    import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;
    import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasingPhase;
    import staport.finance.gBanque.leasing.echeancesLeasing.dto.*;
    import staport.finance.gBanque.leasing.echeancesLeasing.entities.EcheanceLeasing;
    import staport.finance.gBanque.leasing.echeancesLeasing.enums.StatutEcheanceLeasing;
    import staport.finance.gBanque.leasing.echeancesLeasing.mapper.EcheanceLeasingMapper;
    import staport.finance.gBanque.leasing.echeancesLeasing.repositories.EcheanceLeasingRepository;
    import staport.finance.gBanque.leasing.echeancesLeasing.specification.EcheanceLeasingSpecification;
    import staport.finance.gBanque.leasing.echeancesLeasing.services.interfaces.IEcheanceLeasingService;
    import staport.finance.gBanque.reports.service.StaportPdfReportService;
    import staport.finance.gBanque.transactions.enums.TransactionSource;

    import java.awt.Color;
    import java.time.LocalDate;
import java.math.BigDecimal;
    import java.math.RoundingMode;
            import java.time.format.DateTimeFormatter;
    import java.time.temporal.TemporalAdjusters;
    import java.util.ArrayList;
    import java.util.Collection;
    import java.util.Comparator;
    import java.util.HashMap;
    import java.util.List;
        import java.util.Map;


    /**
     * Service pour la gestion (consultation, mise à jour de statut) et les calculs agrégés
     * des échéances de leasing avec génération automatique des commissions.
     */
    @Service
    public class EcheanceLeasingService implements IEcheanceLeasingService {

        private static final Logger log = LoggerFactory.getLogger(EcheanceLeasingService.class);
        private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        private static final Color MED_BLUE = Color.decode("#2563eb");
        private static final Color LIGHT_BLUE = Color.decode("#dbeafe");
        private static final Color ROW_ALT = Color.decode("#f8fafc");
        private static final Color GREEN = Color.decode("#16a34a");
        private static final Color ORANGE = Color.decode("#ea580c");
        private static final Color BORDER_GRAY = Color.decode("#e5e7eb");

        private final ICommissionService commissionService;
        private final ICommissionConfigService commissionConfigService;
        private final EcheanceLeasingRepository echeanceLeasingRepository;
        private final EcheanceLeasingMapper echeanceLeasingMapper;
        private final StaportPdfReportService staportPdfReportService;
        private final ReverseTransactionService reverseTransactionService;
        private static final int SCALE = 2;

        public EcheanceLeasingService(ICommissionService commissionService,
                                      ICommissionConfigService commissionConfigService,
                                      EcheanceLeasingRepository echeanceLeasingRepository,
                                      EcheanceLeasingMapper echeanceLeasingMapper,
                                      StaportPdfReportService staportPdfReportService,
                                      ReverseTransactionService reverseTransactionService) {
            this.reverseTransactionService = reverseTransactionService;
            this.commissionService       = commissionService;
            this.commissionConfigService = commissionConfigService;
            this.echeanceLeasingRepository = echeanceLeasingRepository;
            this.echeanceLeasingMapper     = echeanceLeasingMapper;
            this.staportPdfReportService   = staportPdfReportService;
        }

        /**
         * Génère l'ensemble des échéances pour un nouveau contrat de leasing donné.
         * Cette méthode est appelée uniquement lors de la création initiale du contrat.
         * Elle génère aussi automatiquement les commissions pour les banques éligibles.
         *
         * @param contrat Le contrat de leasing pour lequel les échéances doivent être générées.
         * @throws ValidationException si les données du contrat sont insuffisantes pour le calcul.
         */
        @Transactional
        public void generateEcheances(ContratLeasing contrat) {
            log.info("Début de la génération des échéances pour le contrat ID : {}", contrat.getId());

            // 1. Générer les échéances
            List<EcheanceLeasing> echeances = _generateEcheancesInternal(contrat);

            // 2. Sauvegarder les échéances
            List<EcheanceLeasing> savedEcheances = echeanceLeasingRepository.saveAll(echeances);
            log.info("Échéances sauvegardées avec succès : {} échéances créées", savedEcheances.size());

            // 3. Générer les commissions pour les banques éligibles
            generateCommissionsForEcheances(contrat, savedEcheances);

            log.info("Génération des échéances terminée avec succès pour le contrat ID : {}", contrat.getId());
        }

        /**
         * Régénère les échéances ET les commissions pour un contrat existant.
         * Supprime d'abord les anciennes commissions et échéances avant de tout régénérer.
         *
         * @param contrat Le contrat de leasing (déjà chargé par l'appelant — ContratLeasingServiceImpl).
         */
        @Transactional
        public void regenerateEcheances(ContratLeasing contrat) {
            Long contratId = contrat.getId();
            log.info("Début de la régénération des échéances pour le contrat ID : {}", contratId);

            // 1. Vérifier s'il existe des échéances PAYÉES pour ce contrat
            boolean hasPaidEcheances = this.hasPaidEcheances(contratId);

            if (hasPaidEcheances) {
                log.warn("Régénération bloquée — contrat ID : {} contient des échéances payées", contratId);
                throw new ValidationException("Impossible de régénérer les échéances pour le contrat " + contratId +
                        " car il contient des échéances déjà payées. c-à-d qu'on a déjà commencé l'amortissement pour ce contrat. Veuillez contacter l'administrateur.");
            }

            deleteEcheancesAndCommissionByContrat(contrat);

            // 2. Régénérer les nouvelles échéances et commissions
            log.debug("Génération des nouvelles échéances pour le contrat ID : {}", contratId);
            generateEcheances(contrat);

            log.info("Régénération des échéances terminée avec succès pour le contrat ID : {}", contratId);
        }

        /**
         * Méthode interne pour le calcul des échéances.
         * Elle ne gère pas la sauvegarde des échéances.
         * Appelée uniquement par `generateEcheances`.
         *
         * @param contrat Le contrat de leasing
         * @return La liste des échéances générées (non sauvegardées)
         */
        private List<EcheanceLeasing> _generateEcheancesInternal(ContratLeasing contrat) {
            log.info("========== DÉBUT CALCUL ÉCHÉANCES LEASING - Contrat ID: {} ==========", contrat.getId());

            List<EcheanceLeasing> echeances = new ArrayList<>();

            // Date de début du contrat (déjà en LocalDate — aucune conversion timezone nécessaire)
            LocalDate dateReference = contrat.getDateDebut().minusMonths(1);
            log.debug(" Date de référence (1 mois avant la date début) : {}", dateReference);

            int numeroEcheanceGlobal = 0;

            // Trier les phases par numeroPhase
            List<ContratLeasingPhase> sortedPhases = contrat.getPhases().stream()
                    .sorted(Comparator.comparingInt(ContratLeasingPhase::getNumeroPhase))
                    .toList();

            log.info(" Génération des échéances - {} phase(s) à traiter", sortedPhases.size());

            // Génération des échéances de loyer
            for (ContratLeasingPhase phase : sortedPhases) {
                log.debug("Traitement de la phase {} - Durée : {} mois",
                        phase.getNumeroPhase(), phase.getDureeMois());

                BigDecimal mensualiteTTCDuMois = phase.getMensualiteTTC().setScale(SCALE, RoundingMode.HALF_UP);

                for (int i = 0; i < phase.getDureeMois(); i++) {
                    numeroEcheanceGlobal++;

                    // Calculer chaque date à partir de la date de référence
                    LocalDate dateEcheanceCourante = dateReference.plusMonths(numeroEcheanceGlobal);

                    //  LOG 6 : Échéances générées (les 3 premières et les 2 dernières de chaque phase)
                    if (i < 3 || i >= phase.getDureeMois() - 2) {
                        log.debug("   ✓ Phase {} - Échéance {} : {} (ref + {} mois)",
                                phase.getNumeroPhase(),
                                numeroEcheanceGlobal,
                                dateEcheanceCourante,
                                numeroEcheanceGlobal);
                    } else if (i == 3) {
                        log.debug("   ... ({} échéances intermédiaires) ...", phase.getDureeMois() - 5);
                    }

                    StatutEcheanceLeasing statut = StatutEcheanceLeasing.PREVU;

                    EcheanceLeasing echeance = new EcheanceLeasing(
                            contrat,
                            numeroEcheanceGlobal,
                            dateEcheanceCourante,
                            mensualiteTTCDuMois,
                            statut
                    );
                    echeance.setLibelle(echeanceLeasingMapper.computeLibelle(echeance));
                    echeance.setObservation("Prélèvement leasing - échéance n°" + numeroEcheanceGlobal
                            + "/" + (contrat.getPeriodeEnMois() + 1) + " - contrat N° " + contrat.getNumeroContrat());
                    echeances.add(echeance);
                }
            }
            log.info(" {} échéances de loyer générées", echeances.size());

            // Ajout de l'échéance dédiée pour la valeur résiduelle
            numeroEcheanceGlobal++;

            // Calculer la date de la valeur résiduelle à partir de la date de référence
            LocalDate dateValeurResiduelle = dateReference.plusMonths(numeroEcheanceGlobal);

            log.debug("  Échéance {} (Valeur résiduelle) : {} (ref + {} mois)",
                    numeroEcheanceGlobal,
                    dateValeurResiduelle,
                    numeroEcheanceGlobal);

            StatutEcheanceLeasing statut = StatutEcheanceLeasing.PREVU;

            EcheanceLeasing echeanceValeurResiduelle = new EcheanceLeasing(
                    contrat,
                    numeroEcheanceGlobal,
                    dateValeurResiduelle,
                    contrat.getValeurResiduelle().setScale(SCALE, RoundingMode.HALF_UP),
                    statut
            );
            echeanceValeurResiduelle.setLibelle(echeanceLeasingMapper.computeLibelle(echeanceValeurResiduelle));
            echeanceValeurResiduelle.setObservation("Prélèvement leasing - échéance n°" + numeroEcheanceGlobal
                    + "/" + (contrat.getPeriodeEnMois() + 1) + " - contrat N° " + contrat.getNumeroContrat() + " (valeur résiduelle)");

            echeances.add(echeanceValeurResiduelle);

            log.info(" Total échéances générées : {} (dont 1 valeur résiduelle)", echeances.size());
            log.info("========== FIN CALCUL ÉCHÉANCES LEASING ==========\n");

            return echeances;
        }

        /**
         * Génère les commissions pour toutes les échéances d'un contrat.
         * Les commissions sont créées uniquement si la banque est configurée dans le YAML.
         * Prend en compte les règles conditionnelles basées sur l'organisme de leasing.
         *
         * @param contrat Le contrat de leasing
         * @param echeances La liste des échéances générées et sauvegardées
         */
        private void generateCommissionsForEcheances(ContratLeasing contrat, List<EcheanceLeasing> echeances) {
            String nomBanque = contrat.getBanqueDePrelevement().getNom();
            String nomOrganisme = contrat.getContratCadre().getOrganisme().getNom();

            log.debug("Vérification de l'éligibilité pour la génération de commissions");
            log.debug("  → Banque : {}, Organisme : {}", nomBanque, nomOrganisme);

            // Vérifier si la banque a une commission configurée
            if (!commissionConfigService.hasCommission(nomBanque, TransactionSource.LEASING)) {
                log.debug("Aucune commission configurée pour la banque '{}'. Génération ignorée.", nomBanque);
                return;
            }

            // Récupérer le montant en tenant compte de l'organisme
            BigDecimal montantCommission = commissionConfigService
                    .resolveCommission(nomBanque, nomOrganisme, TransactionSource.LEASING)
                    .orElse(null);

            if (montantCommission == null) {
                log.warn("Impossible de déterminer le montant de commission pour {} / {}", nomBanque, nomOrganisme);
                return;
            }

            log.debug("Commission configurée pour la banque '{}' / organisme '{}'", nomBanque, nomOrganisme);

            int commissionsCreees = 0;
            int commissionsEchec = 0;

            // Créer une commission pour chaque échéance
            for (EcheanceLeasing echeance : echeances) {
                try {
                    CommissionRequestDto commissionDto = creerCommissionDto(contrat, echeance, montantCommission);
                    commissionService.createCommission(commissionDto);
                    commissionsCreees++;

                    log.debug("Commission créée pour l'échéance {}", echeance.getNumeroEcheance());

                } catch (Exception e) {
                    commissionsEchec++;
                    log.error("Erreur lors de la création de la commission pour l'échéance {} : {}",
                            echeance.getNumeroEcheance(), e.getMessage());
                }
            }

            log.info("Génération des commissions terminée - Créées : {}, Échecs : {}, Total : {}",
                    commissionsCreees, commissionsEchec, echeances.size());

            if (commissionsEchec > 0) {
                log.warn("  {} commission(s) n'ont pas pu être créées pour le contrat {}",
                        commissionsEchec, contrat.getNumeroContrat());
            }
        }
        /**
         * Crée un DTO de commission à partir d'une échéance.
         *
         * @param contrat Le contrat de leasing
         * @param echeance L'échéance concernée
         * @param montantCommission Le montant de la commission (depuis la config YAML)
         * @return Le DTO de commission prêt à être créé
         */
        private CommissionRequestDto creerCommissionDto(
                ContratLeasing contrat,
                EcheanceLeasing echeance,
                BigDecimal montantCommission) {

            // Libellé et observation avec le numéro de contrat et d'échéance
            String texte = String.format(
                    "Commission Prelevement Leasing contrat N°%s",
                    contrat.getNumeroContrat()
            );

            return new CommissionRequestDto(
                    texte,
                    texte,
                    // Montant de la commission (montant fixe depuis la config YAML)
                    montantCommission,
                    // Banque
                    contrat.getBanqueDePrelevement().getId(),
                    // Opération
                    TransactionSource.LEASING,
                    // Date d'émission = date d'échéance
                    echeance.getDateEcheance()
            );
        }



        // ==================== MÉTHODES EXISTANTES (INCHANGÉES) ====================

        @Transactional(readOnly = true)
        public TableauAmortissementDTO getTableauAmortissementForContrat(Long contratId, String numeroContrat,
                                                                          EcheanceLeasingSearchDTO searchDTO, Pageable pageable) {
            log.debug("Récupération du tableau d'amortissement pour le contrat ID : {}", contratId);
            log.debug("Critères de recherche : {}, Page : {}, Taille : {}", searchDTO, pageable.getPageNumber(), pageable.getPageSize());

            Specification<EcheanceLeasing> spec = EcheanceLeasingSpecification.searchByCriteria(searchDTO)
                    .and(EcheanceLeasingSpecification.byContratId(contratId));

            Page<EcheanceLeasing> echeancesPage = echeanceLeasingRepository.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));
            Page<EcheanceLeasingResponseDTO> echeancesDtoPage = echeancesPage.map(echeanceLeasingMapper::toDto);

            log.debug("Nombre d'échéances dans la page : {}", echeancesPage.getNumberOfElements());

            BigDecimal totalIndemniteAPayer = echeanceLeasingRepository
                    .sumMontantTTCByContratLeasingId(contratId)
                    .setScale(SCALE, RoundingMode.HALF_UP);

            BigDecimal montantTotalDejaPaye = echeanceLeasingRepository
                    .sumMontantTTCByContratLeasingIdAndStatutPaiement(contratId, StatutEcheanceLeasing.PAYE)
                    .setScale(SCALE, RoundingMode.HALF_UP);

            BigDecimal montantRestantAPayer = totalIndemniteAPayer.subtract(montantTotalDejaPaye).setScale(SCALE, RoundingMode.HALF_UP);
            log.debug("Tableau d'amortissement calculé pour contrat ID : {}", contratId);

            return new TableauAmortissementDTO(
                    contratId,
                    numeroContrat,
                    totalIndemniteAPayer,
                    montantTotalDejaPaye,
                    montantRestantAPayer,
                    echeancesDtoPage
            );
        }

        @AuditableAction(operation = AuditOperation.UPDATE, entity = EcheanceLeasing.class)
        @Transactional
        public EcheanceLeasingResponseDTO toggleStatus(Long echeanceId) {
            EcheanceLeasing echeance = echeanceLeasingRepository.findById(echeanceId)
                    .orElseThrow(() -> {
                        log.warn("EcheanceLeasing non trouvée — ID : {}", echeanceId);
                        return new ResourceNotFoundException("EcheanceLeasing with ID " + echeanceId + " not found.");
                    });

            // Retour « en cours » d'une opération déjà rapprochée : sa transaction est annulée (même effet que l'annulation
            // de la validation) ; sans transaction liée, la bascule reste libre (correction manuelle).
            if (echeance.getStatutPaiement() == StatutEcheanceLeasing.PAYE
                    && reverseTransactionService.annulerSiRapproche(Set.of(TransactionSource.LEASING), echeanceId).effectuee()) {
                return echeanceLeasingMapper.toDto(echeance);
            }
            StatutEcheanceLeasing newStatus = echeance.getStatutPaiement() == StatutEcheanceLeasing.PAYE
                    ? StatutEcheanceLeasing.PREVU
                    : StatutEcheanceLeasing.PAYE;
            echeance.setStatutPaiement(newStatus);
            echeance = this.echeanceLeasingRepository.save(echeance);
            log.info("Statut échéance ID : {} → {}", echeanceId, newStatus);
            return echeanceLeasingMapper.toDto(echeance);
        }

        /**
         * Met à jour les champs d'une échéance dont le statut est {@code PREVU}.
         * Toute tentative de modification d'une échéance {@code PAYE} lève une {@link ValidationException}.
         *
         * @param id  L'identifiant de l'échéance à modifier
         * @param dto Les nouvelles valeurs des champs
         * @return Le DTO de l'échéance mise à jour
         */
        @AuditableAction(operation = AuditOperation.UPDATE, entity = EcheanceLeasing.class)
        @Transactional
        public EcheanceLeasingResponseDTO updateEcheance(Long id, EcheanceLeasingUpdateRequestDTO dto) {
            EcheanceLeasing echeance = echeanceLeasingRepository.findById(id)
                    .orElseThrow(() -> {
                        log.warn("EcheanceLeasing non trouvée — ID : {}", id);
                        return new ResourceNotFoundException("EcheanceLeasing with ID " + id + " not found.");
                    });

            if (echeance.getStatutPaiement() == StatutEcheanceLeasing.PAYE) {
                log.warn("Tentative de modification d'une échéance déjà payée — ID : {}", id);
                throw new ValidationException("Impossible de modifier une échéance dont le statut est PAYE.");
            }

            echeance.setDateEcheance(dto.dateEcheance());
            echeance.setMontantTTC(dto.montantTTC());
            echeance.setDateOperation(dto.dateOperation());
            echeance.setDateValeur(dto.dateValeur());
            if (dto.observation() != null) echeance.setObservation(dto.observation());
            if (dto.libelle() != null) echeance.setLibelle(dto.libelle());

            EcheanceLeasing saved = echeanceLeasingRepository.save(echeance);
            log.info("Échéance ID : {} mise à jour avec succès", id);
            return echeanceLeasingMapper.toDto(saved);
        }

        /**
         * Récupère une échéance de leasing par son identifiant.
         * Utilisé par l'écran de récapitulatif unifié (validation) pour résoudre le contrat de
         * leasing avant l'ouverture du formulaire de validation.
         *
         * @param id L'identifiant de l'échéance de leasing
         * @return Le DTO de l'échéance correspondante
         * @throws ResourceNotFoundException si aucune échéance ne correspond à cet ID
         */
        @Transactional(readOnly = true)
        public EcheanceLeasingResponseDTO getEcheanceLeasingById(Long id) {
            EcheanceLeasing echeance = echeanceLeasingRepository.findById(id)
                    .orElseThrow(() -> {
                        log.warn("EcheanceLeasing non trouvée — ID : {}", id);
                        return new ResourceNotFoundException("EcheanceLeasing with ID " + id + " not found.");
                    });
            return echeanceLeasingMapper.toDto(echeance);
        }

        @Override
        @Transactional(readOnly = true)
        public Map<Long, MontantsContratLeasing> getMontantsByContratIds(Collection<Long> contratIds) {
            if (contratIds == null || contratIds.isEmpty()) {
                return Map.of();
            }
            Map<Long, BigDecimal> payes = new HashMap<>();
            for (Object[] row : echeanceLeasingRepository.sumMontantTTCGroupedByContratAndStatut(contratIds, StatutEcheanceLeasing.PAYE)) {
                payes.put((Long) row[0], (BigDecimal) row[1]);
            }
            Map<Long, MontantsContratLeasing> montants = new HashMap<>();
            for (Object[] row : echeanceLeasingRepository.sumMontantTTCGroupedByContrat(contratIds)) {
                Long contratId = (Long) row[0];
                montants.put(contratId, new MontantsContratLeasing(
                        ((BigDecimal) row[1]).setScale(SCALE, RoundingMode.HALF_UP),
                        payes.getOrDefault(contratId, BigDecimal.ZERO).setScale(SCALE, RoundingMode.HALF_UP)));
            }
            return montants;
        }

        @Transactional(readOnly = true)
        public boolean hasPaidEcheances(Long contratId) {
            log.debug("Vérification de l'existence d'échéances payées pour le contrat ID : {}", contratId);

            boolean hasPaid = echeanceLeasingRepository.existsByContratLeasingIdAndStatutPaiement(
                    contratId, StatutEcheanceLeasing.PAYE);

            log.debug("Le contrat ID : {} {} d'échéances payées",
                    contratId, hasPaid ? "possède" : "ne possède pas");

            return hasPaid;
        }

        public void deleteEcheancesAndCommissionByContrat(ContratLeasing contrat){



            // 3. Supprimer d'abord les anciennes commissions
            log.debug("Suppression des anciennes commissions pour le contrat N° : {}", contrat.getNumeroContrat());
            commissionService.deleteCommissionsByContratLeasing(contrat.getNumeroContrat());

            // 4. Supprimer les anciennes échéances
            log.debug("Suppression des anciennes échéances pour le contrat ID : {}", contrat.getId());
            echeanceLeasingRepository.deleteByContratLeasing(contrat);

        }

        // ==================== EXPORT PDF ====================

        @Transactional(readOnly = true)
        @Override
        public byte[] exportTableauAmortissementPdf(ContratLeasing contrat, EcheanceLeasingSearchDTO searchDTO) {
            log.info("Export PDF du tableau d'amortissement — contrat leasing ID : {}", contrat.getId());

            BigDecimal montantTotalAPayer = echeanceLeasingRepository
                    .sumMontantTTCByContratLeasingId(contrat.getId())
                    .setScale(SCALE, RoundingMode.HALF_UP);

            BigDecimal montantTotalDejaPaye = echeanceLeasingRepository
                    .sumMontantTTCByContratLeasingIdAndStatutPaiement(contrat.getId(), StatutEcheanceLeasing.PAYE)
                    .setScale(SCALE, RoundingMode.HALF_UP);

            BigDecimal montantRestantAPayer = montantTotalAPayer.subtract(montantTotalDejaPaye);

            EcheanceLeasingSearchDTO effectiveSearchDto = (searchDTO == null
                    ? new EcheanceLeasingSearchDTO(contrat.getId(), null, null, null, null, null, null, null, null)
                    : new EcheanceLeasingSearchDTO(contrat.getId(), searchDTO.numeroEcheance(),
                            searchDTO.dateEcheanceDebut(), searchDTO.dateEcheanceFin(),
                            searchDTO.dateOperationDebut(), searchDTO.dateOperationFin(),
                            searchDTO.dateValeurDebut(), searchDTO.dateValeurFin(),
                            searchDTO.statutPaiement()));
            Specification<EcheanceLeasing> spec = EcheanceLeasingSpecification.searchByCriteria(effectiveSearchDto);
            List<EcheanceLeasing> echeances = echeanceLeasingRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "numeroEcheance"));

            List<String> filterLines = buildTableauFilterSummary(searchDTO);
            String title = "Tableau d'Amortissement — Contrat N°" + contrat.getNumeroContrat();

            try {
                return staportPdfReportService.generateReport(title, (doc, writer) -> {
                    try {
                        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
                        Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.decode("#374151"));
                        Font hdrFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
                        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
                        Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.decode("#1e3a8a"));

                        doc.add(Chunk.NEWLINE);
                        doc.add(Chunk.NEWLINE);

                        Paragraph titleParagraph = new Paragraph(title, titleFont);
                        titleParagraph.setAlignment(Element.ALIGN_CENTER);
                        doc.add(titleParagraph);

                        String exportInfo = "Exporté le " + LocalDate.now().format(DATE_FMT)
                                + "  •  " + echeances.size() + " échéance(s)";
                        if (!filterLines.isEmpty()) {
                            exportInfo += "  •  Filtres : " + String.join("  |  ", filterLines);
                        }
                        Paragraph sub = new Paragraph(exportInfo, subFont);
                        sub.setAlignment(Element.ALIGN_CENTER);
                        sub.setSpacingAfter(6f);
                        doc.add(sub);

                        // Résumé Endettement / Payé / Restant, identique à l'écran
                        PdfPTable summaryTable = new PdfPTable(3);
                        summaryTable.setWidthPercentage(100f);
                        summaryTable.setSpacingAfter(10f);
                        addSummaryCell(summaryTable, "Endettement", MontantFormatter.format(montantTotalAPayer), totalFont, subFont);
                        addSummaryCell(summaryTable, "Montant déjà payé", MontantFormatter.format(montantTotalDejaPaye), totalFont, subFont);
                        addSummaryCell(summaryTable, "Montant restant à payer", MontantFormatter.format(montantRestantAPayer), totalFont, subFont);
                        doc.add(summaryTable);

                        float[] colWidths = {10f, 15f, 15f, 12f};
                        PdfPTable table = new PdfPTable(colWidths);
                        table.setWidthPercentage(100f);
                        table.setSpacingBefore(4f);
                        table.setHeaderRows(1);

                        for (String h : new String[]{"N° Échéance", "Date d'échéance", "Montant TTC (MAD)", "Statut"}) {
                            PdfPCell c = new PdfPCell(new Phrase(h, hdrFont));
                            c.setBackgroundColor(MED_BLUE);
                            c.setPadding(5f);
                            c.setHorizontalAlignment(Element.ALIGN_CENTER);
                            c.setBorderColor(Color.WHITE);
                            c.setBorderWidth(0.5f);
                            table.addCell(c);
                        }

                        for (int i = 0; i < echeances.size(); i++) {
                            EcheanceLeasing e = echeances.get(i);
                            Color rowBg = i % 2 == 0 ? Color.WHITE : ROW_ALT;

                            addPdfCell(table, String.valueOf(e.getNumeroEcheance()), cellFont, rowBg, BORDER_GRAY, Element.ALIGN_CENTER);
                            addPdfCell(table, e.getDateEcheance() != null ? e.getDateEcheance().format(DATE_FMT) : "—", cellFont, rowBg, BORDER_GRAY, Element.ALIGN_CENTER);
                            addPdfCell(table, MontantFormatter.format(e.getMontantTTC()), cellFont, rowBg, BORDER_GRAY, Element.ALIGN_RIGHT);

                            boolean paye = e.getStatutPaiement() == StatutEcheanceLeasing.PAYE;
                            PdfPCell statusCell = new PdfPCell(new Phrase(
                                    paye ? "PAYÉ" : "PRÉVU",
                                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE)));
                            statusCell.setBackgroundColor(paye ? GREEN : ORANGE);
                            statusCell.setPadding(4f);
                            statusCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                            statusCell.setBorderColor(BORDER_GRAY);
                            statusCell.setBorderWidth(0.5f);
                            table.addCell(statusCell);
                        }

                        doc.add(table);
                    } catch (DocumentException e) {
                        throw new RuntimeException("Erreur lors de la génération du contenu du PDF du tableau d'amortissement.", e);
                    }
                });
            } catch (DocumentException e) {
                throw new RuntimeException("Erreur lors de la génération du PDF du tableau d'amortissement.", e);
            }
        }

        private void addSummaryCell(PdfPTable table, String label, String value, Font valueFont, Font labelFont) {
            Paragraph content = new Paragraph();
            content.add(new Phrase(label + "\n", labelFont));
            content.add(new Phrase(value + " MAD", valueFont));
            PdfPCell cell = new PdfPCell(content);
            cell.setBackgroundColor(LIGHT_BLUE);
            cell.setPadding(6f);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBorderColor(MED_BLUE);
            table.addCell(cell);
        }

        private List<String> buildTableauFilterSummary(EcheanceLeasingSearchDTO dto) {
            List<String> lines = new ArrayList<>();
            if (dto == null) return lines;

            if (dto.numeroEcheance() != null) {
                lines.add("N° Échéance : " + dto.numeroEcheance());
            }
            if (dto.statutPaiement() != null) {
                lines.add("Statut : " + dto.statutPaiement().name());
            }
            if (dto.dateEcheanceDebut() != null) {
                lines.add("Date échéance min : " + dto.dateEcheanceDebut().format(DATE_FMT));
            }
            if (dto.dateEcheanceFin() != null) {
                lines.add("Date échéance max : " + dto.dateEcheanceFin().format(DATE_FMT));
            }
            if (dto.dateOperationDebut() != null) {
                lines.add("Date opération min : " + dto.dateOperationDebut().format(DATE_FMT));
            }
            if (dto.dateOperationFin() != null) {
                lines.add("Date opération max : " + dto.dateOperationFin().format(DATE_FMT));
            }
            if (dto.dateValeurDebut() != null) {
                lines.add("Date valeur min : " + dto.dateValeurDebut().format(DATE_FMT));
            }
            if (dto.dateValeurFin() != null) {
                lines.add("Date valeur max : " + dto.dateValeurFin().format(DATE_FMT));
            }
            return lines;
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
    }
