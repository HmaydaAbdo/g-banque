package staport.finance.gBanque.Bank.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.dtos.HistoriqueSoldeBanqueDto;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.entities.HistoriqueSoldeBanque;
import staport.finance.gBanque.Bank.mappers.HistoriqueSoldeBanqueMapper;
import staport.finance.gBanque.Bank.repositories.HistoriqueSoldeBanqueRepository;
import staport.finance.gBanque.exception.customExceptions.HistoriqueAlreadyExistsException;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HistoriqueSoldeBanqueService {

    private final HistoriqueSoldeBanqueRepository historiqueRepository;

    public HistoriqueSoldeBanqueService(HistoriqueSoldeBanqueRepository historiqueRepository) {
        this.historiqueRepository = historiqueRepository;
    }

    /**
     * Crée un nouvel historique de solde pour une banque
     * Applique les règles de validation métier
     */
    @Transactional
    public HistoriqueSoldeBanque createHistorique(
            Banque banque,
            Integer mois,
            Integer annee,
            BigDecimal soldeDepart,
            BigDecimal soldeFin) {

        // Validation 1: Vérifier que la banque n'est pas null
        if (banque == null) {
            throw new IllegalArgumentException("La banque ne peut pas être null");
        }

        // Validation 2: Vérifier que le mois est valide (1-12)
        if (mois == null || mois < 1 || mois > 12) {
            throw new IllegalArgumentException("Le mois doit être entre 1 et 12");
        }

        // Validation 3: Vérifier que l'année est valide
        if (annee == null || annee < 2000 || annee > 2100) {
            throw new IllegalArgumentException("L'année doit être entre 2000 et 2100");
        }

        // Validation 4: Vérifier que les soldes ne sont pas null
        if (soldeDepart == null || soldeFin == null) {
            throw new IllegalArgumentException("Les soldes de départ et de fin ne peuvent pas être null");
        }

        // Validation 5: Vérifier qu'il n'existe pas déjà un historique pour cette période
        if (historiqueRepository.existsByBanqueIdAndMoisAndAnnee(banque.getId(), mois, annee)) {
            throw new HistoriqueAlreadyExistsException(
                    String.format("Un historique existe déjà pour la banque %s pour le mois %d/%d",
                            banque.getNom(), mois, annee)
            );
        }

        // Création de l'historique
        HistoriqueSoldeBanque historique = new HistoriqueSoldeBanque(
                banque,
                mois,
                annee,
                soldeDepart,
                soldeFin
        );

        return historiqueRepository.save(historique);
    }

    /**
     * Récupère l'historique complet d'une banque
     */
    @Transactional(readOnly = true)
    public List<HistoriqueSoldeBanqueDto> getHistoriqueByBanque(Long banqueId) {
        if (banqueId == null) {
            throw new IllegalArgumentException("L'ID de la banque ne peut pas être null");
        }

        return historiqueRepository.findByBanqueIdOrderByAnneeDescMoisDesc(banqueId)
                .stream()
                .map(HistoriqueSoldeBanqueMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Récupère un historique spécifique pour une période donnée
     */
    @Transactional(readOnly = true)
    public HistoriqueSoldeBanqueDto getHistoriqueByPeriode(Long banqueId, Integer mois, Integer annee) {
        if (banqueId == null || mois == null || annee == null) {
            throw new IllegalArgumentException("Les paramètres ne peuvent pas être null");
        }

        HistoriqueSoldeBanque historique = historiqueRepository
                .findByBanqueIdAndMoisAndAnnee(banqueId, mois, annee)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Aucun historique trouvé pour la banque %d pour le mois %d/%d",
                                banqueId, mois, annee)
                ));

        return HistoriqueSoldeBanqueMapper.toDto(historique);
    }

    /**
     * Récupère l'historique d'une année spécifique
     */
    @Transactional(readOnly = true)
    public List<HistoriqueSoldeBanqueDto> getHistoriqueByAnnee(Long banqueId, Integer annee) {
        if (banqueId == null || annee == null) {
            throw new IllegalArgumentException("Les paramètres ne peuvent pas être null");
        }

        return historiqueRepository.findByBanqueIdAndAnnee(banqueId, annee)
                .stream()
                .map(HistoriqueSoldeBanqueMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Vérifie si un historique existe pour une période donnée
     */
    @Transactional(readOnly = true)
    public boolean existsForPeriode(Long banqueId, Integer mois, Integer annee) {
        if (banqueId == null || mois == null || annee == null) {
            return false;
        }
        return historiqueRepository.existsByBanqueIdAndMoisAndAnnee(banqueId, mois, annee);
    }
}
