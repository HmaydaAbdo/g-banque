package staport.finance.gBanque.Initializers;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.entities.OrganismeLeasing;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.repositories.OrganismeLeasingRepository;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Configuration
public class OrganismeLeasingInitializer5 {

    private static final Logger log = LoggerFactory.getLogger(OrganismeLeasingInitializer5.class);
    private final DataFormatter dataFormatter = new DataFormatter();

    private static final DateTimeFormatter CUSTOM_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");

    @Bean
    @Order(5)
    CommandLineRunner organismeLeasingDataInitializer(OrganismeLeasingRepository repository) {
        return args -> {
            log.info("Initializing OrganismeLeasing data from XLSX...");

            String XLSX_FILE_PATH = "organismes_leasing_data.xlsx";
            try (InputStream inputStream = new ClassPathResource(XLSX_FILE_PATH).getInputStream();
                 Workbook workbook = new XSSFWorkbook(inputStream)) {

                Sheet sheet = workbook.getSheetAt(0);

                for (Row row : sheet) {
                    if (row.getRowNum() == 0) continue; // Skip header

                    String nom = getCellValueAsString(row.getCell(0));
                    String plafondStr = getCellValueAsString(row.getCell(1));
                    String dateDebutStr = getCellValueAsString(row.getCell(2));
                    String dateFinStr = getCellValueAsString(row.getCell(3));

                    if (nom == null || nom.isBlank()) {
                        log.warn("Skipping row {} due to empty name", row.getRowNum());
                        continue;
                    }

                    Optional<OrganismeLeasing> existing = repository.findByNom(nom);
                    if (existing.isPresent()) {
                        log.debug("OrganismeLeasing '{}' already exists. Skipping.", nom);
                        continue;
                    }

                    BigDecimal plafond = parseBigDecimal(plafondStr);
                    OffsetDateTime dateDebut = parseDate(dateDebutStr);
                    OffsetDateTime dateFin = parseDate(dateFinStr);

                    OrganismeLeasing organisme = new OrganismeLeasing(nom, plafond, dateDebut, dateFin);
                    repository.save(organisme);
                    log.info("Saved OrganismeLeasing: '{}'", nom);
                }

                log.info("OrganismeLeasing data initialization complete.");

            } catch (Exception e) {
                log.error("Failed to initialize OrganismeLeasing data from XLSX: {}", e.getMessage(), e);
            }
        };
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        return dataFormatter.formatCellValue(cell);
    }

    private BigDecimal parseBigDecimal(String value) {
        try {
            if (value == null || value.isBlank()) return BigDecimal.ZERO;
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            log.error("Failed to parse BigDecimal from '{}'", value);
            return BigDecimal.ZERO;
        }
    }

    private OffsetDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            // First, try to parse with the custom format that has no offset.
            LocalDateTime localDateTime = LocalDateTime.parse(dateStr, CUSTOM_DATE_TIME_FORMATTER);
            return localDateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime();
        } catch (Exception e) {
            try {
                // If that fails, try the ISO_DATE_TIME format which expects an offset.
                return OffsetDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME);
            } catch (Exception e2) {
                log.error("Failed to parse OffsetDateTime from '{}' using both formats.", dateStr);
                return null;
            }
        }
    }
}