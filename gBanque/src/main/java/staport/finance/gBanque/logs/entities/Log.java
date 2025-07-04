package staport.finance.gBanque.logs.entities;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "log") // Explicitly define table name
public class Log {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "timestamp", nullable = false)
    private OffsetDateTime timestamp;

    @Column(name = "user_name", nullable = false)
    private String userName;

    @Column(name = "operation_type", nullable = false)
    private String operationType = "UPDATE"; // Default as per your requirement

    @Column(name = "entity_name", nullable = false)
    private String entityName;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    // One-to-Many relationship with LogEntryDetail
    // mappedBy refers to the 'log' field in LogEntryDetail
    // CascadeType.ALL means all operations (persist, merge, remove, refresh, detach) on Log
    // will be cascaded to its LogEntryDetail children.
    // orphanRemoval = true means if a LogEntryDetail is removed from the collection, it's deleted from the database.
    @OneToMany(mappedBy = "log", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LogEntryDetail> logDetails = new ArrayList<>();


    // Constructors
    public Log() {
        this.timestamp = OffsetDateTime.now();
    }

    public Log(String userName, String entityName, Long entityId) {
        this(); // Call default constructor to set timestamp
        this.userName = userName;
        this.entityName = entityName;
        this.entityId = entityId;
    }

    // Helper method to add a LogEntryDetail and ensure bidirectional relationship is set
    public void addLogDetail(LogEntryDetail detail) {
        logDetails.add(detail);
        detail.setLog(this); // Set the parent Log for the detail
    }

    // Helper method to remove a LogEntryDetail
    public void removeLogDetail(LogEntryDetail detail) {
        logDetails.remove(detail);
        detail.setLog(null); // Remove the parent Log reference
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

    public List<LogEntryDetail> getLogDetails() {
        return logDetails;
    }

    public void setLogDetails(List<LogEntryDetail> logDetails) {
        this.logDetails = logDetails;
    }

    @PrePersist
    protected void onCreate() {
        timestamp = OffsetDateTime.now();
    }
}