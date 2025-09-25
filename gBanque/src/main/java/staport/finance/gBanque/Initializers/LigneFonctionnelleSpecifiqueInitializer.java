package staport.finance.gBanque.Initializers;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.repositories.NatureLigneFonctionnelleRepository;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.dtos.LigneFonctionnelleSpecifiqueRequest;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.repositories.LigneFonctionnelleSpecifiqueRepository;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.services.LigneFonctionnelleSpecifiqueService;
import staport.finance.gBanque.marches.repositories.MarcheRepository;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.stream.StreamSupport;

@Component
@Order(10) // S'exécute après MarcheInitializer et les autres initialisateurs nécessaires
public class LigneFonctionnelleSpecifiqueInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(LigneFonctionnelleSpecifiqueInitializer.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final LigneFonctionnelleSpecifiqueService ligneFonctionnelleSpecifiqueService;
    private final LigneFonctionnelleSpecifiqueRepository ligneFonctionnelleSpecifiqueRepository;;
    private final BanqueRepository banqueRepository;
    private final NatureLigneFonctionnelleRepository natureLigneFonctionnelleRepository;
    private final MarcheRepository marcheRepository;
    private final DataFormatter dataFormatter = new DataFormatter();

    public LigneFonctionnelleSpecifiqueInitializer(
            LigneFonctionnelleSpecifiqueService ligneFonctionnelleSpecifiqueService, LigneFonctionnelleSpecifiqueRepository ligneFonctionnelleSpecifiqueRepository,
            BanqueRepository banqueRepository,
            NatureLigneFonctionnelleRepository natureLigneFonctionnelleRepository,
            MarcheRepository marcheRepository) {
        this.ligneFonctionnelleSpecifiqueService = ligneFonctionnelleSpecifiqueService;
        this.ligneFonctionnelleSpecifiqueRepository = ligneFonctionnelleSpecifiqueRepository;
        this.banqueRepository = banqueRepository;
        this.natureLigneFonctionnelleRepository = natureLigneFonctionnelleRepository;
        this.marcheRepository = marcheRepository;
    }

    @Override
    public void run(String... args) {

        if (ligneFonctionnelleSpecifiqueRepository.count() > 0) {
            logger.info("Banques already exist. Skipping initialization.");
            return;
        }

        logger.info("Démarrage de l'initialisation des lignes fonctionnelles spécifiques depuis le fichier XLSX...");

        String XLSX_FILE_PATH = "lignes_fonctionnelles_specifiques_data.xlsx";
        try (InputStream inputStream = new ClassPathResource(XLSX_FILE_PATH).getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);

            StreamSupport.stream(sheet.spliterator(), false)
                    .skip(1) // Ignore la ligne d'en-tête
                    .forEach(this::processRow);

            logger.info("Initialisation des lignes fonctionnelles spécifiques terminée avec succès.");
        } catch (Exception e) {
            logger.error("Échec de l'initialisation des lignes fonctionnelles spécifiques : {}", e.getMessage(), e);
        }
    }

    private void processRow(Row row) {
        try {
            // Extraction des valeurs de la ligne en utilisant les en-têtes du fichier CSV
            String banqueName = getCellValueAsString(row.getCell(0));
            String natureLibelle = getCellValueAsString(row.getCell(1));
            String marcheNumero = getCellValueAsString(row.getCell(6)); // `Numero de marche` est à la 7e colonne

            BigDecimal plafond = parseBigDecimal(getCellValueAsString(row.getCell(2)));
            BigDecimal taux = parseBigDecimal(getCellValueAsString(row.getCell(3)));
            OffsetDateTime dateDepart = parseDate(getCellValueAsString(row.getCell(4)));
            OffsetDateTime dateFin = parseDate(getCellValueAsString(row.getCell(5)));
            Boolean actif = parseBoolean(getCellValueAsString(row.getCell(7)));

            // Recherche des IDs à partir des noms/libellés
            Long banqueId = banqueRepository.findByNom(banqueName)
                    .orElseThrow(() -> new IllegalArgumentException("Banque non trouvée: " + banqueName))
                    .getId();

            Long natureLigneFonctionnelleId = natureLigneFonctionnelleRepository.findByLabel(natureLibelle)
                    .orElseThrow(() -> new IllegalArgumentException("Nature de ligne non trouvée: " + natureLibelle))
                    .getId();

            Long marcheId = marcheRepository.findByNumero(marcheNumero)
                    .orElseThrow(() -> new IllegalArgumentException("Marché non trouvé: " + marcheNumero))
                    .getId();


            LigneFonctionnelleSpecifiqueRequest dto = new LigneFonctionnelleSpecifiqueRequest();
            dto.setBanqueId(banqueId);
            dto.setNatureLigneFonctionnelleId(natureLigneFonctionnelleId);
            dto.setMarcheId(marcheId);
            dto.setPlafond(plafond);
            dto.setTaux(taux);
            dto.setDateDepart(dateDepart);
            dto.setDateFin(dateFin);
            dto.setActif(actif);

            ligneFonctionnelleSpecifiqueService.createLigneFonctionnelleSpecifique(dto);
            logger.info("Ligne fonctionnelle spécifique pour le Marché {} sauvegardée avec succès.", marcheNumero);

        } catch (Exception e) {
            logger.error("Erreur lors du traitement de la ligne {} : {}", row.getRowNum(), e.getMessage(), e);
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        return dataFormatter.formatCellValue(cell).trim();
    }

    private BigDecimal parseBigDecimal(String value) {
        try {
            if (value == null || value.isBlank()) return null;
            // Remplacement de la virgule par un point pour le parsing
            String sanitizedValue = value.replace(',', '.');
            return new BigDecimal(sanitizedValue);
        } catch (NumberFormatException e) {
            logger.error("Impossible de parser BigDecimal '{}'.", value);
            return null;
        }
    }

    private OffsetDateTime parseDate(String value) {
        try {
            if (value == null || value.isBlank()) return null;
            LocalDate localDate = LocalDate.parse(value, DATE_FORMATTER);
            return localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime();
        } catch (Exception e) {
            logger.error("Impossible de parser la date '{}'.", value);
            return null;
        }
    }

    private Boolean parseBoolean(String value) {
        if (value == null || value.isBlank()) return null;
        String lowerValue = value.toLowerCase();
        return "true".equals(lowerValue) || "vrai".equals(lowerValue) || "1".equals(lowerValue);
    }
}