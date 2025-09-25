package staport.finance.gBanque.emails;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender emailSender;

    @Value("${spring.mail.from}") // Inject the 'from' address from application.yml
    private String fromEmailAddress;

    @Autowired
    public EmailService(JavaMailSender emailSender) {
        this.emailSender = emailSender;
    }


    /**
     * Sends a sophisticated HTML email with a company logo, intro, and a table for skipped items.
     * @param to The recipient's email address.
     * @param subject The subject of the email.
     * @param skippedItems A list of maps, where each map represents a skipped item.
     */
    public void reportFunPaiementSynchronisation(String to, String subject, List<Map<String, Object>> skippedItems) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmailAddress);
            helper.setTo(to);
            helper.setSubject(subject);

            // Building the HTML content
            StringBuilder htmlContent = new StringBuilder();

            // Introduction and summary
            htmlContent.append("<p>Bonjour,</p>");
            htmlContent.append("<p>Suite à la synchronisation des données avec l'application FunPaiement, nous vous informons que certaines transactions ont été ignorées en raison d'erreurs.</p>");
            htmlContent.append("<p>Le rapport ci-dessous liste les ").append(skippedItems.size()).append(" éléments ignorés :</p>");

            // Skipped items table
            if (!skippedItems.isEmpty()) {
                htmlContent.append("<table border='1' cellpadding='5' cellspacing='0' style='width: 100%; border-collapse: collapse;'>");

                // Table header
                htmlContent.append("<tr style='background-color: #f2f2f2;'>");
                htmlContent.append("<th style='padding: 10px;'>Numéro de DTO</th>");
                htmlContent.append("<th style='padding: 10px;'>Motif de l'erreur</th>");
                htmlContent.append("</tr>");

                // Table rows
                for (Map<String, Object> item : skippedItems) {
                    htmlContent.append("<tr>");
                    htmlContent.append("<td style='padding: 10px;'>").append(item.getOrDefault("numero", "N/A")).append("</td>");
                    htmlContent.append("<td style='padding: 10px;'>").append(item.getOrDefault("reason", "N/A")).append("</td>");
                    htmlContent.append("</tr>");
                }

                htmlContent.append("</table>");
            } else {
                htmlContent.append("<p>Aucun élément n'a été ignoré durant le traitement.</p>");
            }

            htmlContent.append("<p>Cordialement,<br/>L'équipe Gbanque</p>");
            htmlContent.append("</body></html>");

            helper.setText(htmlContent.toString(), true); // 'true' indicates HTML content

            emailSender.send(message);
            log.info("HTML email sent successfully to {} with subject: '{}'", to, subject);

        } catch (MessagingException | MailException e) {
            log.error("Failed to send HTML email to {}. Reason: {}", to, e.getMessage());
        }
    }
}
