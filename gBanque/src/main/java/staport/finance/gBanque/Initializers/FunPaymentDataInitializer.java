package staport.finance.gBanque.Initializers;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.FunPaiementService.services.EtasDesEncoursService;

@Component
@Order(8)
public class FunPaymentDataInitializer implements CommandLineRunner {


    private static final Logger log = LoggerFactory.getLogger(FunPaymentDataInitializer.class);
    private final EtasDesEncoursService etasDesEncoursService;


    public FunPaymentDataInitializer(EtasDesEncoursService etasDesEncoursService) {
        this.etasDesEncoursService = etasDesEncoursService;
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            JobExecution jobExecution = etasDesEncoursService.launchPaymentImportJob();
            String message = String.format("Payment import job launched successfully. JobExecution ID: %d, Status: %s",
                    jobExecution.getId(), jobExecution.getStatus());
            log.info(message);

        } catch (JobExecutionException e) {
            String errorMessage = "Failed to launch payment import job: " + e.getMessage();
            log.error(errorMessage, e);

        } catch (Exception e) {
            String errorMessage = "An unexpected error occurred while launching the payment import job: " + e.getMessage();
            log.error(errorMessage, e);

        }
    }

}
