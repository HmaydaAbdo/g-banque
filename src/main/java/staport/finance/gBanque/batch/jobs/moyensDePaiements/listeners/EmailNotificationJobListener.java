package staport.finance.gBanque.batch.jobs.moyensDePaiements.listeners;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.moyensDePaiements.synchronisation.dtos.SkippedItemReportDto;
import staport.finance.gBanque.emails.EmailService;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class EmailNotificationJobListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationJobListener.class);

    private final ProcessingSkipListener processingSkipListener;
    private final EmailService emailService;

    @Value("${batch.skipped-items.report-recipient:hmayda@staport-sa.ma}")
    private String reportRecipient;

    @Autowired
    public EmailNotificationJobListener(ProcessingSkipListener processingSkipListener, EmailService emailService) {
        this.processingSkipListener = processingSkipListener;
        this.emailService = emailService;
    }

    @Override
    public void beforeJob( JobExecution jobExecution) {
        log.info("Starting job execution listener.");
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        log.info("Job finished. Checking for skipped item reports to send emails.");

        // Type is now List<SkippedItemReportDto>
        Map<String, List<SkippedItemReportDto>> reports = processingSkipListener.getSkippedReportsByType();

        reports.forEach((type, skippedItems) -> {
            if (!skippedItems.isEmpty()) {
                List<Map<String, Object>> formattedSkippedItems = new ArrayList<>();

                for (SkippedItemReportDto reportDto : skippedItems) {
                    Map<String, Object> itemMap = new HashMap<>();

                    // 1. Numéro de DTO (using numeroDto)
                    itemMap.put("numero", reportDto.getNumeroDto());

                    // 2. Motif de l'erreur
                    // This constructs a clear, clean message for the email table.
                    String reason = String.format(
                            reportDto.getCoreReason()
                    );

                    itemMap.put("reason", reason);

                    formattedSkippedItems.add(itemMap);
                }

                String subject = String.format(" %d %s ignorés lors de la synchronisation", skippedItems.size(), type);

                log.info("Sending HTML email for skipped items of type: {}", type);
                emailService.reportFunPaiementSynchronisation(reportRecipient, subject, formattedSkippedItems);
            }
        });

        if (reports.isEmpty()) {
            log.info("No skipped items found for any type. No emails will be sent.");
        }
    }
}