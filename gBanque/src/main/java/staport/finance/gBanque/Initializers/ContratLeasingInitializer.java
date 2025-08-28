package staport.finance.gBanque.Initializers;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.leasing.contratLeasing.dto.ContratLeasingPhaseRequestDTO;
import staport.finance.gBanque.leasing.contratLeasing.dto.ContratLeasingRequestDTO;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;
import staport.finance.gBanque.leasing.contratLeasing.enums.TypeContratLeasing;
import staport.finance.gBanque.leasing.contratLeasing.repositories.ContratLeasingRepository;
import staport.finance.gBanque.leasing.contratLeasing.service.ContratLeasingService;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.entities.OrganismeLeasing;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.repositories.OrganismeLeasingRepository;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Configuration
@Order(6)
public class ContratLeasingInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ContratLeasingInitializer.class);

    private static final DateTimeFormatter DATE_FORMATTER_YMD = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_FORMATTER_MDY = DateTimeFormatter.ofPattern("M/d/yy");

    private final ContratLeasingService contratLeasingService;
    private final ContratLeasingRepository contratLeasingRepository;
    private final BanqueRepository banqueRepository;
    private final OrganismeLeasingRepository organismeLeasingRepository;

    public ContratLeasingInitializer(
            ContratLeasingService contratLeasingService,
            ContratLeasingRepository contratLeasingRepository,
            BanqueRepository banqueRepository,
            OrganismeLeasingRepository organismeLeasingRepository) {
        this.contratLeasingService = contratLeasingService;
        this.contratLeasingRepository = contratLeasingRepository;
        this.banqueRepository = banqueRepository;
        this.organismeLeasingRepository = organismeLeasingRepository;
    }

    @Override
    public void run(String... args) {
        log.info("Initializing LINEAR ContratLeasing data from XLSX file...");

        Map<String, Long> banqueIdCache = new HashMap<>();
        Map<String, Long> organismeIdCache = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        DataFormatter dataFormatter = new DataFormatter();

        try (InputStream inputStream = new ClassPathResource("leasing.xlsx").getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);

            StreamSupport.stream(sheet.spliterator(), false)
                    .skip(1)
                    .forEach(row -> {
                        String numeroContrat = null;
                        try {
                            numeroContrat = getCellValueAsString(row.getCell(0), dataFormatter);
                            if (numeroContrat == null || numeroContrat.isBlank()) {
                                log.warn("Skipping line with empty contract number at row {}.", row.getRowNum());
                                return;
                            }

                            Optional<ContratLeasing> existingContract = contratLeasingRepository.findByNumeroContrat(numeroContrat);

                            if (existingContract.isPresent()) {
                                log.debug("Contract '{}' already exists. Skipping.", numeroContrat);
                            } else {
                                var dto = new ContratLeasingRequestDTO();

                                dto.setNumeroContrat(numeroContrat);
                                dto.setBanqueDePrelevementId(getBanqueId(getCellValueAsString(row.getCell(1), dataFormatter), banqueIdCache));
                                dto.setOrganismeLeasingId(getOrganismeId(getCellValueAsString(row.getCell(2), dataFormatter), organismeIdCache));
                                dto.setImmobilisation(getCellValueAsString(row.getCell(3), dataFormatter));
                                dto.setMontantAchatTTC(new BigDecimal(cleanNumericString(getCellValueAsString(row.getCell(4), dataFormatter))));

                                String dateDebutStr = getCellValueAsString(row.getCell(5), dataFormatter);
                                dto.setDateDebut(parseDate(dateDebutStr));

                                String periodeEnMoisStr = cleanNumericString(getCellValueAsString(row.getCell(6), dataFormatter));
                                dto.setPeriodeEnMois((int) Double.parseDouble(periodeEnMoisStr));

                                dto.setTypeContrat(TypeContratLeasing.valueOf(getCellValueAsString(row.getCell(7), dataFormatter).toUpperCase()));
                                dto.setValeurResiduelle(new BigDecimal(cleanNumericString(getCellValueAsString(row.getCell(8), dataFormatter))));

                                String phasesJson = getCellValueAsString(row.getCell(9), dataFormatter);
                                if (phasesJson != null && !phasesJson.isBlank()) {
                                    phasesJson = phasesJson.replace("\"\"", "\"");
                                    List<ContratLeasingPhaseRequestDTO> phases = objectMapper.readValue(phasesJson, new TypeReference<>() {});
                                    dto.setPhases(phases);
                                }

                                contratLeasingService.createContratLeasing(dto);
                                log.info("Successfully created new contract: '{}'", dto.getNumeroContrat());
                            }
                        } catch (ValidationException e) {
                            log.error("Validation failed for contract '{}' at row {}: {}", numeroContrat, row.getRowNum(), e.getMessage());
                        } catch (JsonMappingException | JsonParseException e) {
                            String phasesJson = getCellValueAsString(row.getCell(9), dataFormatter);
                            log.error("Failed to parse JSON for phases of contract '{}' at row {}. The JSON string was: '{}'. Please correct the format.", numeroContrat, row.getRowNum(), phasesJson, e);
                        } catch (Exception e) {
                            log.error("An error occurred while processing contract '{}' at row {}: {}", numeroContrat, row.getRowNum(), e.getMessage(), e);
                        }
                    });

            log.info("ContratLeasing data initialization complete.");
        } catch (Exception e) {
            log.error("An unexpected error occurred during XLSX file processing: {}", e.getMessage(), e);
        }
    }

    private OffsetDateTime parseDate(String dateString) {
        if (dateString == null || dateString.isBlank()) {
            return null;
        }
        try {
            LocalDate localDate = LocalDate.parse(dateString, DATE_FORMATTER_YMD);
            return localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime();
        } catch (DateTimeParseException e1) {
            try {
                LocalDate localDate = LocalDate.parse(dateString, DATE_FORMATTER_MDY);
                return localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime();
            } catch (DateTimeParseException e2) {
                log.error("Failed to parse date string '{}' with both 'yyyy-MM-dd' and 'M/d/yy' formats.", dateString);
                return null;
            }
        }
    }

    private String getCellValueAsString(Cell cell, DataFormatter dataFormatter) {
        if (cell == null) {
            return null;
        }
        return dataFormatter.formatCellValue(cell);
    }

    private String cleanNumericString(String numericString) {
        if (numericString == null) {
            return "0";
        }
        return numericString.trim().replace(",", ".").replaceAll("[^\\d.]", "");
    }

    private Long getBanqueId(String nomBanque, Map<String, Long> cache) {
        return cache.computeIfAbsent(nomBanque, k -> banqueRepository.findByNom(k).map(Banque::getId).orElseThrow(() ->
                new IllegalStateException("Banque with name '" + k + "' not found. Cannot initialize contracts."))
        );
    }

    private Long getOrganismeId(String nomOrganisme, Map<String, Long> cache) {
        return cache.computeIfAbsent(nomOrganisme, k -> organismeLeasingRepository.findByNom(k).map(OrganismeLeasing::getId).orElseThrow(() ->
                new IllegalStateException("OrganismeLeasing with name '" + k + "' not found. Cannot initialize contracts."))
        );
    }
}