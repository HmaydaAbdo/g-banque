package staport.finance.gBanque.Initializers;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.marches.dtos.MarcheRequestDTO;
import staport.finance.gBanque.marches.repositories.MarcheRepository;
import staport.finance.gBanque.marches.service.MarcheService;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.stream.StreamSupport;

@Component
@Order(9)
public class MarcheInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(MarcheInitializer.class);

    private final MarcheService marcheService;
    private final MarcheRepository repository ;
    private final DataFormatter dataFormatter = new DataFormatter();

    public MarcheInitializer(MarcheService marcheService, MarcheRepository repository) {
        this.marcheService = marcheService;
        this.repository = repository;
    }

    @Override
    public void run(String... args) {

        if (repository.count() > 0) {
            logger.info("Les Marchés already exist. Skipping initialization.");
            return;
        }

        logger.info("Démarrage de l'initialisation des marchés depuis le fichier XLSX...");

        String XLSX_FILE_PATH = "marches_data.xlsx";
        try (InputStream inputStream = new ClassPathResource(XLSX_FILE_PATH).getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);

            StreamSupport.stream(sheet.spliterator(), false)
                    .skip(1) // Ignore header row
                    .forEach(this::processRow);

            logger.info("Initialisation des marchés terminée avec succès.");
        } catch (Exception e) {
            logger.error("Échec de l'initialisation des marchés : {}", e.getMessage(), e);
        }
    }

    private void processRow(Row row) {
        try {
            String numero = getCellValueAsString(row.getCell(0));
            String nom = getCellValueAsString(row.getCell(1));
            BigDecimal montant = parseBigDecimal(getCellValueAsString(row.getCell(2)));
            String maitreDouvrage = getCellValueAsString(row.getCell(3));

            OffsetDateTime dateDebut = parseDate(getCellValueAsString(row.getCell(4)));
            OffsetDateTime dateFin = parseDate(getCellValueAsString(row.getCell(5)));

            if (numero == null || numero.isBlank()) {
                logger.warn("Ligne {} ignorée : numero vide.", row.getRowNum());
                return;
            }

            MarcheRequestDTO dto = new MarcheRequestDTO();
            dto.setNumero(numero);
            dto.setNom(nom);
            dto.setMontant(montant);
            dto.setMaitreDouvrage(maitreDouvrage);
            dto.setDateDebutDesTravaux(dateDebut);
            dto.setDateDAchievementDesTravaux(dateFin);

            marcheService.createMarche(dto);
            logger.info("Marché {} sauvegardé avec succès.", numero);

        } catch (Exception e) {
            logger.error("Erreur lors du traitement de la ligne {} : {}", row.getRowNum(), e.getMessage());
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        return dataFormatter.formatCellValue(cell);
    }

    private BigDecimal parseBigDecimal(String value) {
        try {
            if (value == null || value.isBlank()) return BigDecimal.ZERO;
            // Replace comma with dot
            value = value.trim().replace(",", ".");
            return new BigDecimal(value);
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
}
