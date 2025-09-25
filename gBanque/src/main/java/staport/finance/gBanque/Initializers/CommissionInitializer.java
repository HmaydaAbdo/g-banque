package staport.finance.gBanque.Initializers;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.commision.dtos.CommissionRequestDto;
import staport.finance.gBanque.commision.repository.CommisionRepository;
import staport.finance.gBanque.commision.service.CommissionService;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.stream.StreamSupport;

@Component
@Order(14)
public class CommissionInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(CommissionInitializer.class);
    private final CommissionService commissionService;
    private final CommisionRepository commissionRepository;
    private final BanqueRepository banqueRepository;
    private final DataFormatter dataFormatter = new DataFormatter();

    public CommissionInitializer(CommissionService commissionService, CommisionRepository commissionRepository, BanqueRepository banqueRepository) {
        this.commissionService = commissionService;
        this.commissionRepository = commissionRepository;
        this.banqueRepository = banqueRepository;
    }

    @Override
    public void run(String... args) {

        if (commissionRepository.count() > 0) {
            logger.info("Les commissions  already exist. Skipping initialization.");
            return;
        }
        logger.info("Démarrage de l'initialisation des commissions depuis le fichier XLSX...");
        String XLSX_FILE_PATH = "commissions_data.xlsx";

        try (InputStream inputStream = new ClassPathResource(XLSX_FILE_PATH).getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            StreamSupport.stream(sheet.spliterator(), false)
                    .skip(1) // Ignore la ligne d'en-tête
                    .forEach(this::processRow);

            logger.info("Initialisation des commissions terminée avec succès.");
        } catch (Exception e) {
            logger.error("Échec de l'initialisation des commissions : {}", e.getMessage(), e);
        }
    }

    private void processRow(Row row) {
        try {
            String libelle = getCellValueAsString(row.getCell(0));
            BigDecimal montant = parseBigDecimal(getCellValueAsString(row.getCell(1)));
            String banqueNom = getCellValueAsString(row.getCell(2));
            TransactionSource operation = parseOperation(getCellValueAsString(row.getCell(3)));
            OffsetDateTime dateEmission = parseDate(getCellValueAsString(row.getCell(4)));

            if (libelle == null || montant == null || banqueNom == null || operation == null || dateEmission == null) {
                logger.warn("Ligne {} ignorée : Données obligatoires manquantes.", row.getRowNum());
                return;
            }

            Banque banque = banqueRepository.findByNom(banqueNom)
                    .orElseThrow(() -> new IllegalArgumentException("Banque non trouvée avec le nom: " + banqueNom));

            CommissionRequestDto dto = new CommissionRequestDto();
            dto.setLibelle(libelle);
            dto.setMontant(montant);
            dto.setBanqueId(banque.getId());
            dto.setOperation(operation);
            dto.setDateEmission(dateEmission);

            commissionService.createCommission(dto);
            logger.info("Commission '{}' pour la banque '{}' sauvegardée avec succès.", libelle, banqueNom);

        } catch (Exception e) {
            logger.error("Erreur lors du traitement de la ligne {} : {}", row.getRowNum(), e.getMessage());
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        return dataFormatter.formatCellValue(cell).trim();
    }

    private BigDecimal parseBigDecimal(String value) {
        try {
            if (value == null || value.isBlank()) return BigDecimal.ZERO;
            return new BigDecimal(value.replace(',', '.').trim());
        } catch (NumberFormatException e) {
            logger.error("Impossible de parser BigDecimal '{}'.", value);
            return BigDecimal.ZERO;
        }
    }

    private OffsetDateTime parseDate(String value) {
        try {
            if (value == null || value.isBlank()) return null;
            LocalDate localDate = LocalDate.parse(value.trim());
            return localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime();
        } catch (Exception e) {
            logger.error("Impossible de parser la date '{}'.", value);
            return null;
        }
    }

    private TransactionSource parseOperation(String value) {
        try {
            if (value == null || value.isBlank()) return null;
            return TransactionSource.valueOf(value);
        } catch (IllegalArgumentException e) {
            logger.error("Type d'opération non valide: '{}'. Les valeurs acceptées sont {}", value, java.util.Arrays.toString(TransactionSource.values()));
            return null;
        }
    }
}