package staport.finance.gBanque.Agios.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Agios.dtos.AgiosJournalierResponseDto;
import staport.finance.gBanque.Agios.dtos.AgiosTrimestrielResponseDto;
import staport.finance.gBanque.Agios.entities.AgiosJournalier;
import staport.finance.gBanque.Agios.entities.AgiosTrimestriel;

@Component
public class AgiosCalculatorMapper {


    public AgiosTrimestrielResponseDto toTrimDto(AgiosTrimestriel e) {
        AgiosTrimestrielResponseDto d = new AgiosTrimestrielResponseDto();
        d.setId(e.getId());
        d.setTypeAgios(e.getTypeAgios());
        d.setTrimestre(e.getTrimestre());
        d.setAnnee(e.getAnnee());
        d.setDateDebut(e.getDateDebut());
        d.setDateFin(e.getDateFin());
        d.setBanqueId(e.getBanque().getId());
        d.setBanqueNom(e.getBanque().getNom());
        d.setTotalAgiosHT(e.getTotalAgiosHT());
        d.setTotalTVA(e.getTotalTVA());
        d.setTotalAgiosTTC(e.getTotalAgiosTTC());
        d.setNbJoursCalcules(e.getNbJoursCalcules());
        d.setNbJoursTotal(e.getNbJoursTotal());
        d.setStatut(e.getStatut());
        d.setDateDernierCalcul(e.getDateDernierCalcul());
        d.setDateCloture(e.getDateCloture());
        d.setAgiosDefinitifId(e.getAgiosDefinitifId());
        return d;
    }

    public AgiosJournalierResponseDto toJourDto(AgiosJournalier e) {
        AgiosJournalierResponseDto d = new AgiosJournalierResponseDto();
        d.setId(e.getId());
        d.setTypeAgios(e.getTypeAgios());
        d.setDateValeur(e.getDateValeur());
        d.setBanqueId(e.getBanque().getId());
        d.setBanqueNom(e.getBanque().getNom());
        d.setSoldeJournalier(e.getSoldeJournalier());
        d.setBaseCalcul(e.getBaseCalcul());
        d.setTauxApplique(e.getTauxApplique());
        d.setAgiosHT(e.getAgiosHT());
        d.setTva(e.getTva());
        d.setAgiosTTC(e.getAgiosTTC());
        d.setDateCalcul(e.getDateCalcul());
        d.setNbRecalculs(e.getNbRecalculs());
        return d;
    }

}
