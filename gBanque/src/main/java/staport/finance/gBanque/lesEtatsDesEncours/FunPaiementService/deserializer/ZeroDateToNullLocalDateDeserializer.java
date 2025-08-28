package staport.finance.gBanque.lesEtatsDesEncours.FunPaiementService.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;

public class ZeroDateToNullLocalDateDeserializer extends JsonDeserializer<LocalDate> {

    private static final Logger log = LoggerFactory.getLogger(ZeroDateToNullLocalDateDeserializer.class);

    @Override
    public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String dateAsString = p.getText();

        if (dateAsString == null || dateAsString.trim().isEmpty() || "0000-00-00".equals(dateAsString)) {
            log.debug("Intercepted invalid date string '{}', deserializing to null.", dateAsString);
            return null;
        }

        // Laisse Jackson parser normalement si ce n’est pas "0000-00-00"
        return LocalDate.parse(dateAsString);
    }
}
