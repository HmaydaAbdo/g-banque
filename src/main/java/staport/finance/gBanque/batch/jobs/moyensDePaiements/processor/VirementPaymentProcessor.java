    package staport.finance.gBanque.batch.jobs.moyensDePaiements.processor;

    import org.springframework.stereotype.Component;
    import staport.finance.gBanque.Bank.entities.Banque;
    import staport.finance.gBanque.Bank.repositories.BanqueRepository;
    import staport.finance.gBanque.exception.customExceptions.InvalidChequeDataException;
    import staport.finance.gBanque.moyensDePaiements.synchronisation.dtos.FunPaymentDto;
    import staport.finance.gBanque.moyensDePaiements.virements.entities.Virement;
    import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
    import staport.finance.gBanque.moyensDePaiements.virements.repositories.VirementRepository;
    import java.util.List;
    import java.util.Objects;
    import java.util.Optional;
    import java.util.stream.Stream;

    @Component
    public class VirementPaymentProcessor extends AbstractPaymentProcessor {

        private final VirementRepository virementRepository;

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
            v.setDateEmission(item.getDateCreation());
            v.setBeneficiaire(item.getBeneficiaire());
            v.setMontant(item.getMontant());
            v.setBanqueEmettrice(banque);
            Optional<Banque> banqueBeneficaire=this.banqueRepository.findByNomIgnoreCase(item.getBanqueBeneficiaire());
            banqueBeneficaire.ifPresent(v::setBanqueBeneficiare);;
            v.setNomBanqueBeneficiaireLibre(item.getBanqueBeneficiaire());
            v.setRibBeneficiaire(item.getRibBeneficiaire());
            v.setActive(true);

            if(type==Type.ENCAISSEMENT){
                v.setEmetteur("STAPORT");
            }

            return v;
        }
    }
