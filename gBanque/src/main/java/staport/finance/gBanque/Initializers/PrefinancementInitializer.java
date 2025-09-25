package staport.finance.gBanque.Initializers;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.marches.repositories.MarcheRepository;
import staport.finance.gBanque.prefinancement.dtos.PrefinancementRequestDTO;
import staport.finance.gBanque.prefinancement.enums.Type;
import staport.finance.gBanque.prefinancement.repository.PrefinancementRepository;
import staport.finance.gBanque.prefinancement.services.PrefinancementService;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.stream.StreamSupport;

@Component
@Order(11)
public class PrefinancementInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(PrefinancementInitializer.class);
    private final PrefinancementService prefinancementService;
    private  final PrefinancementRepository prefinancementRepository;
    private final MarcheRepository marcheRepository;
    private final DataFormatter dataFormatter = new DataFormatter();

    public PrefinancementInitializer(PrefinancementService prefinancementService, PrefinancementRepository prefinancementRepository, MarcheRepository marcheRepository) {
        this.prefinancementService = prefinancementService;
        this.prefinancementRepository = prefinancementRepository;
        this.marcheRepository = marcheRepository;
    }

    @Override
    public void run(String... args) {

        if ( prefinancementRepository.count() > 0 ) {
            logger.info("Les encours des préfinancements already exist. Skipping initialization.");
            return;
        }

        logger.info("Démarrage de l'initialisation des préfinancements depuis le fichier XLSX...");
        String XLSX_FILE_PATH = "prefinancements_data.xlsx";

        try (InputStream inputStream = new ClassPathResource(XLSX_FILE_PATH).getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);

            StreamSupport.stream(sheet.spliterator(), false)
                    .skip(1) // Ignore la ligne d'en-tête
                    .forEach(this::processRow);

            logger.info("Initialisation des préfinancements terminée avec succès.");
        } catch (Exception e) {
            logger.error("Échec de l'initialisation des préfinancements : {}", e.getMessage(), e);
        }
    }

    private void processRow(Row row) {
        try {
            // Extraction des valeurs de la ligne
            String marcheNumero = getCellValueAsString(row.getCell(0));
            OffsetDateTime dateEmission = parseDate(getCellValueAsString(row.getCell(1)));
            BigDecimal montant = parseBigDecimal(getCellValueAsString(row.getCell(2)));
            Type type = parseType(getCellValueAsString(row.getCell(3)));

            // Validation de base
            if (marcheNumero == null || montant == null || type == null) {
                logger.warn("Ligne {} ignorée : Données obligatoires manquantes.", row.getRowNum());
                return;
            }

            // Recherche du marché par son numéro
            Marche marche = marcheRepository.findByNumero(marcheNumero)
                    .orElseThrow(() -> new IllegalArgumentException("Marché non trouvé avec le numéro: " + marcheNumero));

            // Création du DTO de requête
            PrefinancementRequestDTO dto = new PrefinancementRequestDTO();
            dto.setMarcheId(marche.getId());
            dto.setDateEmission(dateEmission);
            dto.setMontant(montant);
            dto.setType(type);

            // Appel du service pour créer l'entité
            prefinancementService.createPrefinancement(dto);
            logger.info("Préfinancement de type {} pour le marché {} sauvegardé avec succès.", type, marcheNumero);

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
            // Utiliser le parsing par défaut, comme dans MarcheInitializer
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
            logger.error("Type de préfinancement non valide: '{}'. Les valeurs acceptées sont {}", value, java.util.Arrays.toString(Type.values()));
            return null;
        }
    }
}