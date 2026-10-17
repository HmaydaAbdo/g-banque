package staport.finance.gBanque.recapitulatif.mappers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.recapitulatif.dtos.response.RecaputilatifItem;
import staport.finance.gBanque.recapitulatif.entities.RecapitulatifEncoursId;
import staport.finance.gBanque.recapitulatif.entities.RecapitulatifEncoursView;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class RecapitulatifEncoursMapperTest {

    private final RecapitulatifEncoursMapper mapper = Mappers.getMapper(RecapitulatifEncoursMapper.class);

    @Test
    @DisplayName("La date de référence est exposée même quand la source n'a pas de date d'échéance (chèque, virement, commission…)")
    void dateReferenceExposeeSansEcheance() {
        RecapitulatifEncoursView vue = BeanUtils.instantiateClass(RecapitulatifEncoursView.class);
        ReflectionTestUtils.setField(vue, "sens", Type.DECAISSEMENT);
        ReflectionTestUtils.setField(vue, "montant", new BigDecimal("100.00"));
        ReflectionTestUtils.setField(vue, "dateEmission", LocalDate.of(2026, 10, 12));
        ReflectionTestUtils.setField(vue, "dateReference", LocalDate.of(2026, 10, 12));
        ReflectionTestUtils.setField(vue, "dateEcheanceAffichage", null);
        ReflectionTestUtils.setField(vue, "id", identifiant(TransactionSource.CHEQUE));

        RecaputilatifItem item = mapper.toDto(vue);

        assertThat(item.dateEcheance()).isNull();
        assertThat(item.dateReference()).isEqualTo(LocalDate.of(2026, 10, 12));
        assertThat(item.debit()).isEqualByComparingTo("100.00");
        assertThat(item.credit()).isNull();
    }

    @Test
    @DisplayName("LCN escompte : la date de référence est l'émission, distincte de l'échéance affichée")
    void dateReferenceDistincteDeLEcheance() {
        RecapitulatifEncoursView vue = BeanUtils.instantiateClass(RecapitulatifEncoursView.class);
        ReflectionTestUtils.setField(vue, "sens", Type.ENCAISSEMENT);
        ReflectionTestUtils.setField(vue, "montant", new BigDecimal("50.00"));
        ReflectionTestUtils.setField(vue, "dateEmission", LocalDate.of(2026, 10, 1));
        ReflectionTestUtils.setField(vue, "dateReference", LocalDate.of(2026, 10, 1));
        ReflectionTestUtils.setField(vue, "dateEcheanceAffichage", LocalDate.of(2026, 12, 31));
        ReflectionTestUtils.setField(vue, "id", identifiant(TransactionSource.LCN_ESCOMPTE));

        RecaputilatifItem item = mapper.toDto(vue);

        assertThat(item.dateReference()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(item.dateEcheance()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(item.credit()).isEqualByComparingTo("50.00");
    }

    private static RecapitulatifEncoursId identifiant(TransactionSource source) {
        RecapitulatifEncoursId id = BeanUtils.instantiateClass(RecapitulatifEncoursId.class);
        ReflectionTestUtils.setField(id, "sourceType", source);
        ReflectionTestUtils.setField(id, "entityId", 1L);
        return id;
    }
}
