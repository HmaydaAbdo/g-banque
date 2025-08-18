package staport.finance.gBanque.lesEtatsDesEncours.FunPaiementService.batch.listeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.SkipListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.lesEtatsDesEncours.FunPaiementService.dtos.FunPaymentDto;
import staport.finance.gBanque.lesEtatsDesEncours.core.entities.Cheque;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class ChequeProcessingSkipListener implements SkipListener<FunPaymentDto, Cheque> {

    private static final Logger log = LoggerFactory.getLogger(ChequeProcessingSkipListener.class);
    private static final Logger skippedItemsLog = LoggerFactory.getLogger("SKIPPED_CHEQUE_ITEMS"); // Dedicated logger for easy filtering

    private final String skippedItemsFilePath; // Path to the file where skipped items will be saved
    private static final DateTimeFormatter FILE_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // A thread-safe list to collect skipped items (for in-memory access/reporting)
    private final List<FunPaymentDto> skippedFunPaymentDtos = Collections.synchronizedList(new ArrayList<>());
    private final List<String> skippedReasons = Collections.synchronizedList(new ArrayList<>());

    // Constructor to inject the file path from properties
    public ChequeProcessingSkipListener(@Value("${batch.skipped-items.file-path:skipped_cheques.log}") String skippedItemsFilePath) {
        this.skippedItemsFilePath = skippedItemsFilePath;
        log.info("ChequeProcessingSkipListener initialized. Skipped items will be written to: {}", skippedItemsFilePath);
    }

    @Override
    public void onSkipInRead(Throwable t) {
        String timestamp = LocalDateTime.now().format(FILE_TIMESTAMP_FORMATTER);
        // We don't have the item here, so only log the error message
        String logEntry = String.format("%s - READ_SKIP - Reason: %s", timestamp, t.getMessage());
        log.error(logEntry); // Log to standard log
        writeSkippedItemToFile(logEntry); // Write to dedicated file
    }

    @Override
    public void onSkipInProcess(FunPaymentDto item, Throwable t) {
        String timestamp = LocalDateTime.now().format(FILE_TIMESTAMP_FORMATTER);
        String reason = "Error in processing: " + t.getMessage();

        // **MODIFICATION HERE: Include item.toString() for all attributes**
        String logEntry = String.format("%s - PROCESS_SKIP - Reason: %s%n  Skipped Item DTO: %s",
                timestamp,
                reason,
                item.toString()); // Calling toString() on the DTO to get all its attributes

        skippedItemsLog.warn(logEntry); // Log to dedicated skipped items logger
        skippedFunPaymentDtos.add(item); // Add to in-memory list
        skippedReasons.add(reason);      // Add reason to in-memory list

        writeSkippedItemToFile(logEntry); // Write to dedicated file
    }

    @Override
    public void onSkipInWrite(Cheque item, Throwable t) {
        String timestamp = LocalDateTime.now().format(FILE_TIMESTAMP_FORMATTER);
        // Include item.toString() for the Cheque entity when skipping in write
        String logEntry = String.format("%s - WRITE_SKIP - Reason: %s%n  Skipped Item Entity: %s",
                timestamp,
                t.getMessage(),
                item.toString()); // Calling toString() on the Cheque entity

        log.error(logEntry); // Log to standard log
        writeSkippedItemToFile(logEntry); // Write to dedicated file
    }

    /**
     * Helper method to write a log entry to the skipped items file.
     * Uses synchronized block to ensure thread-safe writes in a potentially multi-threaded step.
     */
    private void writeSkippedItemToFile(String logEntry) {
        synchronized (this) {
            try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(skippedItemsFilePath, true)))) {
                writer.println(logEntry);
            } catch (IOException e) {
                log.error("Failed to write skipped item to file {}: {}", skippedItemsFilePath, e.getMessage(), e);
            }
        }
    }

    // --- Methods for retrieving collected skipped items after job completion ---
    public List<FunPaymentDto> getSkippedFunPaymentDtos() {
        return Collections.unmodifiableList(skippedFunPaymentDtos);
    }

    public List<String> getSkippedReasons() {
        return Collections.unmodifiableList(skippedReasons);
    }

    public void clearSkippedItems() {
        skippedFunPaymentDtos.clear();
        skippedReasons.clear();
        log.info("Cleared in-memory skipped items list and reasons.");
    }
}