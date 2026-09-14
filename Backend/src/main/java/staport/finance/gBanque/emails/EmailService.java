package staport.finance.gBanque.emails;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender emailSender;

    @Value("${spring.mail.from}")
    private String fromEmailAddress;

    @Value("${spring.mail.notification.batch-job.recipients}")
    private String defaultRecipients;

    @Autowired
    public EmailService(JavaMailSender emailSender) {
        this.emailSender = emailSender;
    }

    /**
     * ✅ MÉTHODE DE TEST SIMPLE
     * Envoie un email de test avec juste un sujet et un contenu.
     * Utilise automatiquement l'expéditeur et le destinataire configurés dans application.yml
     *
     * @param subject Le sujet de l'email
     * @param content Le contenu de l'email (texte simple ou HTML)
     */
    public void sendTestEmail(String subject, String content) {
        try {
            // Prendre le premier email de la liste des destinataires
            String recipient = defaultRecipients.split(",")[0].trim();

            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmailAddress);
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(content, true); // true = HTML

            emailSender.send(message);

            log.info("✅ Test email sent successfully!");
            log.info("   From: {}", fromEmailAddress);
            log.info("   To: {}", recipient);
            log.info("   Subject: {}", subject);

        } catch (MessagingException | MailException e) {
            log.error("❌ Failed to send test email. Reason: {}", e.getMessage(), e);
            throw new RuntimeException("Email test failed: " + e.getMessage(), e);
        }
    }

    /**
     * Sends a sophisticated HTML email with a company logo, intro, and a table for skipped items.
     */
    public void reportFunPaiementSynchronisation(String to, String subject, List<Map<String, Object>> skippedItems) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmailAddress);
            helper.setTo(to);
            helper.setSubject(subject);

            StringBuilder htmlContent = new StringBuilder();
            htmlContent.append("<p>Bonjour,</p>");
            htmlContent.append("<p>Suite à la synchronisation des données avec l'application FunPaiement, nous vous informons que certaines transactions ont été ignorées en raison d'erreurs.</p>");
            htmlContent.append("<p>Le rapport ci-dessous liste les ").append(skippedItems.size()).append(" éléments ignorés :</p>");

            if (!skippedItems.isEmpty()) {
                htmlContent.append("<table border='1' cellpadding='5' cellspacing='0' style='width: 100%; border-collapse: collapse;'>");
                htmlContent.append("<tr style='background-color: #f2f2f2;'>");
                htmlContent.append("<th style='padding: 10px;'>Numéro de DTO</th>");
                htmlContent.append("<th style='padding: 10px;'>Motif de l'erreur</th>");
                htmlContent.append("</tr>");

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

            helper.setText(htmlContent.toString(), true);
            emailSender.send(message);
            log.info("HTML email sent successfully to {} with subject: '{}'", to, subject);

        } catch (MessagingException | MailException e) {
            log.error("Failed to send HTML email to {}. Reason: {}", to, e.getMessage());
        }
    }

    /**
     * Sends a comprehensive batch job failure notification email.
     */
    public void sendBatchJobFailureNotification(
            String to,
            String subject,
            String jobName,
            Long jobId,
            String status,
            String startTime,
            String endTime,
            long durationSeconds,
            Map<String, Map<String, Object>> stepStatistics,
            List<Map<String, Object>> failureDetails,
            String exitDescription) {

        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmailAddress);
            helper.setTo(to);
            helper.setSubject(subject);

            String htmlContent = buildBatchJobFailureEmailContent(
                    jobName, jobId, status, startTime, endTime, durationSeconds,
                    stepStatistics, failureDetails, exitDescription
            );

            helper.setText(htmlContent, true);
            emailSender.send(message);
            log.info("Batch job failure notification email sent successfully to {} for job: {}", to, jobName);
        } catch (MessagingException | MailException e) {
            log.error("Failed to send batch job failure notification email to {} for job: {}. Reason: {}",
                    to, jobName, e.getMessage(), e);
        }
    }

    private String buildBatchJobFailureEmailContent(
            String jobName,
            Long jobId,
            String status,
            String startTime,
            String endTime,
            long durationSeconds,
            Map<String, Map<String, Object>> stepStatistics,
            List<Map<String, Object>> failureDetails,
            String exitDescription) {

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html><head><meta charset='UTF-8'></head><body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>");

        html.append("<div style='background-color: #d32f2f; color: white; padding: 20px; border-radius: 5px;'>");
        html.append("<h2 style='margin: 0;'>⚠️ Échec du Job Batch - Intervention Requise</h2>");
        html.append("</div>");

        html.append("<div style='padding: 20px;'>");
        html.append("<p>Bonjour,</p>");
        html.append("<p>Le job batch <strong>").append(escapeHtml(jobName))
                .append("</strong> a échoué lors de son exécution automatique.</p>");

        html.append("<h3 style='color: #d32f2f; border-bottom: 2px solid #d32f2f; padding-bottom: 5px;'>📋 Informations Générales</h3>");
        html.append("<table style='width: 100%; border-collapse: collapse; margin-bottom: 20px;'>");
        html.append("<tr style='background-color: #f5f5f5;'>");
        html.append("<td style='padding: 12px; border: 1px solid #ddd; font-weight: bold; width: 30%;'>Nom du Job</td>");
        html.append("<td style='padding: 12px; border: 1px solid #ddd;'>").append(escapeHtml(jobName)).append("</td>");
        html.append("</tr>");
        html.append("<tr>");
        html.append("<td style='padding: 12px; border: 1px solid #ddd; font-weight: bold;'>ID d'exécution</td>");
        html.append("<td style='padding: 12px; border: 1px solid #ddd;'>").append(jobId).append("</td>");
        html.append("</tr>");
        html.append("<tr style='background-color: #f5f5f5;'>");
        html.append("<td style='padding: 12px; border: 1px solid #ddd; font-weight: bold;'>Statut</td>");
        html.append("<td style='padding: 12px; border: 1px solid #ddd; color: #d32f2f; font-weight: bold;'>")
                .append(escapeHtml(status)).append("</td>");
        html.append("</tr>");
        html.append("<tr>");
        html.append("<td style='padding: 12px; border: 1px solid #ddd; font-weight: bold;'>Date de début</td>");
        html.append("<td style='padding: 12px; border: 1px solid #ddd;'>").append(escapeHtml(startTime)).append("</td>");
        html.append("</tr>");
        html.append("<tr style='background-color: #f5f5f5;'>");
        html.append("<td style='padding: 12px; border: 1px solid #ddd; font-weight: bold;'>Date de fin</td>");
        html.append("<td style='padding: 12px; border: 1px solid #ddd;'>").append(escapeHtml(endTime)).append("</td>");
        html.append("</tr>");
        html.append("<tr>");
        html.append("<td style='padding: 12px; border: 1px solid #ddd; font-weight: bold;'>Durée totale</td>");
        html.append("<td style='padding: 12px; border: 1px solid #ddd;'>")
                .append(formatDuration(durationSeconds)).append("</td>");
        html.append("</tr>");
        html.append("</table>");

        if (!stepStatistics.isEmpty()) {
            html.append("<h3 style='color: #1976d2; border-bottom: 2px solid #1976d2; padding-bottom: 5px;'>📊 Statistiques Détaillées par Étape</h3>");
            for (Map.Entry<String, Map<String, Object>> entry : stepStatistics.entrySet()) {
                String stepName = entry.getKey();
                Map<String, Object> stats = entry.getValue();
                html.append("<h4 style='color: #555; margin-top: 15px;'>▸ Étape : ").append(escapeHtml(stepName)).append("</h4>");
                html.append("<table style='width: 100%; border-collapse: collapse; margin-bottom: 15px;'>");
                boolean alternate = false;
                for (Map.Entry<String, Object> stat : stats.entrySet()) {
                    html.append("<tr").append(alternate ? " style='background-color: #f9f9f9;'" : "").append(">");
                    html.append("<td style='padding: 10px; border: 1px solid #ddd; font-weight: bold; width: 40%;'>")
                            .append(escapeHtml(stat.getKey())).append("</td>");
                    html.append("<td style='padding: 10px; border: 1px solid #ddd;'>")
                            .append(stat.getValue()).append("</td>");
                    html.append("</tr>");
                    alternate = !alternate;
                }
                html.append("</table>");
            }
        }

        if (!failureDetails.isEmpty()) {
            html.append("<h3 style='color: #d32f2f; border-bottom: 2px solid #d32f2f; padding-bottom: 5px;'>❌ Détails des Erreurs</h3>");
            html.append("<table style='width: 100%; border-collapse: collapse; margin-bottom: 20px;'>");
            html.append("<thead>");
            html.append("<tr style='background-color: #d32f2f; color: white;'>");
            html.append("<th style='padding: 12px; border: 1px solid #ddd; text-align: left; width: 30%;'>Composant / Étape</th>");
            html.append("<th style='padding: 12px; border: 1px solid #ddd; text-align: left;'>Description de l'erreur</th>");
            html.append("</tr>");
            html.append("</thead>");
            html.append("<tbody>");
            boolean alternate = false;
            for (Map<String, Object> item : failureDetails) {
                html.append("<tr").append(alternate ? " style='background-color: #ffebee;'" : "").append(">");
                html.append("<td style='padding: 10px; border: 1px solid #ddd; font-weight: bold;'>")
                        .append(escapeHtml(String.valueOf(item.getOrDefault("numero", "N/A")))).append("</td>");
                html.append("<td style='padding: 10px; border: 1px solid #ddd;'>")
                        .append(escapeHtml(String.valueOf(item.getOrDefault("reason", "N/A")))).append("</td>");
                html.append("</tr>");
                alternate = !alternate;
            }
            html.append("</tbody>");
            html.append("</table>");
        }

        if (exitDescription != null && !exitDescription.trim().isEmpty()) {
            html.append("<h3 style='color: #ff6f00; border-bottom: 2px solid #ff6f00; padding-bottom: 5px;'>📝 Message de Sortie</h3>");
            html.append("<div style='background-color: #fff3e0; padding: 15px; border-left: 4px solid #ff6f00; border-radius: 3px; margin-bottom: 20px;'>");
            html.append("<p style='margin: 0; font-family: monospace;'>").append(escapeHtml(exitDescription)).append("</p>");
            html.append("</div>");
        }

        html.append("<div style='background-color: #fff9c4; padding: 15px; border-left: 4px solid #fbc02d; border-radius: 3px; margin: 20px 0;'>");
        html.append("<h4 style='margin-top: 0; color: #f57f17;'>⚡ Actions Requises</h4>");
        html.append("<ul style='margin: 10px 0;'>");
        html.append("<li>Vérifier les logs détaillés du serveur d'application</li>");
        html.append("<li>Analyser les causes racines des erreurs détectées</li>");
        html.append("<li>Corriger les problèmes identifiés dans la base de données ou le code</li>");
        html.append("<li>Relancer manuellement le job si nécessaire après correction</li>");
        html.append("</ul>");
        html.append("</div>");

        html.append("<hr style='border: none; border-top: 1px solid #ddd; margin: 30px 0;'>");
        html.append("<p style='color: #666; font-size: 0.9em;'>");
        html.append("Cet email a été généré automatiquement par le système de monitoring des jobs batch.<br>");
        html.append("Pour toute question, veuillez contacter l'équipe technique.<br><br>");
        html.append("<strong>Cordialement,</strong><br>");
        html.append("L'équipe Gbanque - Système de Batch Jobs");
        html.append("</p>");
        html.append("</div>");
        html.append("</body></html>");

        return html.toString();
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String formatDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        if (hours > 0) {
            return String.format("%d h %d min %d s", hours, minutes, secs);
        } else if (minutes > 0) {
            return String.format("%d min %d s", minutes, secs);
        } else {
            return String.format("%d s", secs);
        }
    }
}