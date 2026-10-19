package staport.finance.gBanque.leasing.echeancesLeasing.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.audit.AuditOperation;
import staport.finance.gBanque.audit.AuditableAction;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
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

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Rattachement du moyen de paiement qui règle l'avance d'un contrat de leasing.
 * <p>
 * Une fois rattachée, l'avance (échéance n°1) est représentée par son moyen de paiement : son statut est lu en direct
 * sur celui-ci (aucune copie à resynchroniser) et elle n'apparaît plus dans les encours de leasing — le chèque, la LCN
 * ou le virement y figure à sa place, de sorte qu'elle n'est jamais comptée deux fois.
 */
@Service
public class AvanceLeasingService implements IAvanceLeasingService {

    private static final Logger log = LoggerFactory.getLogger(AvanceLeasingService.class);
    private static final int NUMERO_ECHEANCE_AVANCE = 1;

    private final EcheanceLeasingRepository echeanceLeasingRepository;
    private final Map<TypeMoyenPaiement, MoyenPaiementReader> readers = new EnumMap<>(TypeMoyenPaiement.class);

    public AvanceLeasingService(EcheanceLeasingRepository echeanceLeasingRepository,
                                List<MoyenPaiementReader> moyenPaiementReaders) {
        this.echeanceLeasingRepository = echeanceLeasingRepository;
        moyenPaiementReaders.forEach(reader -> readers.put(reader.type(), reader));
    }

    @Override
    @Transactional(readOnly = true)
    public AvanceLeasingResponseDTO getAvance(Long contratId) {
        return toDto(findAvanceOrThrow(contratId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MoyenPaiementAvanceDTO> getMoyensPaiementDisponibles(Long contratId) {
        EcheanceLeasing avance = findAvanceOrThrow(contratId);
        return readers.values().stream()
                .flatMap(reader -> reader.findDecaissementsNonAnnulesDeMontant(avance.getMontantTTC()).stream())
                .filter(m -> !echeanceLeasingRepository.existsByMoyenPaiementTypeAndMoyenPaiementId(m.type(), m.id()))
                .map(AvanceLeasingService::toMoyenDto)
                .toList();
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = EcheanceLeasing.class, idAccessor = "echeanceId")
    public AvanceLeasingResponseDTO rattacherMoyenPaiement(Long contratId, AvanceMoyenPaiementRequestDTO dto) {
        EcheanceLeasing avance = findAvanceOrThrow(contratId);

        if (avance.isRattacheAMoyenPaiement()) {
            throw new ValidationException(
                    "L'avance de ce contrat est déjà rattachée à un moyen de paiement. Détachez-le d'abord.");
        }
        if (avance.getContratLeasing().isCompleted()) {
            throw new ValidationException("Le contrat est clôturé : son avance ne peut plus être modifiée.");
        }

        MoyenPaiementResume moyen = readerOf(dto.type()).findById(dto.moyenPaiementId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Moyen de paiement introuvable (" + dto.type() + " n°" + dto.moyenPaiementId() + ")."));

        if (!moyen.decaissement()) {
            throw new ValidationException("Seul un décaissement peut régler l'avance d'un contrat de leasing.");
        }
        if (moyen.statut() == StatutMoyenPaiement.ANNULE) {
            throw new ValidationException("Ce moyen de paiement est annulé : il ne peut pas régler l'avance.");
        }
        if (moyen.montant().compareTo(avance.getMontantTTC()) != 0) {
            throw new ValidationException("Le montant du moyen de paiement (" + moyen.montant()
                    + ") doit être égal à l'avance du contrat (" + avance.getMontantTTC() + ").");
        }
        if (echeanceLeasingRepository.existsByMoyenPaiementTypeAndMoyenPaiementId(dto.type(), dto.moyenPaiementId())) {
            throw new ValidationException("Ce moyen de paiement règle déjà l'avance d'un contrat de leasing.");
        }

        avance.rattacherMoyenPaiement(dto.type(), dto.moyenPaiementId());
        echeanceLeasingRepository.saveAndFlush(avance);
        log.info("Avance du contrat ID : {} rattachée à {} n°{}", contratId, dto.type(), dto.moyenPaiementId());
        return toDto(avance);
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = EcheanceLeasing.class, idAccessor = "echeanceId")
    public AvanceLeasingResponseDTO detacherMoyenPaiement(Long contratId) {
        EcheanceLeasing avance = findAvanceOrThrow(contratId);

        if (!avance.isRattacheAMoyenPaiement()) {
            throw new ValidationException("L'avance de ce contrat n'est rattachée à aucun moyen de paiement.");
        }
        if (avance.getStatutPaiement() == StatutEcheanceLeasing.PAYE) {
            throw new ValidationException(
                    "Le moyen de paiement est déjà réglé : l'avance est payée et ne peut plus être détachée.");
        }

        avance.detacherMoyenPaiement();
        echeanceLeasingRepository.saveAndFlush(avance);
        log.info("Moyen de paiement détaché de l'avance du contrat ID : {}", contratId);
        return toDto(avance);
    }

    // -------------------------------------------------------------------------
    // Méthodes privées
    // -------------------------------------------------------------------------

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
