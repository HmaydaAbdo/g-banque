package staport.finance.gBanque.leasing.echeancesLeasing.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;


/**
 * Service pour la gestion (consultation, mise à jour de statut) et les calculs agrégés
 * des échéances de leasing.
 */
@Service
public class EcheanceLeasingService {


    private static final Logger log = LoggerFactory.getLogger(EcheanceLeasingService.class);

    private final EcheanceLeasingRepository echeanceLeasingRepository;
    private final ContratLeasingRepository contratLeasingRepository;
    private final EcheanceLeasingMapper echeanceLeasingMapper;
    private final BanqueRepository banqueRepository;
    private static final int SCALE = 2;



    public EcheanceLeasingService(EcheanceLeasingRepository echeanceLeasingRepository,
                                  ContratLeasingRepository contratLeasingRepository,
                                  EcheanceLeasingMapper echeanceLeasingMapper,
                                  BanqueRepository banqueRepository) {
        this.echeanceLeasingRepository = echeanceLeasingRepository;
        this.contratLeasingRepository = contratLeasingRepository;
        this.echeanceLeasingMapper = echeanceLeasingMapper;
        this.banqueRepository = banqueRepository;

    }



    /**
     * Génère l'ensemble des échéances pour un nouveau contrat de leasing donné.
     * Cette méthode est appelée uniquement lors de la création initiale du contrat.
     * Elle ne gère pas la régénération/modification des échéances existantes.
     *
     * @param contrat Le contrat de leasing pour lequel les échéances doivent être générées.
     * @throws ValidationException si les données du contrat sont insuffisantes pour le calcul.
     */
    @Transactional // L'ensemble de l'opération (création) est atomique
    public void generateEcheances(ContratLeasing contrat) {
        log.info("Début de la génération des échéances pour le contrat ID : {}", contrat.getId());
        _generateEcheancesInternal(contrat);
        log.info("Génération des échéances terminée avec succès pour le contrat ID : {}", contrat.getId());
    }


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

        // 2. Vérifier s'il existe des échéances PAYE pour ce contrat
        // Si oui, la régénération complète est risquée et potentiellement incorrecte.
        boolean hasPaidEcheances =this.hasPaidEcheances(contratId);

        if (hasPaidEcheances) {
            log.error("Tentative de régénération d'échéances pour le contrat ID : {} avec des échéances déjà payées", contratId);
            throw new ValidationException("Impossible de régénérer les échéances pour le contrat " + contratId +
                    " car il contient des échéances déjà payées  . c-à-d qu on a déja commencer l ammortisement pour cette contract Veuillez contacter l'administrateur.");
        }

        log.debug("Suppression des anciennes échéances pour le contrat ID : {}", contratId);
        echeanceLeasingRepository.deleteByContratLeasing(contrat);
        log.debug("Génération des nouvelles échéances pour le contrat ID : {}", contratId);
        //  Régénérer les nouvelles échéances en utilisant la logique existante
        generateEcheances(contrat);
        log.info("Régénération des échéances terminée avec succès pour le contrat ID : {}", contratId);

    }

    /**
     * Méthode interne pour le calcul et la persistance des échéances.
     * Elle ne gère pas la suppression d'anciennes échéances.
     * Appelée uniquement par `generateEcheances`.
     *
     * @param contrat Le contrat de leasing
     */
    private void _generateEcheancesInternal(ContratLeasing contrat) {

        log.debug("Début du calcul interne des échéances pour le contrat ID : {}", contrat.getId());

        List<EcheanceLeasing> echeances = new ArrayList<>();
        // La première échéance est à la date de début du contrat, donc initialiser à (date de début - 1 mois)
        // pour que le premier .plus(1, ChronoUnit.MONTHS) donne la date de début.
        OffsetDateTime dateEcheanceCourante = contrat.getDateDebut().minusMonths(1);
        int numeroEcheanceGlobal = 0; // Compteur pour le numéro de l'échéance à travers toutes les phases

        // Assurez-vous que les phases sont triées par numeroPhase pour un ordre correct
        List<ContratLeasingPhase> sortedPhases = contrat.getPhases().stream()
                .sorted((p1, p2) -> Integer.compare(p1.getNumeroPhase(), p2.getNumeroPhase()))
                .toList();

        log.debug("Nombre de phases à traiter : {}", sortedPhases.size());

        // Génération des échéances de loyer
        for (ContratLeasingPhase phase : sortedPhases) {
            log.debug("Traitement de la phase {} - Durée : {} mois, Mensualité TTC : {}", phase.getNumeroPhase(), phase.getDureeMois(), phase.getMensualiteTTC());

            BigDecimal mensualiteTTCDuMois = phase.getMensualiteTTC().setScale(SCALE, RoundingMode.HALF_UP);

            for (int i = 0; i < phase.getDureeMois(); i++) {
                numeroEcheanceGlobal++;
                dateEcheanceCourante = dateEcheanceCourante.plusMonths(1); // Incrémente la date de l'échéance

                // Check if the echeance's date is before or equal to the cut-off date
                StatutEcheanceLeasing statut =StatutEcheanceLeasing.PREVU;

                EcheanceLeasing echeance = new EcheanceLeasing(
                        contrat,
                        numeroEcheanceGlobal,
                        dateEcheanceCourante,
                        mensualiteTTCDuMois, // Le montant est directement la mensualité TTC de la phase
                        statut
                );
                echeances.add(echeance);
            }
        }
        log.debug("Nombre d'échéances de loyer générées : {}", echeances.size());

        // Ajout de l'échéance dédiée pour la valeur résiduelle

        numeroEcheanceGlobal++; // Le numéro de l'échéance sera (période en mois + 1)
        dateEcheanceCourante = dateEcheanceCourante.plusMonths(1); // Un mois après la dernière échéance de loyer

        StatutEcheanceLeasing statut =  StatutEcheanceLeasing.PREVU;

        EcheanceLeasing echeanceValeurResiduelle = new EcheanceLeasing(
                    contrat,
                    numeroEcheanceGlobal,
                    dateEcheanceCourante,
                    contrat.getValeurResiduelle().setScale(SCALE, RoundingMode.HALF_UP),
                    statut
            );

        echeances.add(echeanceValeurResiduelle);

        log.debug("Échéance de valeur résiduelle ajoutée. Total d'échéances à sauvegarder : {}", echeances.size());
        echeanceLeasingRepository.saveAll(echeances);
        log.debug("Toutes les échéances ont été sauvegardées en base de données");

    }


    public TableauAmortissementDTO getTableauAmortissementForContrat(Long contratId, EcheanceLeasingSearchDTO searchDTO, Pageable pageable) {

        log.info("Récupération du tableau d'amortissement pour le contrat ID : {}", contratId);
        log.debug("Critères de recherche : {}, Page : {}, Taille : {}", searchDTO, pageable.getPageNumber(), pageable.getPageSize());

        ContratLeasing contrat = contratLeasingRepository.findById(contratId)
                .orElseThrow(() -> {
                    log.error("Contrat de leasing non trouvé avec l'ID : {}", contratId);
                    return new ResourceNotFoundException("ContratLeasing with ID " + contratId + " not found.");
                });
        // Utilise la nouvelle méthode buildSpecification pour une gestion unifiée des filtres.
        Specification<EcheanceLeasing> spec = EcheanceLeasingSpecification.buildSpecification(searchDTO)
                .and(EcheanceLeasingSpecification.byContratId(contratId));

        // Fetch a PAGE of Echeances based on search criteria and Pageable
        Page<EcheanceLeasing> echeancesPage = echeanceLeasingRepository.findAll(spec, pageable);

        // Map the Page of entities to a Page of DTOs
        Page<EcheanceLeasingResponseDTO> echeancesDtoPage = echeancesPage.map(echeanceLeasingMapper::toDto);

        log.debug("Nombre d'échéances dans la page : {}", echeancesPage.getNumberOfElements());

        // Calculate totals based on ALL échéances for the contract, regardless of the current page.
        // This ensures the summary totals are always accurate for the entire contract.
        // NB : la VR est considere aussi comme échéance
        List<EcheanceLeasing> allEcheancesForTotals = echeanceLeasingRepository.findByContratLeasing(contrat);

        // totalIndemniteAPayer should be the sum of ALL échéances (loyers + valeur résiduelle).
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

    // we only change the status , no banque operation are involved
    @Transactional
    public EcheanceLeasingResponseDTO toggleStatus(Long echeanceId) {
        log.info("Début de la modification du statut de l'échéance ID : {}", echeanceId);

        EcheanceLeasing echeance = echeanceLeasingRepository.findById(echeanceId)
                .orElseThrow(() -> {
                    log.error("Échéance de leasing non trouvée avec l'ID : {}", echeanceId);
                    return new ResourceNotFoundException("EcheanceLeasing with ID " + echeanceId + " not found.");
                });
        StatutEcheanceLeasing currentStatus = echeance.getStatutPaiement();
        StatutEcheanceLeasing newStatus = currentStatus == StatutEcheanceLeasing.PAYE ? StatutEcheanceLeasing.PREVU : StatutEcheanceLeasing.PAYE;

        log.debug("Changement du statut de {} à {} pour l'échéance ID : {}", currentStatus, newStatus, echeanceId);
        echeance.setStatutPaiement(newStatus);
        echeance = this.echeanceLeasingRepository.save(echeance);
        log.info("Statut de l'échéance ID : {} modifié avec succès", echeanceId);

        return echeanceLeasingMapper.toDto(echeance);
    }

    @Transactional(readOnly = true)
    public EncoursLeasingResponseDTO getEncoursLeasingForBanque(EncoursLeasingRequestDTO requestDTO, Pageable pageable) {
        log.info("Récupération des encours de leasing pour la banque ID : {}", requestDTO.getBanqueId());
        log.debug("Paramètres de requête : {}, Page : {}, Taille : {}", requestDTO, pageable.getPageNumber(), pageable.getPageSize());

        // 1. Validate the bank's existence and retrieve the entity
        Banque banque = banqueRepository.findById(requestDTO.getBanqueId())
                .orElseThrow(() -> {
                    log.error("Banque non trouvée avec l'ID : {}", requestDTO.getBanqueId());
                    return new ResourceNotFoundException("Banque with ID " + requestDTO.getBanqueId() + " not found.");
                });

        log.debug("Banque trouvée : {}", banque.getNom());

        // 2. Start building the specification with the mandatory filters
        Specification<EcheanceLeasing> spec = EcheanceLeasingSpecification.byContratBanqueId(requestDTO.getBanqueId())
                .and(EcheanceLeasingSpecification.outstandingUntil());

        // 3. Add the optional filter for the leasing agency if it exists
        if (requestDTO.getOrganismeLeasingId() != null) {
            log.debug("Filtre par organisme de leasing ID : {}", requestDTO.getOrganismeLeasingId());
            spec = spec.and(EcheanceLeasingSpecification.byContratOrganismeLeasingId(requestDTO.getOrganismeLeasingId()));
        }

        if (requestDTO.getNumeroContrat() != null && !requestDTO.getNumeroContrat().isEmpty()) {
            log.debug("Filtre par numéro de contrat : {}", requestDTO.getNumeroContrat());
            spec = spec.and(EcheanceLeasingSpecification.byContratNumeroContrat(requestDTO.getNumeroContrat()));
        }

        if (requestDTO.getMontant()!=null ){
            log.debug("Filtre par montant : {}", requestDTO.getMontant());
            spec=spec.and(EcheanceLeasingSpecification.byMontant(requestDTO.getMontant()));
        }

        // 4. Fetch the paginated list of outstanding payments
        Page<EcheanceLeasing> encoursEcheancesPage = echeanceLeasingRepository.findAll(spec, pageable);

        // 5. Map the Page of entities to a Page of DTOs
        Page<EcheanceLeasingResponseDTO> echeancesDtoPage = encoursEcheancesPage.map(echeanceLeasingMapper::toDto);

        log.debug("Nombre d'échéances en encours dans la page : {}", encoursEcheancesPage.getNumberOfElements());

        // 6. Calculate totals based on ALL outstanding payments for the summary
        List<EcheanceLeasing> allOutstandingEcheances = echeanceLeasingRepository.findAll(spec);

        // ... (calculation logic for totalEncours

        BigDecimal totalEncours = allOutstandingEcheances.stream()
                .map(EcheanceLeasing::getMontantTTC)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(SCALE, RoundingMode.HALF_UP);

        log.info("Encours total calculé pour la banque {} : {}", banque.getNom(), totalEncours);

        // 7. Return the combined DTO
        return new EncoursLeasingResponseDTO(
                banque.getId(),
                banque.getNom(),
                totalEncours,
                echeancesDtoPage
        );
    }


    /**
     * Vérifie si au moins une échéance d'un contrat a déjà été payée.
     * Cette méthode est utilisée pour valider la suppression du contrat.
     * @param contratId L'ID du contrat.
     * @return true si au moins une échéance est PAYE, false sinon.
     */
    @Transactional(readOnly = true)
    public boolean hasPaidEcheances(Long contratId) {
        log.debug("Vérification de l'existence d'échéances payées pour le contrat ID : {}", contratId);

        boolean hasPaid = echeanceLeasingRepository.existsByContratLeasingIdAndStatutPaiement(contratId, StatutEcheanceLeasing.PAYE);

        log.debug("Le contrat ID : {} {} d'échéances payées", contratId, hasPaid ? "possède" : "ne possède pas");

        return hasPaid;
    }


}
