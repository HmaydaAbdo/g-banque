package staport.finance.gBanque.logs.dtos;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

public class LogResponse implements Serializable {

    private Long id;
    private OffsetDateTime timestamp;
    private String userName;
    private String operationType;
    private String entityName;
    private Long entityId;
    private List<LogEntryDetailResponse> logDetails; // Liste des détails du log

    public LogResponse() {
    }

    public LogResponse(Long id, OffsetDateTime timestamp, String userName, String operationType, String entityName, Long entityId, List<LogEntryDetailResponse> logDetails) {
        this.id = id;
        this.timestamp = timestamp;
        this.userName = userName;
        this.operationType = operationType;
        this.entityName = entityName;
        this.entityId = entityId;
        this.logDetails = logDetails;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public List<LogEntryDetailResponse> getLogDetails() {
        return logDetails;
    }

    public void setLogDetails(List<LogEntryDetailResponse> logDetails) {
        this.logDetails = logDetails;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogResponse logResponse = (LogResponse) o;
        return Objects.equals(id, logResponse.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "LogResponse{" +
                "id=" + id +
                ", timestamp=" + timestamp +
                ", userName='" + userName + '\'' +
                ", operationType='" + operationType + '\'' +
                ", entityName='" + entityName + '\'' +
                ", entityId=" + entityId +
                '}';
    }
}