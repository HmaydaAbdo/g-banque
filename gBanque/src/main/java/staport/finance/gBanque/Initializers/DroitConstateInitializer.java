package staport.finance.gBanque.Initializers;


import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.droitConstate.dtos.DroitConstateRequestDto;
import staport.finance.gBanque.droitConstate.repositories.DroitConstateRepository;
import staport.finance.gBanque.droitConstate.service.DroitConstateService;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.marches.repositories.MarcheRepository;
import staport.finance.gBanque.prefinancement.enums.Type;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.stream.StreamSupport;

@Component
@Order(12)
public class DroitConstateInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DroitConstateInitializer.class);
    private final DroitConstateService droitConstateService;
    private final DroitConstateRepository droitConstateRepository;
    private final MarcheRepository marcheRepository;
    private final DataFormatter dataFormatter = new DataFormatter();

    public DroitConstateInitializer(DroitConstateService droitConstateService, DroitConstateRepository droitConstateRepository, MarcheRepository marcheRepository) {
        this.droitConstateService = droitConstateService;
        this.droitConstateRepository = droitConstateRepository;
        this.marcheRepository = marcheRepository;
    }

    @Override
    public void run(String... args) {

        if (droitConstateRepository.count() > 0) {
            logger.info("Les encours des droits constatés already exist. Skipping initialization.");
            return;
        }

        logger.info("Démarrage de l'initialisation des droits constatés depuis le fichier XLSX...");
        String XLSX_FILE_PATH = "droits_constates_data.xlsx";

        try (InputStream inputStream = new ClassPathResource(XLSX_FILE_PATH).getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            StreamSupport.stream(sheet.spliterator(), false)
                    .skip(1) // Ignore la ligne d'en-tête
                    .forEach(this::processRow);

            logger.info("Initialisation des droits constatés terminée avec succès.");
        } catch (Exception e) {
            logger.error("Échec de l'initialisation des droits constatés : {}", e.getMessage(), e);
        }
    }

    private void processRow(Row row) {
        try {
            String marcheNumero = getCellValueAsString(row.getCell(0));
            OffsetDateTime dateEmission = parseDate(getCellValueAsString(row.getCell(1)));
            BigDecimal montant = parseBigDecimal(getCellValueAsString(row.getCell(2)));
            Type type = parseType(getCellValueAsString(row.getCell(3)));

            if (marcheNumero == null || montant == null || type == null) {
                logger.warn("Ligne {} ignorée : Données obligatoires manquantes.", row.getRowNum());
                return;
            }

            Marche marche = marcheRepository.findByNumero(marcheNumero)
                    .orElseThrow(() -> new IllegalArgumentException("Marché non trouvé avec le numéro: " + marcheNumero));

            DroitConstateRequestDto dto = new DroitConstateRequestDto();
            dto.setMarcheId(marche.getId());
            dto.setDateEmission(dateEmission);
            dto.setMontant(montant);
            dto.setType(type);

            droitConstateService.createDroitConstate(dto);
            logger.info("Droit constaté de type {} pour le marché {} sauvegardé avec succès.", type, marcheNumero);

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

    private Type parseType(String value) {
        try {
            if (value == null || value.isBlank()) return null;
            return Type.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.error("Type de droit constaté non valide: '{}'. Les valeurs acceptées sont {}", value, java.util.Arrays.toString(Type.values()));
            return null;
        }
    }
}