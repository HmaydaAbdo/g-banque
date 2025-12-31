package staport.finance.gBanque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import staport.finance.gBanque.cmt.echeances.enums.StatutEcheanceCMT;
import staport.finance.gBanque.cmt.echeances.repositories.EcheanceCMTRepository;
import staport.finance.gBanque.leasing.echeancesLeasing.enums.StatutEcheanceLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.repositories.EcheanceLeasingRepository;
import staport.finance.gBanque.moyensDePaiements.cheques.repositories.ChequeRepository;
import staport.finance.gBanque.moyensDePaiements.lcn.repositories.LcnRepository;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.repositories.MiseADispositionRepository;
import staport.finance.gBanque.moyensDePaiements.virements.repositories.VirementRepository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@RestController
@RequestMapping("/2026")
public class Impayee {

    private static final Logger log = LoggerFactory.getLogger(Impayee.class);

    private final EcheanceCMTRepository cmtRepository;
    private final EcheanceLeasingRepository leasingRepository;
    private final VirementRepository virementRepository;
    private final ChequeRepository chequeRepository;
    private final MiseADispositionRepository miseADispositionRepository;
    private final LcnRepository lcnRepository;

    public Impayee(
            EcheanceCMTRepository cmtRepository,
            EcheanceLeasingRepository leasingRepository,
            VirementRepository virementRepository,
            ChequeRepository chequeRepository,
            MiseADispositionRepository miseADispositionRepository,
            LcnRepository lcnRepository) {
        this.cmtRepository = cmtRepository;
        this.leasingRepository = leasingRepository;
        this.virementRepository = virementRepository;
        this.chequeRepository = chequeRepository;
        this.miseADispositionRepository = miseADispositionRepository;
        this.lcnRepository = lcnRepository;
    }

    @PatchMapping("/process-automatic")
    public void processAutomaticPayments() {
        OffsetDateTime cutoffDate = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

        log.debug("start automatic paying of cmt");
        cmtRepository.findAll()
                .stream()
                .filter(e -> e.getDateEcheance().isBefore(cutoffDate))
                .forEach(e -> {
                    e.setStatutPaiement(StatutEcheanceCMT.PAYE);
                    cmtRepository.save(e);
                });

        log.debug("start automatic paying of leasing");
        leasingRepository.findAll()
                .stream()
                .filter(e -> e.getDateEcheance().isBefore(cutoffDate))
                .forEach(e -> {
                    e.setStatutPaiement(StatutEcheanceLeasing.PAYE);
                    leasingRepository.save(e);
                });

        log.debug("start automatic paying of virements");
        virementRepository.findAll()
                .stream()
                .filter(v -> v.getDateEmission().isBefore(cutoffDate))
                .forEach(v -> {
                    v.setActive(false);
                    virementRepository.save(v);
                });

        log.debug("start automatic paying of cheques");
        chequeRepository.findAll()
                .stream()
                .filter(c -> c.getDateEmission().isBefore(cutoffDate))
                .forEach(c -> {
                    c.setActive(false);
                    chequeRepository.save(c);
                });

        log.debug("start automatic paying of MAD");
        miseADispositionRepository.findAll()
                .stream()
                .filter(m -> m.getDateEmission().isBefore(cutoffDate))
                .forEach(m -> {
                    m.setActive(false);
                    miseADispositionRepository.save(m);
                });

        log.debug("start automatic paying of LCN");
        lcnRepository.findAll()
                .stream()
                .filter(l -> l.getDateEcheance().isBefore(cutoffDate))
                .forEach(l -> {
                    l.setActive(false);
                    lcnRepository.save(l);
                });

        log.info("Automatic payment processing completed");
    }
}