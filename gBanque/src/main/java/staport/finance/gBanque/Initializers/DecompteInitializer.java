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
import staport.finance.gBanque.decomptes.dto.DecompteRequestDto;
import staport.finance.gBanque.decomptes.repositories.DecompteRepository;
import staport.finance.gBanque.decomptes.service.DecompteService;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.marches.repositories.MarcheRepository;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.stream.StreamSupport;

@Component
@Order(13)
public class DecompteInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DecompteInitializer.class);
    private final DecompteService decompteService;
    private final DecompteRepository decompteRepository;
    private final MarcheRepository marcheRepository;
    private final BanqueRepository banqueRepository;
    private final DataFormatter dataFormatter = new DataFormatter();

    public DecompteInitializer(DecompteService decompteService, DecompteRepository decompteRepository, MarcheRepository marcheRepository, BanqueRepository banqueRepository) {
        this.decompteService = decompteService;
        this.decompteRepository = decompteRepository;
        this.marcheRepository = marcheRepository;
        this.banqueRepository = banqueRepository;
    }

    @Override
    public void run(String... args) {

        if (decompteRepository.count() > 0) {
            logger.info("Les décomptes already exist. Skipping initialization.");
            return;
        }

        logger.info("Démarrage de l'initialisation des décomptes depuis le fichier XLSX...");
        String XLSX_FILE_PATH = "decomptes_data.xlsx";

        try (InputStream inputStream = new ClassPathResource(XLSX_FILE_PATH).getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            StreamSupport.stream(sheet.spliterator(), false)
                    .skip(1) // Ignore la ligne d'en-tête
                    .forEach(this::processRow);

            logger.info("Initialisation des décomptes terminée avec succès.");
        } catch (Exception e) {
            logger.error("Échec de l'initialisation des décomptes : {}", e.getMessage(), e);
        }
    }

    private void processRow(Row row) {
        try {
            String marcheNumero = getCellValueAsString(row.getCell(0));
            String banqueNom = getCellValueAsString(row.getCell(1));
            OffsetDateTime dateEmission = parseDate(getCellValueAsString(row.getCell(2)));
            BigDecimal montant = parseBigDecimal(getCellValueAsString(row.getCell(3)));

            if (marcheNumero == null || banqueNom == null || dateEmission == null || montant == null) {
                logger.warn("Ligne {} ignorée : Données obligatoires manquantes.", row.getRowNum());
                return;
            }

            Marche marche = marcheRepository.findByNumero(marcheNumero)
                    .orElseThrow(() -> new IllegalArgumentException("Marché non trouvé avec le numéro: " + marcheNumero));

            Banque banque = banqueRepository.findByNom(banqueNom)
                    .orElseThrow(() -> new IllegalArgumentException("Banque non trouvée avec le nom: " + banqueNom));

            DecompteRequestDto dto = new DecompteRequestDto();
            dto.setMarcheId(marche.getId());
            dto.setBanqueId(banque.getId());
            dto.setDateEmission(dateEmission);
            dto.setMontant(montant);

            decompteService.createDecompte(dto);
            logger.info("Décompte pour le marché {} et la banque {} sauvegardé avec succès.", marcheNumero, banqueNom);

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
}