package staport.finance.gBanque.LigneFonctionnelle.initializer;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.repositories.NatureLigneFonctionnelleRepository;

import java.io.InputStream;

@Configuration
public class NatureLigneFonctionnelleInitializer3 {

    private static final Logger log = LoggerFactory.getLogger(NatureLigneFonctionnelleInitializer3.class);
    private final DataFormatter dataFormatter = new DataFormatter();

    @Bean
    @Order(3)
    CommandLineRunner natureLigneFonctionnelleDataInitializer(NatureLigneFonctionnelleRepository repository) {
        return args -> {
            log.info("Initializing NatureLigneFonctionnelle data from XLSX...");

            String XLSX_FILE_PATH = "nature_lignes_fonctionnelles.xlsx";
            try (InputStream inputStream = new ClassPathResource(XLSX_FILE_PATH).getInputStream();
                 Workbook workbook = new XSSFWorkbook(inputStream)) {

                Sheet sheet = workbook.getSheetAt(0);

                for (Row row : sheet) {
                    if (row.getRowNum() == 0) continue; // Skip header

                    String label = getCellValueAsString(row.getCell(0));
                    String description = getCellValueAsString(row.getCell(1));

                    if (label == null || label.isBlank()) {
                        log.warn("Skipping row {} due to empty label", row.getRowNum());
                        continue;
                    }

                    if (repository.findByLabel(label).isEmpty()) {
                        NatureLigneFonctionnelle nature = new NatureLigneFonctionnelle();
                        nature.setLabel(label);
                        nature.setDescription(description);
                        repository.save(nature);
                        log.info("Saved new NatureLigneFonctionnelle: '{}'", label);
                    } else {
                        log.debug("NatureLigneFonctionnelle with label '{}' already exists. Skipping.", label);
                    }
                }

                log.info("NatureLigneFonctionnelle data initialization complete.");

            } catch (Exception e) {
                log.error("Failed to initialize NatureLigneFonctionnelle data from XLSX: {}", e.getMessage(), e);
            }
        };
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        return dataFormatter.formatCellValue(cell);
    }
}