package staport.finance.gBanque.initializers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.enums.Devise;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import org.springframework.core.io.ClassPathResource; // Import for resource loading
import org.springframework.util.StreamUtils; // Import for byte array conversion

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class BanqueInitializer {

    private static final Logger log = LoggerFactory.getLogger(BanqueInitializer.class);

    @Bean
    CommandLineRunner banqueDataInitializer(BanqueRepository repository) {
        return args -> {
            log.info("Initializing Banque data and loading logos from resources...");

            List<Banque> banksToCreate = new ArrayList<>();

            // Data extracted directly from your banque.json, now with logo paths
            banksToCreate.add(createBanque(
                    "007240000100000000012345",
                    "Attijariwafa Bank",
                    Devise.MAD,
                    "Agence Guéliz",
                    "Avenue Mohammed V, Guéliz, Marrakech",
                    "logos/AWB.png"
            ));
            banksToCreate.add(createBanque(
                    "011240000200000000023456",
                    "BMCE",
                    Devise.MAD,
                    "Agence Liberté",
                    "Place du 16 Novembre, Guéliz, Marrakech",
                    "logos/BMCE.png"
            ));
            banksToCreate.add(createBanque(
                    "013240000300000000034567",
                    "BMCI",
                    Devise.MAD,
                    "Agence Majorelle",
                    "Rue Tariq Bnou Ziad, Guéliz, Marrakech",
                    "logos/BMCI.png"
            ));
            banksToCreate.add(createBanque(
                    "021240000500000000056789",
                    "Credit du Maroc",
                    Devise.MAD,
                    "Agence Yacoub El Mansou",
                    "Avenue Yacoub El Mansour, Guéliz, Marrakech",
                    "logos/CDM.png"
            ));
            banksToCreate.add(createBanque(
                    "022240000800000000089012",
                    "SGMB",
                    Devise.MAD,
                    "Agence Semlalia",
                    "Avenue Abdelkarim El Khattabi, Guéliz, Marrakech",
                    "logos/SGMB.png"
            ));
            banksToCreate.add(createBanque(
                    "022240000822000000089012",
                    "BPMG",
                    Devise.MAD,
                    "Agence Victor Hugo",
                    "Rue Victor Hugo, 20503 Marrakech, Maroc",
                    "logos/BPMG.png"
            ));
            banksToCreate.add(createBanque(
                    "140240000400000000045678",
                    "Banque Populaire",
                    Devise.MAD,
                    "Agence Princesse",
                    "Boulevard Hassan II, Guéliz, Marrakech",
                    "logos/BP.png"
            ));
            banksToCreate.add(createBanque(
                    "230240000600000000067890",
                    "CIH ",
                    Devise.MAD,
                    "Agence Massira",
                    "Avenue Allal El Fassi, Massira, Marrakech",
                    "logos/CIH.png"
            ));
            banksToCreate.add(createBanque(
                    "888240000700000000078901",
                    "FINEA",
                    Devise.MAD,
                    "Agence Hivernage",
                    "Rue Moulay Ismail, Hivernage, Marrakech",
                    "logos/FINEA.png"
            ));


            int newBanksCount = 0;
            for (Banque bank : banksToCreate) {
                if (repository.findByNumeroDeCompte(bank.getNumeroDeCompte()).isEmpty()) {
                    repository.save(bank);
                    newBanksCount++;
                    log.info("Successfully saved new Banque: '{}' ({})", bank.getNom(), bank.getNumeroDeCompte());
                } else {
                    log.debug("Banque '{}' with account number '{}' already exists. Skipping.", bank.getNom(), bank.getNumeroDeCompte());
                }
            }

            if (newBanksCount > 0) {
                log.info("Successfully saved {} new Banque entities.", newBanksCount);
            } else {
                log.info("No new Banque entities to save.");
            }

            log.info("Banque data initialization complete.");
        };
    }

    // Helper method to create a Banque instance, loading logo from resource
    private Banque createBanque(String numeroDeCompte, String nom, Devise devise, String agence, String address,
                                String logoResourcePath) {
        Banque banque = new Banque();
        try {
            // Load logo data from the specified resource path
            byte[] logoData = loadLogoAsByteArray(logoResourcePath);
            banque.setLogoData(logoData);
        } catch (IOException e) {
            log.error("Failed to load logo from resource '{}' for bank '{}'. Using empty logo data. Error: {}",
                    logoResourcePath, nom, e.getMessage());
            banque.setLogoData(new byte[0]); // Fallback to empty byte array on error
        }

        banque.setNumeroDeCompte(numeroDeCompte);
        banque.setNom(nom);
        banque.setDevise(devise);
        banque.setAgence(agence);
        banque.setAddress(address);
        // Solde Actuel: Will be initialized to BigDecimal.ZERO by @PrePersist if null.
        banque.setSoldeActuel(BigDecimal.ZERO);
        // dateCreation and dateModification will be set by @PrePersist
        return banque;
    }

    // Helper method to load a file from resources as byte array
    private byte[] loadLogoAsByteArray(String resourcePath) throws IOException {
        ClassPathResource classPathResource = new ClassPathResource(resourcePath);
        if (!classPathResource.exists()) {
            throw new IOException("Resource not found: " + resourcePath + ". Make sure it's in src/main/resources/" + resourcePath);
        }
        try (InputStream inputStream = classPathResource.getInputStream()) {
            return StreamUtils.copyToByteArray(inputStream);
        }
    }
}