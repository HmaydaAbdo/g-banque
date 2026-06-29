package staport.finance.gBanque.Agios.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Agios.dtos.AgiosJournalierResponseDto;
import staport.finance.gBanque.Agios.dtos.AgiosTrimestrielResponseDto;
import staport.finance.gBanque.Agios.entities.AgiosJournalier;
import staport.finance.gBanque.Agios.entities.AgiosTrimestriel;

@Component
public class AgiosCalculatorMapper {


    public AgiosTrimestrielResponseDto toTrimDto(AgiosTrimestriel e) {
        return new AgiosTrimestrielResponseDto(
                e.getId(),
                e.getTypeAgios(),
                e.getTrimestre(),
                e.getAnnee(),
                e.getDateDebut(),
                e.getDateFin(),
                e.getBanque().getId(),
                e.getBanque().getNom(),
                e.getTotalAgiosHT(),
                e.getTotalTVA(),
                e.getTotalAgiosTTC(),
                e.getNbJoursCalcules(),
                e.getNbJoursTotal(),
                e.getStatut(),
                e.getDateDernierCalcul(),
                e.getDateCloture(),
                e.getAgiosDefinitifId(),
                null
        );
    }

    public AgiosJournalierResponseDto toJourDto(AgiosJournalier e) {
        return new AgiosJournalierResponseDto(
                e.getId(),
                e.getTypeAgios(),
                e.getDateValeur(),
                e.getBanque().getId(),
                e.getBanque().getNom(),
                e.getSoldeJournalier(),
                e.getBaseCalcul(),
                e.getTauxApplique(),
                e.getAgiosHT(),
                e.getTva(),
                e.getAgiosTTC(),
                e.getDateCalcul(),
                e.getNbRecalculs(),
                e.getMarche() != null ? e.getMarche().getId()  : null,
                e.getMarche() != null ? e.getMarche().getNom() : null
        );
    }

}
