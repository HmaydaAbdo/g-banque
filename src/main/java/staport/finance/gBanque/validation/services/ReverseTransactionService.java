package staport.finance.gBanque.validation.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Agios.service.interfaces.IAgiosValidationService;
import staport.finance.gBanque.cmt.echeances.entities.EcheanceCMT;
import staport.finance.gBanque.cmt.echeances.enums.StatutEcheanceCMT;
import staport.finance.gBanque.cmt.echeances.repositories.EcheanceCMTRepository;
import staport.finance.gBanque.commision.entities.Commission;
import staport.finance.gBanque.commision.repository.CommisionRepository;
import staport.finance.gBanque.decomptes.entities.Decompte;
import staport.finance.gBanque.decomptes.repositories.DecompteRepository;
import staport.finance.gBanque.droitConstate.entities.DroitConstate;
import staport.finance.gBanque.droitConstate.repositories.DroitConstateRepository;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.factoring.entities.Factoring;
import staport.finance.gBanque.factoring.repository.FactoringRepository;
import staport.finance.gBanque.fraisBancaires.entities.FraisBancaire;
import staport.finance.gBanque.fraisBancaires.repositories.FraisBancaireRepository;
import staport.finance.gBanque.impayees.entities.Impayee;
import staport.finance.gBanque.impayees.repository.ImpayeeRepository;
import staport.finance.gBanque.impot_taxes.entities.ImpotEtTaxe;
import staport.finance.gBanque.impot_taxes.repositories.ImpotEtTaxeRepository;
import staport.finance.gBanque.leasing.echeancesLeasing.entities.EcheanceLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.enums.StatutEcheanceLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.repositories.EcheanceLeasingRepository;
import staport.finance.gBanque.moyensDePaiements.cheques.entities.Cheque;
import staport.finance.gBanque.moyensDePaiements.cheques.repositories.ChequeRepository;
import staport.finance.gBanque.moyensDePaiements.lcn.entities.Lcn;
import staport.finance.gBanque.moyensDePaiements.lcn.repositories.LcnRepository;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.entities.MiseADisposition;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.repositories.MiseADispositionRepository;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;
import staport.finance.gBanque.moyensDePaiements.virementInternational.entities.VirementInternational;
import staport.finance.gBanque.moyensDePaiements.virementInternational.repositories.VirementInternationalRepository;
import staport.finance.gBanque.moyensDePaiements.virements.entities.Virement;
import staport.finance.gBanque.moyensDePaiements.virements.repositories.VirementRepository;
import staport.finance.gBanque.operationErrone.entities.OperationErrone;
import staport.finance.gBanque.operationErrone.repositories.OperationErroneRepository;
import staport.finance.gBanque.prefinancement.entities.Prefinancement;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prefinancement.repository.PrefinancementRepository;
import staport.finance.gBanque.prelevements.entitie.Prelevement;
import staport.finance.gBanque.prelevements.repository.PrelevementRepository;
import staport.finance.gBanque.restitution.entities.Restitution;
import staport.finance.gBanque.restitution.repository.RestitutionRepository;
import staport.finance.gBanque.rapprochement.enums.StatutRapprochement;
import staport.finance.gBanque.rapprochement.services.SoldeCascadeService;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.transactions.repositories.TransactionRepository;


/**
 * Service simple pour annuler une transaction en inversant ses effets
 */
@Service
public class ReverseTransactionService {

    private static final Logger log = LoggerFactory.getLogger(ReverseTransactionService.class);

    private final TransactionRepository transactionRepository;

    // Repositories pour les entités sources
    private final EcheanceLeasingRepository echeanceLeasingRepository;
    private final EcheanceCMTRepository echeanceCMTRepository;
    private final ChequeRepository chequeRepository;
    private final LcnRepository lcnRepository;
    private final VirementRepository virementRepository;
    private final MiseADispositionRepository miseADispositionRepository;
    private final PrefinancementRepository prefinancementRepository;
    private final DroitConstateRepository droitConstateRepository;
    private final DecompteRepository decompteRepository;
    private final CommisionRepository commisionRepository;
    private final IAgiosValidationService agiosValidationService;
    private final PrelevementRepository prelevementRepository;
    private final FraisBancaireRepository fraisBancaireRepository;
    private final FactoringRepository factoringRepository;
    private final ImpotEtTaxeRepository impotEtTaxeRepository;
    private final RestitutionRepository restitutionRepository;
    private final OperationErroneRepository operationErroneRepository;
    private final VirementInternationalRepository virementInternationalRepository;
    private final ImpayeeRepository impayeeRepository;
    private final SoldeCascadeService soldeCascadeService;

    public ReverseTransactionService(
            TransactionRepository transactionRepository,
            EcheanceLeasingRepository echeanceLeasingRepository,
            EcheanceCMTRepository echeanceCMTRepository,
            ChequeRepository chequeRepository,
            LcnRepository lcnRepository,
            VirementRepository virementRepository,
            MiseADispositionRepository miseADispositionRepository,
            PrefinancementRepository prefinancementRepository,
            DroitConstateRepository droitConstateRepository,
            DecompteRepository decompteRepository,
            CommisionRepository commisionRepository,
            IAgiosValidationService agiosValidationService,
            PrelevementRepository prelevementRepository,
            FraisBancaireRepository fraisBancaireRepository,
            FactoringRepository factoringRepository, ImpotEtTaxeRepository impotEtTaxeRepository, RestitutionRepository restitutionRepository, OperationErroneRepository operationErroneRepository, VirementInternationalRepository virementInternationalRepository, ImpayeeRepository impayeeRepository, SoldeCascadeService soldeCascadeService) {
        this.transactionRepository = transactionRepository;
        this.echeanceLeasingRepository = echeanceLeasingRepository;
        this.echeanceCMTRepository = echeanceCMTRepository;
        this.chequeRepository = chequeRepository;
        this.lcnRepository = lcnRepository;
        this.virementRepository = virementRepository;
        this.miseADispositionRepository = miseADispositionRepository;
        this.prefinancementRepository = prefinancementRepository;
        this.droitConstateRepository = droitConstateRepository;
        this.decompteRepository = decompteRepository;
        this.commisionRepository = commisionRepository;
        this.agiosValidationService = agiosValidationService;
        this.prelevementRepository = prelevementRepository;
        this.fraisBancaireRepository = fraisBancaireRepository;
        this.factoringRepository = factoringRepository;
        this.impotEtTaxeRepository = impotEtTaxeRepository;
        this.restitutionRepository = restitutionRepository;
        this.operationErroneRepository = operationErroneRepository;
        this.virementInternationalRepository = virementInternationalRepository;
        this.impayeeRepository = impayeeRepository;
        this.soldeCascadeService = soldeCascadeService;
    }

    /**
     * Annule une transaction en inversant ses effets
     *
     * @param transactionId ID de la transaction à annuler
     * @throws Exception si l'annulation échoue
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void annulerTransaction(Long transactionId) throws Exception {
        log.info("Début de l'annulation de la transaction ID: {}", transactionId);

        // 1. Récupérer la transaction
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction introuvable avec l'ID: " + transactionId));

        // Guard — rapprochement définitivement clôturé : aucune modification autorisée
        if (transaction.getRapprochement().getStatut() == StatutRapprochement.CLOTURE_DEFINITIVE) {
            throw new ValidationException(
                    "Impossible d'annuler la transaction " + transactionId
                    + " : le rapprochement est définitivement clôturé.");
        }

        // 2. Capture rapprochement info BEFORE deletion (for cascade)
        StatutRapprochement statutRapprochement = transaction.getRapprochement().getStatut();
        Long banqueId = transaction.getBanqueDePrelevement().getId();

        // 3. Remettre l'entité source à son état initial
        resetEntiteSource(transaction);

        // 4. Supprimer la transaction
        transactionRepository.delete(transaction);

        // 5. Si le rapprochement était CLOTURE_PROVISOIRE → recalculer la cascade des soldes
        if (statutRapprochement == StatutRapprochement.CLOTURE_PROVISOIRE) {
            soldeCascadeService.recalculerCascade(banqueId);
        }

        log.info("Transaction {} annulée et supprimée avec succès", transactionId);
    }



    /**
     * Remet l'entité source à son état initial selon le type de transaction
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    private void resetEntiteSource(Transaction transaction) throws Exception {
        Long referentId = transaction.getReferent();
        TransactionSource source = transaction.getTransactionSource();

        log.debug("Reset entité source: {} [ID: {}]", source, referentId);

        switch (source) {
            case LEASING:
                resetLeasing(referentId);
                break;
            case CMT:
                resetCMT(referentId);
                break;
            case CHEQUE:
                resetCheque(referentId);
                break;
            case LCN_DECAISSEMENT,LCN_ENCAISSEMENT,LCN_ESCOMPTE:
                resetLcn(referentId);
                break;
            case Virement:
                resetVirement(referentId);
                break;
            case MiseADisposition:
                resetMiseADisposition(referentId);
                break;
            case PREFINANCEMENT:
                resetPrefinancement(referentId);
                break;
            case DROIT_CONSTATE:
                resetDroitConstate(referentId);
                break;
            case DECOMPTES:
                resetDecompte(referentId);
                break;
            case COMMISSION:
                resetCommission(referentId);
                break;
            case AGIOS:
                agiosValidationService.resetAgios(referentId);
                break;
            case PRELEVEMENT:
                resetPrelevement(referentId);
                break;
            case FRAIS_BANCAIRE:
                resetFraisBancaire(referentId);
                break;
            case FACTORING:
                resetFactoring(referentId);
                break;
            case IMPOT_TAXE:
                resetImpotEtTaxe(referentId);
                break;
            case RESTITUTION:
                resetRestitution(referentId);
                break;
            case OPERATION_ERRONE:
                resetOperationErrone(referentId);
                break;
            case VIREMENT_INTERNATIONAL:
                resetVirementInternational(referentId);
                break;
            case IMPAYEE:
                resetImpayee(referentId);
                break;
            default:
                throw new Exception("Type de transaction non supporté pour l'annulation: " + source);
        }
    }

    // ========== Méthodes de reset pour chaque type d'entité ==========

    private void resetLeasing(Long echeanceId) throws Exception {
        EcheanceLeasing echeance = echeanceLeasingRepository.findById(echeanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Echeance leasing introuvable: " + echeanceId));

        echeance.setStatutPaiement(StatutEcheanceLeasing.PREVU);
        echeance.setDateValeur(null);
        echeance.setDateOperation(null);
        echeanceLeasingRepository.save(echeance);
        log.debug("Échéance leasing {} remise à NON_PAYE", echeanceId);
    }

    private void resetCMT(Long echeanceId) throws Exception {
        EcheanceCMT echeance = echeanceCMTRepository.findById(echeanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Echeance CMT introuvable: " + echeanceId));

        echeance.setStatutPaiement(StatutEcheanceCMT.PREVU);
        echeance.setDateValeur(null);
        echeance.setDateOperation(null);
        echeanceCMTRepository.save(echeance);
        log.debug("Échéance CMT {} remise à NON_PAYE", echeanceId);
    }

    private void resetCheque(Long chequeId) throws Exception {
        Cheque cheque = chequeRepository.findById(chequeId)
                .orElseThrow(() -> new ResourceNotFoundException("Cheque introuvable: " + chequeId));

        cheque.setStatut(StatutMoyenPaiement.ENCOURS);
        cheque.setDateOperation(null);
        cheque.setDateValeur(null);
        chequeRepository.save(cheque);
        log.debug("Chèque {} remis à actif", chequeId);
    }

    private void resetLcn(Long lcnId) throws Exception {
        Lcn lcn = lcnRepository.findById(lcnId)
                .orElseThrow(() -> new ResourceNotFoundException("LCN introuvable: " + lcnId));

        lcn.setStatut(StatutMoyenPaiement.ENCOURS);
        lcn.setDateOperation(null);
        lcn.setDateValeur(null);
        lcnRepository.save(lcn);
        log.debug("LCN {} remis à actif", lcnId);
    }

    private void resetVirement(Long virementId) throws Exception {
        Virement virement = virementRepository.findById(virementId)
                .orElseThrow(() -> new ResourceNotFoundException("Virement introuvable: " + virementId));

        virement.setStatut(StatutMoyenPaiement.ENCOURS);
        virement.setDateOperation(null);
        virement.setDateValeur(null);
        virementRepository.save(virement);
        log.debug("Virement {} remis à actif", virementId);
    }

    private void resetMiseADisposition(Long miseADispoId) throws Exception {
        MiseADisposition miseADispo = miseADispositionRepository.findById(miseADispoId)
                .orElseThrow(() -> new ResourceNotFoundException("Mise à disposition introuvable: " + miseADispoId));

        miseADispo.setStatut(StatutMoyenPaiement.ENCOURS);
        miseADispo.setDateOperation(null);
        miseADispo.setDateValeur(null);
        miseADispositionRepository.save(miseADispo);
        log.debug("Mise à disposition {} remise à actif", miseADispoId);
    }

    private void resetPrefinancement(Long prefinancementId) throws Exception {
        Prefinancement prefinancement = prefinancementRepository.findById(prefinancementId)
                .orElseThrow(() -> new ResourceNotFoundException("Préfinancement introuvable: " + prefinancementId));

        prefinancement.setStatus(Status.PREVU);
        prefinancement.setDateOperation(null);
        prefinancement.setDateValeur(null);
        prefinancementRepository.save(prefinancement);
        log.debug("Préfinancement {} remis à NON_PAYE", prefinancementId);
    }

    private void resetDroitConstate(Long droitConstateId) throws Exception {
        DroitConstate droitConstate = droitConstateRepository.findById(droitConstateId)
                .orElseThrow(() -> new ResourceNotFoundException("Droit constaté introuvable: " + droitConstateId));

        droitConstate.setStatus(Status.PREVU);
        droitConstate.setDateOperation(null);
        droitConstate.setDateValeur(null);
        droitConstateRepository.save(droitConstate);
        log.debug("Droit constaté {} remis à NON_PAYE", droitConstateId);
    }

    private void resetDecompte(Long decompteId) throws Exception {
        Decompte decompte = decompteRepository.findById(decompteId)
                .orElseThrow(() -> new ResourceNotFoundException("Décompte introuvable: " + decompteId));

        decompte.setStatus(Status.PREVU);
        decompte.setDateOperation(null);
        decompte.setDateValeur(null);
        decompteRepository.save(decompte);
        log.debug("Décompte {} remis à NON_PAYE", decompteId);
    }

    private void resetCommission(Long commissionId) throws Exception {
        Commission commission = commisionRepository.findById(commissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Commission introuvable: " + commissionId));

        commission.setStatus(Status.PREVU);
        commission.setDateOperation(null);
        commission.setDateValeur(null);
        commisionRepository.save(commission);
        log.debug("Commission {} remise à NON_PAYE", commissionId);
    }

    private void resetPrelevement(Long prelevementId) throws Exception {
        Prelevement prelevement = prelevementRepository.findById(prelevementId)
                .orElseThrow(() -> new ResourceNotFoundException("Prélèvement introuvable: " + prelevementId));

        prelevement.setStatus(Status.PREVU);
        prelevement.setDateOperation(null);
        prelevement.setDateValeur(null);
        prelevementRepository.save(prelevement);
        log.debug("Prélèvement {} remis à NON_PAYE", prelevementId);
    }


    private void resetFraisBancaire(Long fraisBancaireId) throws Exception {
        FraisBancaire fraisBancaire = fraisBancaireRepository.findById(fraisBancaireId)
                .orElseThrow(() -> new ResourceNotFoundException("Frais bancaire introuvable: " + fraisBancaireId));

        fraisBancaire.setStatus(Status.PREVU);
        fraisBancaire.setDateOperation(null);
        fraisBancaire.setDateValeur(null);
        fraisBancaireRepository.save(fraisBancaire);
        log.debug("Frais bancaire {} remis à NON_PAYE", fraisBancaireId);
    }

    private void resetFactoring(Long factoringId) throws Exception {
        Factoring factoring = factoringRepository.findById(factoringId)
                .orElseThrow(() -> new ResourceNotFoundException("Factoring introuvable: " + factoringId));

        factoring.setStatus(Status.PREVU);
        factoring.setDateOperation(null);
        factoring.setDateValeur(null);
        factoringRepository.save(factoring);
        log.debug("Factoring {} remis à NON_PAYE", factoringId);
    }

    private void resetImpotEtTaxe(Long impotEtTaxeId) throws Exception {
        ImpotEtTaxe impotEtTaxe = impotEtTaxeRepository.findById(impotEtTaxeId)
                .orElseThrow(() -> new ResourceNotFoundException("Impôt/Taxe introuvable: " + impotEtTaxeId));

        impotEtTaxe.setStatus(Status.PREVU);
        impotEtTaxe.setDateOperation(null);
        impotEtTaxe.setDateValeur(null);
        impotEtTaxeRepository.save(impotEtTaxe);
        log.debug("Impôt/Taxe {} remis(e) à PREVU", impotEtTaxeId);
    }

    private void resetRestitution(Long restitutionId) throws Exception {
        Restitution restitution = restitutionRepository.findById(restitutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Restitution introuvable: " + restitutionId));

        restitution.setStatus(Status.PREVU);
        restitution.setDateOperation(null);
        restitution.setDateValeur(null);
        restitutionRepository.save(restitution);
        log.debug("Restitution {} remise à PREVU", restitutionId);
    }

    private void resetImpayee(Long impayeeId) throws Exception {
        Impayee impayee = impayeeRepository.findById(impayeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Impayée introuvable: " + impayeeId));

        impayee.setStatus(Status.PREVU);
        impayee.setDateOperation(null);
        impayee.setDateValeur(null);
        impayeeRepository.save(impayee);
        log.debug("Impayée {} remise à PREVU", impayeeId);
    }

    private void resetOperationErrone(Long operationErroneId) throws Exception {
        OperationErrone operationErrone = operationErroneRepository.findById(operationErroneId)
                .orElseThrow(() -> new ResourceNotFoundException("Operation erroné introuvable: " + operationErroneId));

        operationErrone.setStatus(Status.PREVU);
        operationErrone.setDateOperation(null);
        operationErrone.setDateValeur(null);
        operationErroneRepository.save(operationErrone);
        log.debug("Operation erroné {} remis à PREVU", operationErroneId);
    }

    private void resetVirementInternational(Long virementInternationalId) throws Exception {
        VirementInternational virementInternational = virementInternationalRepository.findById(virementInternationalId)
                .orElseThrow(() -> new ResourceNotFoundException("Virement international introuvable: " + virementInternationalId));

        virementInternational.setStatut(StatutMoyenPaiement.ENCOURS);
        virementInternational.setDateOperation(null);
        virementInternational.setDateValeur(null);
        virementInternationalRepository.save(virementInternational);
        log.debug("Virement international {} remis à actif", virementInternationalId);
    }
}