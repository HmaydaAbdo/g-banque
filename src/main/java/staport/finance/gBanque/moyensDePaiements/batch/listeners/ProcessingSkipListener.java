package staport.finance.gBanque.moyensDePaiements.batch.listeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.SkipListener;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.moyensDePaiements.synchronisation.dtos.FunPaymentDto;
import staport.finance.gBanque.moyensDePaiements.synchronisation.dtos.SkippedItemReportDto;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ProcessingSkipListener implements SkipListener<FunPaymentDto, Object> {

    private static final Logger log = LoggerFactory.getLogger(ProcessingSkipListener.class);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Map to hold all skipped item reports, keyed by item type
    private final Map<String, List<SkippedItemReportDto>> skippedReportsByType = new ConcurrentHashMap<>();

    // Map to link DTO type to a simple string key
    private final Map<Integer, String> typeToFileNameKeyMap = new ConcurrentHashMap<>();

    public ProcessingSkipListener(

    ) {
        typeToFileNameKeyMap.put(1, "Cheque");
        typeToFileNameKeyMap.put(2, "Lcn");
        typeToFileNameKeyMap.put(3, "Virements");
        typeToFileNameKeyMap.put(4, "MiseADisposition");

        log.info("ProcessingSkipListener initialized for in-memory skip collection.");
    }

    public Map<String, List<SkippedItemReportDto>> getSkippedReportsByType() {
        return skippedReportsByType;
    }

    @Override
    public void onSkipInRead(Throwable t) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        String reason = "READ_SKIP - Reason: " + t.getMessage();
        log.error(reason);

        // Storing basic report for READ skips
        skippedReportsByType.computeIfAbsent("READ", k -> new ArrayList<>()).add(
                new SkippedItemReportDto("N/A", reason, "N/A", timestamp, "READ_SKIP"));
    }

    @Override
    public void onSkipInProcess(FunPaymentDto item, Throwable t) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        String reason = "Error in processing: " + t.getMessage();

        // Creating the simplified structured DTO
        SkippedItemReportDto reportDto = new SkippedItemReportDto(
                item.getNumero(),
                reason,
                String.valueOf(item.getType()),
                timestamp,
                "PROCESS_SKIP"
        );

        // Logging the full verbose string for debugging/traceability
        log.warn("Skipped Item DTO: {}", reportDto.toString());

        // Storing the DTO in the map
        String fileKey = typeToFileNameKeyMap.getOrDefault(item.getType(), "PROCESS");
        skippedReportsByType
                .computeIfAbsent(fileKey, k -> new ArrayList<>())
                .add(reportDto);
    }

    @Override
    public void onSkipInWrite(Object item, Throwable t) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        String reason = "WRITE_SKIP - Reason: " + t.getMessage();
        log.error(reason);

        // Storing basic report for WRITE skips
        skippedReportsByType.computeIfAbsent("WRITE", k -> new ArrayList<>()).add(
                new SkippedItemReportDto("N/A", reason, "N/A", timestamp, "WRITE_SKIP"));
    }
}