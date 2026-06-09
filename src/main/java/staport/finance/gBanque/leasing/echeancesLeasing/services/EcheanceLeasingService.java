    package staport.finance.gBanque.leasing.echeancesLeasing.services;

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
    import staport.finance.gBanque.Bank.repositories.BanqueRepository;
    import staport.finance.gBanque.commision.config.CommissionLeasingConfig;
    import staport.finance.gBanque.commision.dtos.CommissionRequestDto;
    import staport.finance.gBanque.commision.service.CommissionService;
    import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
    import staport.finance.gBanque.exception.customExceptions.ValidationException;
    import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;
    import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasingPhase;
    import staport.finance.gBanque.leasing.contratLeasing.repositories.ContratLeasingRepository;
    import staport.finance.gBanque.leasing.echeancesLeasing.dto.*;
    import staport.finance.gBanque.leasing.echeancesLeasing.entities.EcheanceLeasing;
    import staport.finance.gBanque.leasing.echeancesLeasing.enums.StatutEcheanceLeasing;
    import staport.finance.gBanque.leasing.echeancesLeasing.mapper.EcheanceLeasingMapper;
    import staport.finance.gBanque.leasing.echeancesLeasing.repositories.EcheanceLeasingRepository;
    import staport.finance.gBanque.leasing.echeancesLeasing.specification.EcheanceLeasingSpecification;
    import staport.finance.gBanque.transactions.enums.TransactionSource;

    import java.time.LocalDate;
import java.math.BigDecimal;
    import java.math.RoundingMode;
    import java.time.temporal.TemporalAdjusters;
    import java.util.ArrayList;
    import java.util.Comparator;
    import java.util.List;


    /**
     * Service pour la gestion (consultation, mise à jour de statut) et les calculs agrégés
     * des échéances de leasing avec génération automatique des commissions.
     */
    @Service
    public class EcheanceLeasingService {

        private static final Logger log = LoggerFactory.getLogger(EcheanceLeasingService.class);

        private final CommissionService commissionService;
        private final CommissionLeasingConfig commissionConfig;
        private final EcheanceLeasingRepository echeanceLeasingRepository;
        private final ContratLeasingRepository contratLeasingRepository;
        private final EcheanceLeasingMapper echeanceLeasingMapper;
        private final BanqueRepository banqueRepository;
        private static final int SCALE = 2;

        public EcheanceLeasingService(CommissionService commissionService,
                                      CommissionLeasingConfig commissionConfig,
                                      EcheanceLeasingRepository echeanceLeasingRepository,
                                      ContratLeasingRepository contratLeasingRepository,
                                      EcheanceLeasingMapper echeanceLeasingMapper,
                                      BanqueRepository banqueRepository) {
            this.commissionService = commissionService;
            this.commissionConfig = commissionConfig;
            this.echeanceLeasingRepository = echeanceLeasingRepository;
            this.contratLeasingRepository = contratLeasingRepository;
            this.echeanceLeasingMapper = echeanceLeasingMapper;
            this.banqueRepository = banqueRepository;
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
         * @param contratId L'ID du contrat
         */
        @Transactional
        public void regenerateEcheances(Long contratId) {
            log.info("Début de la régénération des échéances pour le contrat ID : {}", contratId);

            // 1. Charger le contrat et valider son existence
            ContratLeasing contrat = contratLeasingRepository.findById(contratId)
                    .orElseThrow(() -> {
                        log.error("Contrat de leasing non trouvé avec l'ID : {}", contratId);
                        return new ResourceNotFoundException("ContratLeasing with ID " + contratId + " not found.");
                    });

            log.debug("Contrat trouvé : {}", contrat.getNumeroContrat());

            // 2. Vérifier s'il existe des échéances PAYÉES pour ce contrat
            boolean hasPaidEcheances = this.hasPaidEcheances(contratId);

            if (hasPaidEcheances) {
                log.error("Tentative de régénération d'échéances pour le contrat ID : {} avec des échéances déjà payées", contratId);
                throw new ValidationException("Impossible de régénérer les échéances pour le contrat " + contratId +
                        " car il contient des échéances déjà payées. c-à-d qu'on a déjà commencé l'amortissement pour ce contrat. Veuillez contacter l'administrateur.");
            }

            deleteEcheancesAndCommissionByContrat(contrat);



            // 5. Régénérer les nouvelles échéances et commissions
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
            log.info(" Date de référence (1 mois avant la date début) : {}", dateReference);

            log.info(" [ÉTAPE 5] Date de référence NORMALISÉE (12h UTC) : {}", dateReference);
            log.info("   └─ Détails : Année={}, Mois={}, Jour={}",
                    dateReference.getYear(),
                    dateReference.getMonthValue(),
                    dateReference.getDayOfMonth());

            int numeroEcheanceGlobal = 0;

            // Trier les phases par numeroPhase
            List<ContratLeasingPhase> sortedPhases = contrat.getPhases().stream()
                    .sorted(Comparator.comparingInt(ContratLeasingPhase::getNumeroPhase))
                    .toList();

            log.info(" Génération des échéances - {} phase(s) à traiter", sortedPhases.size());

            // Génération des échéances de loyer
            for (ContratLeasingPhase phase : sortedPhases) {
                log.debug("Traitement de la phase {} - Durée : {} mois, Mensualité TTC : {}",
                        phase.getNumeroPhase(), phase.getDureeMois(), phase.getMensualiteTTC());

                BigDecimal mensualiteTTCDuMois = phase.getMensualiteTTC().setScale(SCALE, RoundingMode.HALF_UP);

                for (int i = 0; i < phase.getDureeMois(); i++) {
                    numeroEcheanceGlobal++;

                    // Calculer chaque date à partir de la date de référence
                    LocalDate dateEcheanceCourante = dateReference.plusMonths(numeroEcheanceGlobal);

                    //  LOG 6 : Échéances générées (les 3 premières et les 2 dernières de chaque phase)
                    if (i < 3 || i >= phase.getDureeMois() - 2) {
                        log.info("   ✓ Phase {} - Échéance {} : {} (ref + {} mois)",
                                phase.getNumeroPhase(),
                                numeroEcheanceGlobal,
                                dateEcheanceCourante,
                                numeroEcheanceGlobal);
                    } else if (i == 3) {
                        log.info("   ... ({} échéances intermédiaires) ...", phase.getDureeMois() - 5);
                    }

                    StatutEcheanceLeasing statut = StatutEcheanceLeasing.PREVU;

                    EcheanceLeasing echeance = new EcheanceLeasing(
                            contrat,
                            numeroEcheanceGlobal,
                            dateEcheanceCourante,
                            mensualiteTTCDuMois,
                            statut
                    );
                    echeances.add(echeance);
                }
            }
            log.info(" {} échéances de loyer générées", echeances.size());

            // Ajout de l'échéance dédiée pour la valeur résiduelle
            numeroEcheanceGlobal++;

            // Calculer la date de la valeur résiduelle à partir de la date de référence
            LocalDate dateValeurResiduelle = dateReference.plusMonths(numeroEcheanceGlobal);

            log.info("  Échéance {} (Valeur résiduelle) : {} (ref + {} mois)",
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
            String nomOrganisme = contrat.getOrganismeLeasing().getNom();

            log.info("Vérification de l'éligibilité pour la génération de commissions");
            log.info("  → Banque : {}", nomBanque);
            log.info("  → Organisme : {}", nomOrganisme);

            // Vérifier si la banque a une commission configurée
            if (!commissionConfig.hasCommission(nomBanque)) {
                log.info("Aucune commission configurée pour la banque '{}'. Génération de commissions ignorée.", nomBanque);
                return;
            }

            //  Récupérer la commission en tenant compte de l'organisme
            BigDecimal montantCommission = commissionConfig.getCommission(nomBanque, nomOrganisme);

            if (montantCommission == null) {
                log.warn("Impossible de déterminer le montant de commission pour {} / {}", nomBanque, nomOrganisme);
                return;
            }

            log.info("Commission calculée : {} DH par échéance (Banque: {}, Organisme: {})",
                    montantCommission, nomBanque, nomOrganisme);

            int commissionsCreees = 0;
            int commissionsEchec = 0;

            // Créer une commission pour chaque échéance
            for (EcheanceLeasing echeance : echeances) {
                try {
                    CommissionRequestDto commissionDto = creerCommissionDto(contrat, echeance, montantCommission);
                    commissionService.createCommission(commissionDto);
                    commissionsCreees++;

                    log.debug("Commission créée pour l'échéance {} - Montant : {} DH",
                            echeance.getNumeroEcheance(), montantCommission);

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

            CommissionRequestDto dto = new CommissionRequestDto();

            // Libellé et observation avec le numéro de contrat et d'échéance
            String texte = String.format(
                    "Commission Prelevement Leasing contrat N°%s",
                    contrat.getNumeroContrat()
            );
            dto.setLibelle(texte);
            dto.setObservation(texte);

            // Montant de la commission (montant fixe depuis la config YAML)
            dto.setMontant(montantCommission);

            // Banque
            dto.setBanqueId(contrat.getBanqueDePrelevement().getId());

            // Opération
            dto.setOperation(TransactionSource.LEASING);

            // Date d'émission = date d'échéance
            dto.setDateEmission(echeance.getDateEcheance());

            return dto;
        }



        // ==================== MÉTHODES EXISTANTES (INCHANGÉES) ====================

        @Transactional(readOnly = true)
        public TableauAmortissementDTO getTableauAmortissementForContrat(Long contratId, EcheanceLeasingSearchDTO searchDTO, Pageable pageable) {
            log.info("Récupération du tableau d'amortissement pour le contrat ID : {}", contratId);
            log.debug("Critères de recherche : {}, Page : {}, Taille : {}", searchDTO, pageable.getPageNumber(), pageable.getPageSize());

            ContratLeasing contrat = contratLeasingRepository.findById(contratId)
                    .orElseThrow(() -> {
                        log.error("Contrat de leasing non trouvé avec l'ID : {}", contratId);
                        return new ResourceNotFoundException("ContratLeasing with ID " + contratId + " not found.");
                    });

            Specification<EcheanceLeasing> spec = EcheanceLeasingSpecification.buildSpecification(searchDTO)
                    .and(EcheanceLeasingSpecification.byContratId(contratId));

            Page<EcheanceLeasing> echeancesPage = echeanceLeasingRepository.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));
            Page<EcheanceLeasingResponseDTO> echeancesDtoPage = echeancesPage.map(echeanceLeasingMapper::toDto);

            log.debug("Nombre d'échéances dans la page : {}", echeancesPage.getNumberOfElements());

            List<EcheanceLeasing> allEcheancesForTotals = echeanceLeasingRepository.findByContratLeasing(contrat);

            BigDecimal totalIndemniteAPayer = allEcheancesForTotals.stream()
                    .map(EcheanceLeasing::getMontantTTC)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(SCALE, RoundingMode.HALF_UP);

            BigDecimal montantTotalDejaPaye = allEcheancesForTotals.stream()
                    .filter(e -> e.getStatutPaiement() == StatutEcheanceLeasing.PAYE)
                    .map(EcheanceLeasing::getMontantTTC)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(SCALE, RoundingMode.HALF_UP);

            BigDecimal montantRestantAPayer = totalIndemniteAPayer.subtract(montantTotalDejaPaye).setScale(SCALE, RoundingMode.HALF_UP);
            log.info("Tableau d'amortissement calculé - Total : {}, Payé : {}, Restant : {}", totalIndemniteAPayer, montantTotalDejaPaye, montantRestantAPayer);

            return new TableauAmortissementDTO(
                    contrat.getId(),
                    contrat.getNumeroContrat(),
                    totalIndemniteAPayer,
                    montantTotalDejaPaye,
                    montantRestantAPayer,
                    echeancesDtoPage
            );
        }

        @Transactional
        public EcheanceLeasingResponseDTO toggleStatus(Long echeanceId) {
            log.info("Début de la modification du statut de l'échéance ID : {}", echeanceId);

            EcheanceLeasing echeance = echeanceLeasingRepository.findById(echeanceId)
                    .orElseThrow(() -> {
                        log.error("Échéance de leasing non trouvée avec l'ID : {}", echeanceId);
                        return new ResourceNotFoundException("EcheanceLeasing with ID " + echeanceId + " not found.");
                    });

            StatutEcheanceLeasing currentStatus = echeance.getStatutPaiement();
            StatutEcheanceLeasing newStatus = currentStatus == StatutEcheanceLeasing.PAYE
                    ? StatutEcheanceLeasing.PREVU
                    : StatutEcheanceLeasing.PAYE;

            log.debug("Changement du statut de {} à {} pour l'échéance ID : {}", currentStatus, newStatus, echeanceId);
            echeance.setStatutPaiement(newStatus);
            echeance = this.echeanceLeasingRepository.save(echeance);
            log.info("Statut de l'échéance ID : {} modifié avec succès", echeanceId);

            return echeanceLeasingMapper.toDto(echeance);
        }

        @Transactional(readOnly = true)
        public EncoursLeasingResponseDTO getEncoursLeasingForBanque(EncoursLeasingRequestDTO requestDTO, Pageable pageable) {
            LocalDate endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

            log.info("Récupération des encours de leasing pour la banque ID : {}", requestDTO.getBanqueId());
            log.debug("Paramètres de requête : {}, Page : {}, Taille : {}", requestDTO, pageable.getPageNumber(), pageable.getPageSize());

            Banque banque = banqueRepository.findById(requestDTO.getBanqueId())
                    .orElseThrow(() -> {
                        log.error("Banque non trouvée avec l'ID : {}", requestDTO.getBanqueId());
                        return new ResourceNotFoundException("Banque with ID " + requestDTO.getBanqueId() + " not found.");
                    });

            log.debug("Banque trouvée : {}", banque.getNom());

            Specification<EcheanceLeasing> spec = EcheanceLeasingSpecification.byContratBanqueId(requestDTO.getBanqueId())
                    .and(EcheanceLeasingSpecification.outstandingUntil(endOfMonth));

            if (requestDTO.getOrganismeLeasingId() != null) {
                log.debug("Filtre par organisme de leasing ID : {}", requestDTO.getOrganismeLeasingId());
                spec = spec.and(EcheanceLeasingSpecification.byContratOrganismeLeasingId(requestDTO.getOrganismeLeasingId()));
            }

            if (requestDTO.getNumeroContrat() != null && !requestDTO.getNumeroContrat().isEmpty()) {
                log.debug("Filtre par numéro de contrat : {}", requestDTO.getNumeroContrat());
                spec = spec.and(EcheanceLeasingSpecification.byContratNumeroContrat(requestDTO.getNumeroContrat()));
            }

            if (requestDTO.getMontant() != null) {
                log.debug("Filtre par montant : {}", requestDTO.getMontant());
                spec = spec.and(EcheanceLeasingSpecification.byMontant(requestDTO.getMontant()));
            }

            Page<EcheanceLeasing> encoursEcheancesPage = echeanceLeasingRepository.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));
            Page<EcheanceLeasingResponseDTO> echeancesDtoPage = encoursEcheancesPage.map(echeanceLeasingMapper::toDto);

            log.debug("Nombre d'échéances en encours dans la page : {}", encoursEcheancesPage.getNumberOfElements());

            List<EcheanceLeasing> allOutstandingEcheances = echeanceLeasingRepository.findAll(spec);

            BigDecimal totalEncours = allOutstandingEcheances.stream()
                    .map(EcheanceLeasing::getMontantTTC)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(SCALE, RoundingMode.HALF_UP);

            log.info("Encours total calculé pour la banque {} : {}", banque.getNom(), totalEncours);

            return new EncoursLeasingResponseDTO(
                    banque.getId(),
                    banque.getNom(),
                    totalEncours,
                    echeancesDtoPage
            );
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
    }