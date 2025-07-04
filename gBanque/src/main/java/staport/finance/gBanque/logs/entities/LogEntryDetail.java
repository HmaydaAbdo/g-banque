package staport.finance.gBanque.logs.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "log_entry_detail") // Explicitly define table name
public class LogEntryDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many-to-One relationship with Log
    // @JoinColumn specifies the foreign key column in this table (log_entry_detail)
    // The 'log_id' column will be created/managed by JPA to link to the 'id' of the Log entity.
    @ManyToOne(fetch = FetchType.LAZY) // FetchType.LAZY is recommended for performance
    @JoinColumn(name = "log_id", nullable = false)
    private Log log; // This is the reference to the parent Log entity

    @Column(name = "field_name", nullable = false, length = 255)
    private String fieldName;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;


    // Constructors
    public LogEntryDetail() {
    }

    // Modified constructor to accept the parent Log entity directly
    public LogEntryDetail(Log log, String fieldName, String oldValue, String newValue) {
        this.log = log;
        this.fieldName = fieldName;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    // Replaced getLogId() with getLog() as the relationship is now object-based
    public Log getLog() {
        return log;
    }

    public void setLog(Log log) {
        this.log = log;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }
}