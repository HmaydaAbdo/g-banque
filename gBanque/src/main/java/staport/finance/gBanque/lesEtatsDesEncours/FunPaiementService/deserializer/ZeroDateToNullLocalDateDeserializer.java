    package staport.finance.gBanque.lesEtatsDesEncours.FunPaiementService.deserializer;

    import com.fasterxml.jackson.core.JsonParser;
    import com.fasterxml.jackson.databind.DeserializationContext;
    import com.fasterxml.jackson.databind.JsonDeserializer;

    import java.io.IOException;
    import java.time.LocalDate;
    import java.time.format.DateTimeFormatter;
    import java.time.format.DateTimeParseException;

    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;

    public class ZeroDateToNullLocalDateDeserializer extends JsonDeserializer<LocalDate> {


        private static final Logger log = LoggerFactory.getLogger(ZeroDateToNullLocalDateDeserializer.class);

        // This formatter is used for *valid* date strings like "2025-07-31"
        private static final DateTimeFormatter VALID_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        @Override
        public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String dateAsString = p.getText();

            if (dateAsString == null || dateAsString.trim().isEmpty() || "0000-00-00".equals(dateAsString)) {
                log.debug("Deserializing date string '{}' to null.", dateAsString);
                return null; // For null, empty, or "0000-00-00" cases, deserialize to null
            }

            try {
                // For other (valid) date strings, parse them as LocalDate
                java.time.LocalDate localDate = java.time.LocalDate.parse(dateAsString, VALID_DATE_FORMATTER);
                // Convert to OffsetDateTime by assuming midnight UTC (or your desired default offset)
                log.debug("Successfully deserialized date string '{}' to LocalDate: {}", dateAsString, localDate);
                return localDate;
            } catch (DateTimeParseException e) {
                log.warn("Could not parse date string '{}' with pattern 'yyyy-MM-dd'. Returning null. Error: {}", dateAsString, e.getMessage());
                // If any other unexpected invalid date format appears, also return null
                return null;
            }
        }
    }