package staport.finance.gBanque.logs.dtos;

import org.springframework.format.annotation.DateTimeFormat;
import java.time.OffsetDateTime;
import java.io.Serializable;
import java.util.Objects;

public class LogSearchCriteria implements Serializable {

    private String userName;
    private String operationType;
    private String entityName;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime timestampStart;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime timestampEnd;

    public LogSearchCriteria() {
    }

    // Getters and Setters
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


    public OffsetDateTime getTimestampStart() {
        return timestampStart;
    }

    public void setTimestampStart(OffsetDateTime timestampStart) {
        this.timestampStart = timestampStart;
    }

    public OffsetDateTime getTimestampEnd() {
        return timestampEnd;
    }

    public void setTimestampEnd(OffsetDateTime timestampEnd) {
        this.timestampEnd = timestampEnd;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogSearchCriteria that = (LogSearchCriteria) o;
        return Objects.equals(userName, that.userName) &&
                Objects.equals(operationType, that.operationType) &&
                Objects.equals(entityName, that.entityName) &&
                Objects.equals(timestampStart, that.timestampStart) &&
                Objects.equals(timestampEnd, that.timestampEnd);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userName, operationType, entityName, timestampStart, timestampEnd);
    }

    @Override
    public String toString() {
        return "LogSearchCriteria{" +
                "userName='" + userName + '\'' +
                ", operationType='" + operationType + '\'' +
                ", entityName='" + entityName + '\'' +
                ", timestampStart=" + timestampStart +
                ", timestampEnd=" + timestampEnd +
                '}';
    }
}