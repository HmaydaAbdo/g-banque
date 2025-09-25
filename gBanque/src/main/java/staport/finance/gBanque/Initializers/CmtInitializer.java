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
import staport.finance.gBanque.cmt.dtos.CreditMoyenTermeRequestDto;
import staport.finance.gBanque.cmt.repositories.CreditMoyenTermeRepository;
import staport.finance.gBanque.cmt.services.CreditMoyenTermeService;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Component
@Order(7)
    public class CmtInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(CmtInitializer.class);
    private final CreditMoyenTermeService cmtService;
    private final BanqueRepository banqueRepository;
    private final DataFormatter dataFormatter = new DataFormatter();
    private final CreditMoyenTermeRepository cmtRepository;

    private static final DateTimeFormatter[] DATE_FORMATTERS = new DateTimeFormatter[]{
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("M/d/yy") // Added to handle inconsistent Excel date formats
    };

    public CmtInitializer(CreditMoyenTermeService cmtService, BanqueRepository banqueRepository, CreditMoyenTermeRepository cmtRepository) {
        this.cmtService = cmtService;
        this.banqueRepository = banqueRepository;
        this.cmtRepository = cmtRepository;
    }

    @Override
    public void run(String... args) {

        if (cmtRepository.count() > 0) {
            logger.info("les crédits moyens termes already exist. Skipping initialization.");
            return;
        }

        logger.info("Démarrage de l'initialisation des données de crédit à moyen terme depuis le fichier XLSX...");

        String XLSX_FILE_PATH = "cmt.xlsx";
        try (InputStream inputStream = new ClassPathResource(XLSX_FILE_PATH).getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);

            StreamSupport.stream(sheet.spliterator(), false)
                    .skip(1) // Ignore header row
                    .forEach(this::processRow);

            logger.info("Initialisation des données de crédit à moyen terme terminée.");
        } catch (Exception e) {
            logger.error("Échec de l'initialisation des données de crédit à moyen terme : {}", e.getMessage(), e);
        }
    }

    private void processRow(Row row) {
        try {
            String banqueNom = getCellValueAsString(row.getCell(0));
            if (banqueNom == null || banqueNom.isBlank()) {
                logger.warn("Ligne ignorée en raison d'un nom de banque vide à la ligne {}.", row.getRowNum());
                return;
            }

            Optional<Banque> banqueOpt = banqueRepository.findByNom(banqueNom);
            if (banqueOpt.isEmpty()) {
                logger.error("Banque introuvable pour le nom '{}'. Ligne ignorée: {}", banqueNom, row.getRowNum());
                return;
            }
            Banque banque = banqueOpt.get();

            CreditMoyenTermeRequestDto dto = new CreditMoyenTermeRequestDto();
            dto.setBanqueId(banque.getId());
            dto.setDesignation(getCellValueAsString(row.getCell(1)));
            dto.setNominal(parseBigDecimal(getCellValueAsString(row.getCell(2))));
            dto.setPeriodicite(parseInteger(getCellValueAsString(row.getCell(3))));
            dto.setDuree(parseInteger(getCellValueAsString(row.getCell(4))));
            dto.setEcheance(parseBigDecimal(getCellValueAsString(row.getCell(5))));
            dto.setTaux(parseBigDecimal(getCellValueAsString(row.getCell(6))));

            // This is the line that now calls the improved parsing method
            dto.setDateDebut(parseDate(getCellValueAsString(row.getCell(7))));

            cmtService.create(dto);
            logger.info("Crédit à moyen terme pour la banque '{}' sauvegardé avec succès.", banqueNom);

        } catch (Exception e) {
            logger.error("Une erreur s'est produite lors du traitement de la ligne {}: {}", row.getRowNum(), e.getMessage(), e);
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }
        return dataFormatter.formatCellValue(cell);
    }

    private BigDecimal parseBigDecimal(String value) {
        try {
            if (value == null || value.isBlank()) return BigDecimal.ZERO;
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            logger.error("Échec de l'analyse BigDecimal de la chaîne '{}'. Retour à zéro.", value);
            return BigDecimal.ZERO;
        }
    }

    private Integer parseInteger(String value) {
        try {
            if (value == null || value.isBlank()) return 0;
            return (int) Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            logger.error("Échec de l'analyse Integer de la chaîne '{}'. Retour à zéro.", value);
            return 0;
        }
    }

    private OffsetDateTime parseDate(String dateString) {
        if (dateString == null || dateString.isBlank()) {
            return null;
        }

        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                LocalDate localDate = LocalDate.parse(dateString.trim(), formatter);
                return localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime();
            } catch (DateTimeParseException e) {
                // Try next formatter
            }
        }

        logger.error("Failed to parse date string '{}' with supported formats.", dateString);
        return null;
    }
}