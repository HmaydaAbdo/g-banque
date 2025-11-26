package staport.finance.gBanque.FunPaiementService.batch.processor;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.exception.customExceptions.InvalidChequeDataException;
import staport.finance.gBanque.FunPaiementService.dtos.FunPaymentDto;
import staport.finance.gBanque.virements.entities.Virement;
import staport.finance.gBanque.virements.enums.Type;
import staport.finance.gBanque.virements.repositories.VirementRepository;

import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Component
public class VirementPaymentProcessor extends AbstractPaymentProcessor {

    private final VirementRepository virementRepository;
    private final OffsetDateTime curentDateTime = OffsetDateTime.now();

    public VirementPaymentProcessor(BanqueRepository banqueRepository, VirementRepository virementRepository) {
        super(banqueRepository);
        this.virementRepository = virementRepository;
    }

    @Override
    protected Object processSpecific(FunPaymentDto item, Banque banque) throws InvalidChequeDataException {

        boolean isStaport =
                item.getBeneficiaire() != null &&
                        item.getBeneficiaire().trim().equalsIgnoreCase("STAPORT");

        if (!isStaport) {
            // Standard flow = one virement DECAISSEMENT
            Virement v = createVirement(item, banque, Type.DECAISSEMENT, item.getNumero());
            return v == null ? List.of() : List.of(v); // TOUJOURS une liste
        }

        // STAPORT → create two virements with unique ids
        String numeroEncaissement = item.getNumero() + "-EN";
        String numeroDecaissement = item.getNumero() + "-DE";

        Virement encaissement = createVirement(item, banque, Type.ENCAISSEMENT, numeroEncaissement);
        Virement decaissement = createVirement(item, banque, Type.DECAISSEMENT, numeroDecaissement);

        return Stream.of(encaissement, decaissement)
                .filter(Objects::nonNull)
                .toList(); // TOUJOURS une liste
    }

    private Virement createVirement(FunPaymentDto item, Banque banque, Type type, String numero) {

        if (virementRepository.existsByNumero(numero)) {
            log.warn("Virement {} already exists, skipping", numero);
            return null;
        }

        Virement v = new Virement();
        v.setNumero(numero);
        v.setType(type);
        v.setDateEmission(item.getDateCreation().atStartOfDay(systemZone).toOffsetDateTime());
        v.setBeneficiaire(item.getBeneficiaire());
        v.setMontant(item.getMontant());
        v.setBanqueEmettrice(banque);
        Optional<Banque> banqueBeneficaire=this.banqueRepository.findByNom(item.getBanqueBeneficiaire());
        banqueBeneficaire.ifPresent(v::setBanqueBeneficiare);;
        v.setNomBanqueBeneficiaireLibre(item.getBanqueBeneficiaire());
        v.setRibBeneficiaire(item.getRibBeneficiaire());
        // Activation logic
        YearMonth currentMonth = YearMonth.from(curentDateTime);
        YearMonth emissionMonth = YearMonth.from(v.getDateEmission());
        YearMonth oneMonthAgo = currentMonth.minusMonths(1);

        v.setActive(!emissionMonth.isBefore(oneMonthAgo));

        if(type==Type.ENCAISSEMENT){
            v.setEmetteur("virement intra bancaire");
        }

        return v;
    }
}
