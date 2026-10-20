package staport.finance.gBanque.leasing.echeancesLeasing.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.dto.AvanceLeasingResponseDTO;
import staport.finance.gBanque.leasing.echeancesLeasing.dto.AvanceMoyenPaiementRequestDTO;
import staport.finance.gBanque.leasing.echeancesLeasing.dto.MoyenPaiementAvanceDTO;
import staport.finance.gBanque.leasing.echeancesLeasing.entities.EcheanceLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.enums.StatutEcheanceLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.repositories.EcheanceLeasingRepository;
import staport.finance.gBanque.leasing.echeancesLeasing.services.interfaces.IAvanceLeasingService;
import staport.finance.gBanque.moyensDePaiements.lecture.MoyenPaiementReader;
import staport.finance.gBanque.moyensDePaiements.lecture.MoyenPaiementResume;
import staport.finance.gBanque.moyensDePaiements.lecture.TypeMoyenPaiement;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.transactions.service.ITransactionService;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * Avance d'un contrat de leasing : rattachement du chèque, de la LCN ou du virement qui la règle.
 * <p>
 * Une fois rattachée, l'avance (échéance n°1) est représentée par son moyen de paiement : son statut est lu en direct
 * sur celui-ci (aucune copie à resynchroniser) et elle n'apparaît plus dans les encours de leasing — le chèque, la LCN
 * ou le virement y figure à sa place, de sorte qu'elle n'est jamais comptée deux fois. La seule transaction bancaire
 * est celle du moyen de paiement ; le libellé et l'observation de celle-ci (et du moyen de paiement) portent la mention
 * « avance, échéance n°1 du contrat N° x ».
 */
@Service
public class AvanceLeasingService implements IAvanceLeasingService {

    private static final Logger log = LoggerFactory.getLogger(AvanceLeasingService.class);
    private static final int NUMERO_ECHEANCE_AVANCE = 1;
    /** Longueur maximale du libellé et de l'observation (colonnes varchar(255)). */
    private static final int LONGUEUR_MAX = 255;

    /** Sources de transaction produites par la validation de chaque type de moyen de paiement (décaissement). */
    private static final Map<TypeMoyenPaiement, Set<TransactionSource>> SOURCES_TRANSACTION = new EnumMap<>(Map.of(
            TypeMoyenPaiement.CHEQUE, Set.of(TransactionSource.CHEQUE),
            TypeMoyenPaiement.LCN, Set.of(TransactionSource.LCN_DECAISSEMENT, TransactionSource.LCN),
            TypeMoyenPaiement.VIREMENT, Set.of(TransactionSource.Virement)));

    private static final Map<TypeMoyenPaiement, String> LIBELLE_TYPE = new EnumMap<>(Map.of(
            TypeMoyenPaiement.CHEQUE, "chèque",
            TypeMoyenPaiement.LCN, "LCN",
            TypeMoyenPaiement.VIREMENT, "virement"));

    private final EcheanceLeasingRepository echeanceLeasingRepository;
    private final ITransactionService transactionService;
    private final Map<TypeMoyenPaiement, MoyenPaiementReader> readers = new EnumMap<>(TypeMoyenPaiement.class);

    public AvanceLeasingService(EcheanceLeasingRepository echeanceLeasingRepository,
                                ITransactionService transactionService,
                                List<MoyenPaiementReader> moyenPaiementReaders) {
        this.echeanceLeasingRepository = echeanceLeasingRepository;
        this.transactionService = transactionService;
        moyenPaiementReaders.forEach(reader -> readers.put(reader.type(), reader));
    }

    @Override
    @Transactional(readOnly = true)
    public AvanceLeasingResponseDTO getAvance(Long contratId) {
        return toDto(findAvanceOrThrow(contratId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MoyenPaiementAvanceDTO> getMoyensPaiementDisponibles(TypeMoyenPaiement type, String recherche,
                                                                     Long contratId, Pageable pageable) {
        List<Long> idsExclus = echeanceLeasingRepository.findMoyenPaiementIdsRattaches(type, contratId);
        return readerOf(type).findDecaissementsNonAnnules(recherche, idsExclus, pageable)
                .map(AvanceLeasingService::toMoyenDto);
    }

    @Override
    @Transactional(readOnly = true)
    public MoyenPaiementAvanceDTO resoudreMoyen(AvanceMoyenPaiementRequestDTO dto) {
        MoyenPaiementResume moyen = readerOf(dto.type()).findById(dto.moyenPaiementId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Moyen de paiement introuvable (" + dto.type() + " n°" + dto.moyenPaiementId() + ")."));

        if (!moyen.decaissement()) {
            throw new ValidationException("Seul un décaissement peut régler l'avance d'un contrat de leasing.");
        }
        if (moyen.statut() == StatutMoyenPaiement.ANNULE) {
            throw new ValidationException("Ce moyen de paiement est annulé : il ne peut pas régler l'avance.");
        }
        if (echeanceLeasingRepository.existsByMoyenPaiementTypeAndMoyenPaiementId(dto.type(), dto.moyenPaiementId())) {
            throw new ValidationException("Ce moyen de paiement règle déjà l'avance d'un contrat de leasing.");
        }
        return toMoyenDto(moyen);
    }

    @Override
    @Transactional
    public void rattacher(Long contratId, AvanceMoyenPaiementRequestDTO dto) {
        EcheanceLeasing avance = findAvanceOrThrow(contratId);
        MoyenPaiementResume moyen = readerOf(dto.type()).findById(dto.moyenPaiementId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Moyen de paiement introuvable (" + dto.type() + " n°" + dto.moyenPaiementId() + ")."));

        avance.rattacherMoyenPaiement(dto.type(), dto.moyenPaiementId());
        echeanceLeasingRepository.saveAndFlush(avance);
        modifierMention(avance.getContratLeasing(), moyen, true);
        log.info("Avance du contrat ID : {} rattachée à {} n°{}", contratId, dto.type(), dto.moyenPaiementId());
    }

    @Override
    @Transactional
    public void liberer(ContratLeasing contrat) {
        Optional<EcheanceLeasing> trouvee = echeanceLeasingRepository
                .findByContratLeasingIdAndNumeroEcheance(contrat.getId(), NUMERO_ECHEANCE_AVANCE);
        if (trouvee.isEmpty() || !trouvee.get().isRattacheAMoyenPaiement()) {
            return;
        }
        EcheanceLeasing avance = trouvee.get();
        if (avance.getStatutPaiement() == StatutEcheanceLeasing.PAYE) {
            throw new ValidationException("L'avance du contrat est déjà payée : son moyen de paiement ne peut plus être retiré.");
        }
        readerOf(avance.getMoyenPaiementType()).findById(avance.getMoyenPaiementId())
                .ifPresent(moyen -> modifierMention(contrat, moyen, false));
        avance.detacherMoyenPaiement();
        echeanceLeasingRepository.saveAndFlush(avance);
        log.info("Moyen de paiement détaché de l'avance du contrat ID : {}", contrat.getId());
    }

    // -------------------------------------------------------------------------
    // Méthodes privées
    // -------------------------------------------------------------------------

    /**
     * Ajoute ou retire la mention d'avance sur la transaction du moyen de paiement (s'il est déjà réglé) puis sur le
     * moyen de paiement lui-même — la transaction d'abord : sa propagation réécrit les textes du moyen de paiement.
     * Un moyen de paiement encore en cours porte la mention dès maintenant : elle se retrouve sur la transaction qu'il
     * créera à sa validation.
     */
    private void modifierMention(ContratLeasing contrat, MoyenPaiementResume moyen, boolean ajouter) {
        String suffixeLibelle = " | Avance — échéance n°1 du contrat N° " + contrat.getNumeroContrat();
        String suffixeObservation = " | Avance réglée via " + LIBELLE_TYPE.get(moyen.type())
                + (moyen.numero() != null && !moyen.numero().isBlank() ? " n° " + moyen.numero() : "")
                + " — échéance n°1 du contrat N° " + contrat.getNumeroContrat();

        UnaryOperator<String> libelle = ajouter ? s -> ajouterSuffixe(s, suffixeLibelle) : s -> retirerSuffixe(s, suffixeLibelle);
        UnaryOperator<String> observation = ajouter ? s -> ajouterSuffixe(s, suffixeObservation) : s -> retirerSuffixe(s, suffixeObservation);

        transactionService.modifierTextes(SOURCES_TRANSACTION.get(moyen.type()), moyen.id(), libelle, observation);
        readerOf(moyen.type()).modifierTextes(moyen.id(), libelle, observation);
    }

    private static String ajouterSuffixe(String texte, String suffixe) {
        if (texte == null || texte.isBlank()) {
            return suffixe.substring(3);
        }
        if (texte.contains(suffixe)) {
            return texte;
        }
        String base = texte.length() + suffixe.length() > LONGUEUR_MAX ? texte.substring(0, LONGUEUR_MAX - suffixe.length()) : texte;
        return base + suffixe;
    }

    private static String retirerSuffixe(String texte, String suffixe) {
        if (texte == null) {
            return null;
        }
        // Sans texte d'origine, la mention avait été écrite sans son séparateur initial
        return texte.replace(suffixe, "").replace(suffixe.substring(3), "");
    }

    private EcheanceLeasing findAvanceOrThrow(Long contratId) {
        EcheanceLeasing echeance = echeanceLeasingRepository
                .findByContratLeasingIdAndNumeroEcheance(contratId, NUMERO_ECHEANCE_AVANCE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucune échéance trouvée pour le contrat de leasing ID : " + contratId));
        if (!echeance.getContratLeasing().hasAvance()) {
            throw new ValidationException("Ce contrat de leasing n'a pas d'avance.");
        }
        return echeance;
    }

    private MoyenPaiementReader readerOf(TypeMoyenPaiement type) {
        MoyenPaiementReader reader = readers.get(type);
        if (reader == null) {
            throw new ValidationException("Type de moyen de paiement non pris en charge : " + type);
        }
        return reader;
    }

    private AvanceLeasingResponseDTO toDto(EcheanceLeasing avance) {
        MoyenPaiementAvanceDTO moyen = avance.isRattacheAMoyenPaiement()
                ? readerOf(avance.getMoyenPaiementType()).findById(avance.getMoyenPaiementId())
                        .map(AvanceLeasingService::toMoyenDto).orElse(null)
                : null;
        // Statut lu sur le moyen de paiement (et non sur l'entité, dont le statut calculé date du chargement)
        StatutEcheanceLeasing statut = !avance.isRattacheAMoyenPaiement()
                ? avance.getStatutPaiement()
                : (moyen != null && moyen.statut() == StatutMoyenPaiement.REGLE
                        ? StatutEcheanceLeasing.PAYE
                        : StatutEcheanceLeasing.PREVU);
        return new AvanceLeasingResponseDTO(avance.getId(), avance.getContratLeasing().getId(),
                avance.getMontantTTC(), avance.getDateEcheance(), statut, moyen);
    }

    private static MoyenPaiementAvanceDTO toMoyenDto(MoyenPaiementResume m) {
        return new MoyenPaiementAvanceDTO(m.type(), m.id(), m.numero(), m.montant(), m.statut(), m.dateEmission(),
                m.dateOperation(), m.dateValeur(), m.beneficiaire(), m.nomBanque());
    }
}
