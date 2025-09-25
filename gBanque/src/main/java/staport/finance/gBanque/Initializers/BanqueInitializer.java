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
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.enums.Devise;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@Configuration
public class BanqueInitializer {

    private static final Logger log = LoggerFactory.getLogger(BanqueInitializer.class);

    private final DataFormatter dataFormatter = new DataFormatter();

    @Bean
    @Order(2)
    CommandLineRunner banqueDataInitializer(BanqueRepository repository) {
        return args -> {

            if (repository.count() > 0) {
                log.info("Banques already exist. Skipping initialization.");
                return;
            }

            log.info("Initializing Banque data from banques_data.xlsx...");

            String XLSX_FILE_PATH = "banques_data.xlsx";

            try (InputStream inputStream = new ClassPathResource(XLSX_FILE_PATH).getInputStream();
                 Workbook workbook = new XSSFWorkbook(inputStream)) {

                Sheet sheet = workbook.getSheetAt(0);

                // Skip header row and process each data row
                for (Row row : sheet) {
                    if (row.getRowNum() == 0) continue; // Skip header

                    String numeroDeCompte = getCellValueAsString(row.getCell(0));
                    String nom = getCellValueAsString(row.getCell(1));
                    String agence = getCellValueAsString(row.getCell(2));
                    String address = getCellValueAsString(row.getCell(3));
                    String logoPath = getCellValueAsString(row.getCell(4));

                    if (numeroDeCompte == null || numeroDeCompte.isBlank()) {
                        log.warn("Skipping row {} due to empty account number", row.getRowNum());
                        continue;
                    }

                    Optional<Banque> existingBanque = repository.findByNumeroDeCompte(numeroDeCompte);
                    if (existingBanque.isPresent()) {
                        log.debug("Banque '{}' with account number '{}' already exists. Skipping.", nom, numeroDeCompte);
                        continue;
                    }

                    Banque banque = createBanque(numeroDeCompte, nom, agence, address, logoPath);
                    repository.save(banque);
                    log.info("Saved Banque: '{}' ({})", banque.getNom(), banque.getNumeroDeCompte());
                }

                log.info("Banque data initialization complete.");

            } catch (Exception e) {
                log.error("Failed to initialize Banque data from XLSX: {}", e.getMessage(), e);
            }
        };
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        return dataFormatter.formatCellValue(cell);
    }

    private Banque createBanque(String numeroDeCompte, String nom, String agence, String address,
                                String logoResourcePath) {
        Banque banque = new Banque();
        try {
            byte[] logoData = loadLogoAsByteArray(logoResourcePath);
            banque.setLogoData(logoData);
        } catch (IOException e) {
            log.error("Failed to load logo '{}' for bank '{}'. Error: {}", logoResourcePath, nom, e.getMessage());
            banque.setLogoData(new byte[0]); // fallback
        }

        banque.setNumeroDeCompte(numeroDeCompte);
        banque.setNom(nom);
        banque.setDevise(Devise.MAD);
        banque.setAgence(agence);
        banque.setAddress(address);

        return banque;
    }

    private byte[] loadLogoAsByteArray(String resourcePath) throws IOException {
        ClassPathResource classPathResource = new ClassPathResource(resourcePath);
        if (!classPathResource.exists()) {
            throw new IOException("Resource not found: " + resourcePath);
        }
        try (InputStream inputStream = classPathResource.getInputStream()) {
            return inputStream.readAllBytes();
        }
    }
}
