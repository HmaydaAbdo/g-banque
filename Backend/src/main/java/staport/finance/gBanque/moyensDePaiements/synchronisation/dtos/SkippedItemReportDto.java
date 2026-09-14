package staport.finance.gBanque.moyensDePaiements.synchronisation.dtos;

import java.io.Serializable;

public class SkippedItemReportDto implements Serializable {
    private final String numeroDto;
    private final String coreReason; // e.g., "Error in processing: Beneficiaire is null or empty"
    private final String typeCode;
    private final String logTimestamp;
    private final String logEvent; // e.g., "PROCESS_SKIP"

    public SkippedItemReportDto(String numeroDto, String coreReason, String typeCode, String logTimestamp, String logEvent) {
        this.numeroDto = numeroDto;
        this.coreReason = coreReason;
        this.typeCode = typeCode;
        this.logTimestamp = logTimestamp;
        this.logEvent = logEvent;
    }

    // Getters for the EmailNotificationJobListener
    public String getNumeroDto() { return numeroDto; }
    public String getCoreReason() { return coreReason; }
    public String getTypeCode() { return typeCode; }
    public String getLogTimestamp() { return logTimestamp; }
    public String getLogEvent() { return logEvent; }

    @Override
    public String toString() {
        return String.format("%s - %s - Reason: %s - DTO: {numero='%s', type='%s'}",
                logTimestamp, logEvent, coreReason, numeroDto, typeCode);
    }
}