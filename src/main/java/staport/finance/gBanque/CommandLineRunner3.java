package staport.finance.gBanque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.commision.entities.Commission;
import staport.finance.gBanque.commision.repository.CommisionRepository;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;
import staport.finance.gBanque.leasing.contratLeasing.repositories.ContratLeasingRepository;
import staport.finance.gBanque.leasing.echeancesLeasing.entities.EcheanceLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.enums.StatutEcheanceLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.repositories.EcheanceLeasingRepository;
import staport.finance.gBanque.prefinancement.enums.Status;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
/*
@Component
public class CommandLineRunner3 implements CommandLineRunner {


    private final ContratLeasingRepository contratLeasingRepository;
    private final EcheanceLeasingRepository echeanceLeasingRepository;
    private final CommisionRepository commisionRepository;
    private static final Logger log = LoggerFactory.getLogger(CommandLineRunner3.class);

    public CommandLineRunner3(ContratLeasingRepository contratLeasingRepository, EcheanceLeasingRepository echeanceLeasingRepository, CommisionRepository commisionRepository) {
        this.contratLeasingRepository = contratLeasingRepository;
        this.echeanceLeasingRepository = echeanceLeasingRepository;
        this.commisionRepository = commisionRepository;
    }



    @Override
    public void run(String... args) throws Exception {
        List<String> contrats=List.of("0558600");
        OffsetDateTime date = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        contrats.forEach((numContart)->{
         desactiverCommisionAvantDate(numContart,date);
         desactiverEcheanceAvantDate(numContart,date);
        });

    }


    public void desactiverEcheanceAvantDate(String numContrat,OffsetDateTime fixedDate ){

        ContratLeasing contratLeasing=contratLeasingRepository.findByNumeroContrat(numContrat)
                .orElseThrow(
                        ()->{
                            log.error("contrat with numero  {} not found",numContrat);
                            return new ResourceNotFoundException("Contral Leasing not found");}
                );

        List<EcheanceLeasing> echeanceLeasings=echeanceLeasingRepository.findByContratLeasing(contratLeasing);

        echeanceLeasings=echeanceLeasings.stream()
                .filter(e->e.getDateEcheance().isBefore(fixedDate))
                .toList();

        echeanceLeasings.forEach(e->{
            e.setStatutPaiement(StatutEcheanceLeasing.PAYE);
            echeanceLeasingRepository.save(e);
        });
    }
    public void  desactiverCommisionAvantDate(String numContrat,OffsetDateTime fixedDate){
        String labellePattern= String.format("Commission Prelevement Leasing contrat N°%s",numContrat);
        List<Commission> commissions=commisionRepository.findByLibelleLike(labellePattern);
        commissions=commissions.stream()
                .filter(c->c.getDateEmission().isBefore(fixedDate))
                .toList();
        commissions.forEach(c->{
            c.setStatus(Status.PAYE);
            commisionRepository.save(c);
        });
    }
    public void deleteEcheance( String numContart){
        log.info("strating deleting echeances for contrat {} ",numContart);

        ContratLeasing contratLeasing=contratLeasingRepository.findByNumeroContrat(numContart)
                .orElseThrow(
                        ()->{
                            log.error("contrat with numero  {} not found",numContart);
                            return new ResourceNotFoundException("Contral Leasing not found");}
                );

        List<EcheanceLeasing> echeanceLeasings=echeanceLeasingRepository.findByContratLeasing(contratLeasing);
        log.info("{} echeances trouvé ",echeanceLeasings.size());
        echeanceLeasingRepository.deleteAll(echeanceLeasings);
        log.info("echeances supprimé ");
    }
    public void deleteCommission(String numContrat) {
        log.info("strating deleting commissions ");
        String labellePattern= String.format("Commission Prelevement Leasing contrat N°%s",numContrat);
        List<Commission> commissions=commisionRepository.findByLibelleLike(labellePattern);
        log.info("found {}",commissions.size());
        commisionRepository.deleteAll(commissions);
    }
}
*/