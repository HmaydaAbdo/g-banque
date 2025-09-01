package staport.finance.gBanque.lesEtatsDesEncours.FunPaiementService.batch.listeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.SkipListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.lesEtatsDesEncours.FunPaiementService.dtos.FunPaymentDto;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ProcessingSkipListener implements SkipListener<FunPaymentDto, Object> {

    private static final Logger log = LoggerFactory.getLogger(ProcessingSkipListener.class);
    private static final DateTimeFormatter FILE_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Map<String, String> skippedItemsFilePaths = new ConcurrentHashMap<>();
    private final Map<Integer, String> typeToFileNameKeyMap = new ConcurrentHashMap<>();

    public ProcessingSkipListener(
            @Value("${batch.skipped-items.cheque-file-path:skipped_cheques.log}") String chequeFilePath,
            @Value("${batch.skipped-items.lcn-file-path:skipped_lcns.log}") String lcnFilePath,
            @Value("${batch.skipped-items.mad-file-path:skipped_mises_a_disposition.log}") String madFilePath,
            @Value("${batch.skipped-items.mad-file-path:skipped_virements.log}") String virFilePath
    ) {
        skippedItemsFilePaths.put("Cheque", chequeFilePath);
        skippedItemsFilePaths.put("Lcn", lcnFilePath);
        skippedItemsFilePaths.put("MiseADisposition", madFilePath);
        skippedItemsFilePaths.put("Virements", virFilePath);
        skippedItemsFilePaths.put("READ", "skipped_read.log");
        skippedItemsFilePaths.put("PROCESS", "skipped_process_generic.log");

        typeToFileNameKeyMap.put(1, "Cheque");
        typeToFileNameKeyMap.put(2, "Lcn");
        typeToFileNameKeyMap.put(3, "Virements");
        typeToFileNameKeyMap.put(4, "MiseADisposition");

        log.info("ProcessingSkipListener initialized with type-specific file paths: {}", skippedItemsFilePaths);
    }

    @Override
    public void onSkipInRead(Throwable t) {
        String timestamp = LocalDateTime.now().format(FILE_TIMESTAMP_FORMATTER);
        String logEntry = String.format("%s - READ_SKIP - Reason: %s", timestamp, t.getMessage());
        log.error(logEntry);
        writeSkippedItemToFile(logEntry, "READ");
    }

    @Override
    public void onSkipInProcess(FunPaymentDto item, Throwable t) {
        String timestamp = LocalDateTime.now().format(FILE_TIMESTAMP_FORMATTER);
        String reason = "Error in processing: " + t.getMessage();
        String logEntry = String.format("%s - PROCESS_SKIP - Reason: %s%n  Skipped Item DTO: %s",
                timestamp, reason, item.toString());
        log.warn(logEntry);
        // Use the type from the DTO to get the correct file key
        String fileKey = typeToFileNameKeyMap.getOrDefault(item.getType(), "PROCESS");
        writeSkippedItemToFile(logEntry, fileKey);
    }

    @Override
    public void onSkipInWrite(Object item, Throwable t) {
        String timestamp = LocalDateTime.now().format(FILE_TIMESTAMP_FORMATTER);
        String itemType = item.getClass().getSimpleName();
        String logEntry = String.format("%s - WRITE_SKIP - Reason: %s%n  Skipped Item Entity: %s",
                timestamp, t.getMessage(), item);
        log.error(logEntry);
        writeSkippedItemToFile(logEntry, itemType);
    }

    /**
     * Helper method to write a log entry to the correct skipped items file.
     * Uses a synchronized block to ensure thread-safe writes.
     *
     * @param logEntry The log message to write.
     * @param fileKey  The key to get the correct file path (e.g., "Cheque", "Lcn").
     */
    private void writeSkippedItemToFile(String logEntry, String fileKey) {
        String filePath = skippedItemsFilePaths.get(fileKey);
        if (filePath != null) {
            synchronized (this) {
                try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(filePath, true)))) {
                    writer.println(logEntry);
                } catch (IOException e) {
                    log.error("Failed to write skipped item to file {}: {}", filePath, e.getMessage(), e);
                }
            }
        } else {
            log.error("Failed to write skipped item. No file path found for key: {}", fileKey);
        }
    }
}