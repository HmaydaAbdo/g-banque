package staport.finance.gBanque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableScheduling;
import staport.finance.gBanque.cmt.echeances.entities.EcheanceCMT;
import staport.finance.gBanque.cmt.echeances.enums.StatutEcheanceCMT;
import staport.finance.gBanque.cmt.echeances.repositories.EcheanceCMTRepository;
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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;


@SpringBootApplication
@EnableAspectJAutoProxy
@EnableBatchProcessing
@EnableScheduling
@EnableJpaAuditing
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class GBanqueApplication {

    private static final Logger log = LoggerFactory.getLogger(GBanqueApplication.class);

    public static void main(String[] args) {
		SpringApplication.run(GBanqueApplication.class, args);
	}

    @Bean
    CommandLineRunner cmtAutomaticPaying (EcheanceCMTRepository cmtRepository){
        return args -> {
            log.debug("start automatic paying of cmt");
            List<EcheanceCMT> echeanceCMTList= cmtRepository
                    .findAll()
                    .stream()
                    .filter(echeanceCMT -> echeanceCMT.getDateEcheance().isBefore(OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)))
                    .map(echeanceCMT -> {
                        echeanceCMT.setStatutPaiement(StatutEcheanceCMT.PAYE);
                        return cmtRepository.save(echeanceCMT);
                    })
                    .toList();

            log.debug("end automatic paying of cmt , item processed size is {}",echeanceCMTList.size());
        };
    }


    @Bean
    CommandLineRunner LeasingAutomaticPaying (EcheanceLeasingRepository echeanceLeasingRepository){
        return args -> {
            log.debug("start automatic paying of leasing");
            List<EcheanceLeasing> echeanceLeasings= echeanceLeasingRepository
                    .findAll()
                    .stream()
                    .filter(echeanceLeasing -> echeanceLeasing.getDateEcheance().isBefore(OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)))
                    .map(echeanceLeasing -> {
                        echeanceLeasing.setStatutPaiement(StatutEcheanceLeasing.PAYE);
                        return echeanceLeasingRepository.save(echeanceLeasing);
                    })
                    .toList();

            log.debug("end automatic paying of leasing , item processed size is {}",echeanceLeasings.size());
        };
    }



    @Bean
    CommandLineRunner VirementAutomaticPaying (VirementRepository virementRepository){
        return args -> {
            log.debug("start automatic paying of virements");
            List<Virement> virements= virementRepository
                    .findAll()
                    .stream()
                    .filter(virement -> virement.getDateEmission().isBefore(OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)))
                    .map(virement -> {
                        virement.setActive(false);
                        return virementRepository.save(virement);
                    })
                    .toList();

            log.debug("end automatic paying of virement , item processed size is {}",virements.size());
        };
    }

    @Bean
    CommandLineRunner ChequeAutomaticPaying (ChequeRepository chequeRepository){
        return args -> {
            log.debug("start automatic paying of cheques");
            List<Cheque> cheques= chequeRepository
                    .findAll()
                    .stream()
                    .filter(cheque -> cheque.getDateEmission().isBefore(OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)))
                    .map(cheque -> {
                        cheque.setActive(false);
                        return chequeRepository.save(cheque);
                    })
                    .toList();

            log.debug("end automatic paying of cheque , item processed size is {}",cheques.size());
        };
    }


    @Bean
    CommandLineRunner MiseADispoAutomaticPaying (MiseADispositionRepository miseADispositionRepository){
        return args -> {
            log.debug("start automatic paying of MAD");
            List<MiseADisposition> miseADispositions= miseADispositionRepository
                    .findAll()
                    .stream()
                    .filter(mad -> mad.getDateEmission().isBefore(OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)))
                    .map(mad -> {
                        mad.setActive(false);
                        return miseADispositionRepository.save(mad);
                    })
                    .toList();

            log.debug("end automatic paying of mise à dispositions , item processed size is {}",miseADispositions.size());
        };
    }



    @Bean
    CommandLineRunner LcnAutomaticPaying (LcnRepository lcnRepository){
        return args -> {
            log.debug("start automatic paying of LCN");
            List<Lcn> lcns= lcnRepository
                    .findAll()
                    .stream()
                    .filter(lcn -> lcn.getDateEcheance().isBefore(OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)))
                    .map(mad -> {
                        mad.setActive(false);
                        return lcnRepository.save(mad);
                    })
                    .toList();

            log.debug("end automatic paying of lcn , item processed size is {}",lcns.size());
        };
    }

}
