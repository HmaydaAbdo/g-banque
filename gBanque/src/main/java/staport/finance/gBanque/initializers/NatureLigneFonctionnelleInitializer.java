package staport.finance.gBanque.initializers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.repositories.NatureLigneFonctionnelleRepository;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class NatureLigneFonctionnelleInitializer {

    private static final Logger log = LoggerFactory.getLogger(NatureLigneFonctionnelleInitializer.class);

    @Bean
    CommandLineRunner natureLigneFonctionnelleDataInitializer(NatureLigneFonctionnelleRepository repository) {
        return args -> {
            log.info("Initializing NatureLigneFonctionnelle data directly from code...");

            List<NatureLigneFonctionnelle> initialData = new ArrayList<>();

            // Define your data directly here
            initialData.add(createNatureLigneFonctionnelle("Facilité de Caisse", "Autorisation de découvert de courte durée pour faire face à des décalages de trésorerie passagers et imprévus."));
            initialData.add(createNatureLigneFonctionnelle("Escompte Commercial", "Avance de la banque sur le montant d'effets de commerce non encore échus, déduction faite d'agios."));
            initialData.add(createNatureLigneFonctionnelle("Cautions Provisoires", "Garanties bancaires exigées lors de la soumission à des appels d'offres, attestant la capacité financière."));
            initialData.add(createNatureLigneFonctionnelle("Cautions Définitives", "Garanties bancaires remises après attribution d'un marché, pour garantir la bonne exécution des obligations contractuelles."));
            initialData.add(createNatureLigneFonctionnelle("Cautions Retenue de Garantie", "Garantie bancaire permettant de percevoir l'intégralité du paiement d'un marché sans attendre la fin de la période de garantie."));
            initialData.add(createNatureLigneFonctionnelle("Cautions Diverses", "Catégorie générique incluant toutes les autres formes de garanties bancaires non spécifiquement listées, adaptées à des besoins particuliers."));
            initialData.add(createNatureLigneFonctionnelle("Avance sur Marchés Nantis", "Financement accordé par la banque sur la base de marchés publics ou privés mis en garantie, avance sur les sommes à recevoir."));
            initialData.add(createNatureLigneFonctionnelle("CREDOC (Crédit Documentaire)", "Engagement d'une banque de payer un vendeur si les conditions stipulées dans le crédit documentaire sont remplies."));
            initialData.add(createNatureLigneFonctionnelle("AVAL", "Garantie donnée par une banque sur un effet de commerce (lettre de change, billet à ordre), engageant la banque à payer en cas de défaut."));
            initialData.add(createNatureLigneFonctionnelle("Refinancement", "Opération par laquelle une banque accorde un nouveau prêt pour rembourser un prêt existant, souvent dans des conditions plus favorables."));


            List<NatureLigneFonctionnelle> toSave = new ArrayList<>();

            for (NatureLigneFonctionnelle dataItem : initialData) {
                String label = dataItem.getLabel(); // Get label from the prepared item

                // Check if a NatureLigneFonctionnelle with this label already exists
                if (repository.findByLabel(label).isEmpty()) {
                    // If it doesn't exist, add the prepared item to the list to be saved
                    toSave.add(dataItem);
                    log.info("Prepared to save new NatureLigneFonctionnelle: '{}'", label);
                } else {
                    log.debug("NatureLigneFonctionnelle with label '{}' already exists. Skipping.", label);
                }
            }

            if (!toSave.isEmpty()) {
                repository.saveAll(toSave);
                log.info("Successfully saved {} new NatureLigneFonctionnelle entities.", toSave.size());
            } else {
                log.info("No new NatureLigneFonctionnelle entities to save.");
            }

            log.info("NatureLigneFonctionnelle data initialization complete.");
        };
    }

    // Helper method to create a NatureLigneFonctionnelle instance
    private NatureLigneFonctionnelle createNatureLigneFonctionnelle(String label, String description) {
        NatureLigneFonctionnelle nature = new NatureLigneFonctionnelle();
        nature.setLabel(label);
        nature.setDescription(description);
        return nature;
    }
}