package staport.finance.gBanque.validation.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Agios.entities.Agios;
import staport.finance.gBanque.Agios.repository.AgiosRepository;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
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
import staport.finance.gBanque.factoring.entities.Factoring;
import staport.finance.gBanque.factoring.repository.FactoringRepository;
import staport.finance.gBanque.fraisBancaires.entities.FraisBancaire;
import staport.finance.gBanque.fraisBancaires.repositories.FraisBancaireRepository;
import staport.finance.gBanque.impot_taxes.entities.ImpotEtTaxe;
import staport.finance.gBanque.impot_taxes.repositories.ImpotEtTaxeRepository;
import staport.finance.gBanque.interets.entities.Interet;
import staport.finance.gBanque.interets.repository.InteretRepository;
import staport.finance.gBanque.leasing.echeancesLeasing.entities.EcheanceLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.enums.StatutEcheanceLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.repositories.EcheanceLeasingRepository;
import staport.finance.gBanque.moyensDePaiements.cheques.entities.Cheque;
import staport.finance.gBanque.moyensDePaiements.cheques.repositories.ChequeRepository;
import staport.finance.gBanque.moyensDePaiements.lcn.entities.Lcn;
import staport.finance.gBanque.moyensDePaiements.lcn.repositories.LcnRepository;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.entities.MiseADisposition;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.repositories.MiseADispositionRepository;
import staport.finance.gBanque.moyensDePaiements.virements.entities.Virement;
import staport.finance.gBanque.moyensDePaiements.virements.repositories.VirementRepository;
import staport.finance.gBanque.prefinancement.entities.Prefinancement;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prefinancement.repository.PrefinancementRepository;
import staport.finance.gBanque.prelevements.entitie.Prelevement;
import staport.finance.gBanque.prelevements.repository.PrelevementRepository;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.transactions.repositories.TransactionRepository;

import java.math.BigDecimal;

/**
 * Service simple pour annuler une transaction en inversant ses effets
 */
@Service
public class ReverseTransactionService {

    private static final Logger log = LoggerFactory.getLogger(ReverseTransactionService.class);

    private final TransactionRepository transactionRepository;
    private final BanqueRepository banqueRepository;

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
    private final AgiosRepository agiosRepository;
    private final PrelevementRepository prelevementRepository;
    private final InteretRepository interetRepository;
    private final FraisBancaireRepository fraisBancaireRepository;
    private final FactoringRepository factoringRepository;
    private final ImpotEtTaxeRepository impotEtTaxeRepository;


    public ReverseTransactionService(
            TransactionRepository transactionRepository,
            BanqueRepository banqueRepository,
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
            AgiosRepository agiosRepository,
            PrelevementRepository prelevementRepository,
            InteretRepository interetRepository,
            FraisBancaireRepository fraisBancaireRepository,
            FactoringRepository factoringRepository, ImpotEtTaxeRepository impotEtTaxeRepository) {
        this.transactionRepository = transactionRepository;
        this.banqueRepository = banqueRepository;
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
        this.agiosRepository = agiosRepository;
        this.prelevementRepository = prelevementRepository;
        this.interetRepository = interetRepository;
        this.fraisBancaireRepository = fraisBancaireRepository;
        this.factoringRepository = factoringRepository;
        this.impotEtTaxeRepository = impotEtTaxeRepository;
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

        // 2. Inverser le solde de la banque selon le cas
        inverseSoldeBanque(transaction);

        // 3. Remettre l'entité source à son état initial
        resetEntiteSource(transaction);

        // 4. Supprimer la transaction
        transactionRepository.delete(transaction);

        log.info("Transaction {} annulée et supprimée avec succès", transactionId);
    }

    /**
     * Inverse le solde de la banque
     * - Si la transaction était un DEBIT, on AJOUTE le montant (recrédite)
     * - Si la transaction était un CREDIT, on SOUSTRAIT le montant (redébite)
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    private void inverseSoldeBanque(Transaction transaction) {
        Banque banque = transaction.getBanqueDePrelevement();
        BigDecimal debit = transaction.getDebit();
        BigDecimal credit = transaction.getCredit();

        log.debug("Inversion solde banque {} - Débit original: {}, Crédit original: {}",
                banque.getNom(), debit, credit);

        // Si c'était un débit, on recrédite
        if (debit != null && debit.compareTo(BigDecimal.ZERO) > 0) {
            banque.setSoldeActuel(banque.getSoldeActuel().add(debit));
            log.debug("Recrédit de {} à la banque", debit);
        }

        // Si c'était un crédit, on redébite
        if (credit != null && credit.compareTo(BigDecimal.ZERO) > 0) {
            banque.setSoldeActuel(banque.getSoldeActuel().subtract(credit));
            log.debug("Redébit de {} à la banque", credit);
        }

        banqueRepository.save(banque);
        log.debug("Nouveau solde banque: {}", banque.getSoldeActuel());
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
            case LCN:
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
                resetAgios(referentId);
                break;
            case PRELEVEMENT:
                resetPrelevement(referentId);
                break;
            case INTERETS:
                resetInteret(referentId);
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

        cheque.setActive(true);
        cheque.setDateOperation(null);
        cheque.setDateValeur(null);
        chequeRepository.save(cheque);
        log.debug("Chèque {} remis à actif", chequeId);
    }

    private void resetLcn(Long lcnId) throws Exception {
        Lcn lcn = lcnRepository.findById(lcnId)
                .orElseThrow(() -> new ResourceNotFoundException("LCN introuvable: " + lcnId));

        lcn.setActive(true);
        lcn.setDateOperation(null);
        lcn.setDateValeur(null);
        lcnRepository.save(lcn);
        log.debug("LCN {} remis à actif", lcnId);
    }

    private void resetVirement(Long virementId) throws Exception {
        Virement virement = virementRepository.findById(virementId)
                .orElseThrow(() -> new ResourceNotFoundException("Virement introuvable: " + virementId));

        virement.setActive(true);
        virement.setDateOperation(null);
        virement.setDateValeur(null);
        virementRepository.save(virement);
        log.debug("Virement {} remis à actif", virementId);
    }

    private void resetMiseADisposition(Long miseADispoId) throws Exception {
        MiseADisposition miseADispo = miseADispositionRepository.findById(miseADispoId)
                .orElseThrow(() -> new ResourceNotFoundException("Mise à disposition introuvable: " + miseADispoId));

        miseADispo.setActive(true);
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

    private void resetAgios(Long agiosId) throws Exception {
        Agios agios = agiosRepository.findById(agiosId)
                .orElseThrow(() -> new ResourceNotFoundException("Agios introuvable: " + agiosId));

        agios.setStatus(Status.PREVU);
        agios.setDateOperation(null);
        agios.setDateValeur(null);
        agiosRepository.save(agios);
        log.debug("Agios {} remis à NON_PAYE", agiosId);
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

    private void resetInteret(Long interetId) throws Exception {
        Interet interet = interetRepository.findById(interetId)
                .orElseThrow(() -> new ResourceNotFoundException("Intérêt introuvable: " + interetId));

        interet.setStatus(Status.PREVU);
        interet.setDateOperation(null);
        interet.setDateValeur(null);
        interetRepository.save(interet);
        log.debug("Intérêt {} remis à NON_PAYE", interetId);
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
}