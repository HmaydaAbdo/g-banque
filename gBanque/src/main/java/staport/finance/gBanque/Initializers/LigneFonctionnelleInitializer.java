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
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.repositories.NatureLigneFonctionnelleRepository;
import staport.finance.gBanque.LigneFonctionnelle.dtos.LigneFonctionnelleRequest;
import staport.finance.gBanque.LigneFonctionnelle.services.LigneFonctionnelleService;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.stream.StreamSupport;

@Component
@Order(4)
public class LigneFonctionnelleInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(LigneFonctionnelleInitializer.class);
    private final LigneFonctionnelleService ligneFonctionnelleService;
    private final BanqueRepository banqueRepository;
    private final NatureLigneFonctionnelleRepository natureLigneFonctionnelleRepository;
    private final DataFormatter dataFormatter = new DataFormatter();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public LigneFonctionnelleInitializer(LigneFonctionnelleService ligneFonctionnelleService,
                                         BanqueRepository banqueRepository,
                                         NatureLigneFonctionnelleRepository natureLigneFonctionnelleRepository) {
        this.ligneFonctionnelleService = ligneFonctionnelleService;
        this.banqueRepository = banqueRepository;
        this.natureLigneFonctionnelleRepository = natureLigneFonctionnelleRepository;
    }

    @Override
    public void run(String... args) {
        logger.info("Démarrage de l'initialisation des lignes fonctionnelles depuis le fichier XLSX...");
        String XLSX_FILE_PATH = "lignes_fonctionnelles_data.xlsx";

        try (InputStream inputStream = new ClassPathResource(XLSX_FILE_PATH).getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            StreamSupport.stream(sheet.spliterator(), false)
                    .skip(1) // Ignore la ligne d'en-tête
                    .forEach(this::processRow);

            logger.info("Initialisation des lignes fonctionnelles terminée avec succès.");
        } catch (Exception e) {
            logger.error("Échec de l'initialisation des lignes fonctionnelles : {}", e.getMessage(), e);
        }
    }

    private void processRow(Row row) {
        try {
            // Lecture des colonnes (index 0 à 5)
            String banqueNom = getCellValueAsString(row.getCell(0));
            String natureLibelle = getCellValueAsString(row.getCell(1));
            BigDecimal plafond = parseBigDecimal(getCellValueAsString(row.getCell(2)));
            BigDecimal taux = parseBigDecimal(getCellValueAsString(row.getCell(3)));
            OffsetDateTime dateDepart = parseDate(getCellValueAsString(row.getCell(4)));
            OffsetDateTime dateFin = parseDate(getCellValueAsString(row.getCell(5)));

            if (banqueNom == null || natureLibelle == null || plafond == null || taux == null || dateDepart == null || dateFin == null) {
                logger.warn("Ligne {} ignorée : Données obligatoires manquantes.", row.getRowNum());
                return;
            }

            Banque banque = banqueRepository.findByNom(banqueNom)
                    .orElseThrow(() -> new IllegalArgumentException("Banque non trouvée avec le nom: " + banqueNom));

            NatureLigneFonctionnelle natureLigne = natureLigneFonctionnelleRepository.findByLabel(natureLibelle)
                    .orElseThrow(() -> new IllegalArgumentException("Nature de Ligne Fonctionnelle non trouvée avec le label: " + natureLibelle));


            LigneFonctionnelleRequest dto = new LigneFonctionnelleRequest();
            dto.setBanqueId(banque.getId());
            dto.setNatureLigneFonctionnelleId(natureLigne.getId());
            dto.setPlafond(plafond);
            dto.setTaux(taux);
            dto.setDateDepart(dateDepart);
            dto.setDateFin(dateFin);

            ligneFonctionnelleService.createLigneFonctionnelle(dto);
            logger.info("Ligne fonctionnelle (Type: {}) pour la banque {} sauvegardée avec succès.", natureLibelle, banqueNom);

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
            if (value == null || value.isBlank()) return null;
            return new BigDecimal(value.replace(',', '.').trim());
        } catch (NumberFormatException e) {
            logger.error("Impossible de parser BigDecimal '{}'.", value);
            return null;
        }
    }

    private OffsetDateTime parseDate(String value) {
        try {
            if (value == null || value.isBlank()) return null;
            // Supposons que les dates dans le fichier sont au format 'yyyy-MM-dd' ou 'dd/MM/yyyy'
            LocalDate localDate;
            if (value.contains("-")) {
                localDate = LocalDate.parse(value.trim()); // Utilise le format ISO (yyyy-MM-dd)
            } else {
                localDate = LocalDate.parse(value.trim(), dateFormatter); // Utilise le format dd/MM/yyyy
            }
            return localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime();
        } catch (Exception e) {
            logger.error("Impossible de parser la date '{}'.", value);
            return null;
        }
    }
}