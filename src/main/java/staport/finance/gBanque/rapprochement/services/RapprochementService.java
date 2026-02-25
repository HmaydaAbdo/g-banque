package staport.finance.gBanque.rapprochement.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.rapprochement.dtos.RapprochementRequestDto;
import staport.finance.gBanque.rapprochement.dtos.RapprochementResponseDto;
import staport.finance.gBanque.rapprochement.dtos.RapprochementSearchDto;
import staport.finance.gBanque.rapprochement.dtos.ReportDto;
import staport.finance.gBanque.rapprochement.entities.Rapprochement;
import staport.finance.gBanque.rapprochement.enums.StatutRapprochement;
import staport.finance.gBanque.rapprochement.repository.RapprochementRepository;
import staport.finance.gBanque.rapprochement.mapper.RapprochementMapper;
import staport.finance.gBanque.rapprochement.specs.RapprochementSpecifications;
import staport.finance.gBanque.reports.service.StaportPdfReportService;
import staport.finance.gBanque.transactions.dtos.TransactionResponseDto;
import staport.finance.gBanque.transactions.dtos.TransactionSearchDto;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.repositories.TransactionRepository;
import staport.finance.gBanque.transactions.mapper.TransactionMapper;
import staport.finance.gBanque.transactions.spec.TransactionSpecifications;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static staport.finance.gBanque.transactions.spec.TransactionSpecifications.hasBanqueId;
import static staport.finance.gBanque.transactions.spec.TransactionSpecifications.isConfirmed;

@Service
public class RapprochementService {

    private final RapprochementRepository rapprochementRepository;
    private final TransactionRepository transactionRepository;
    private final BanqueRepository banqueRepository;
    private final RapprochementMapper rapprochementMapper;
    private final StaportPdfReportService pdfReportService;

    public RapprochementService(
            RapprochementRepository rapprochementRepository,
            TransactionRepository transactionRepository,
            BanqueRepository banqueRepository,
            RapprochementMapper rapprochementMapper, StaportPdfReportService pdfReportService
    ) {
        this.rapprochementRepository = rapprochementRepository;
        this.transactionRepository = transactionRepository;
        this.banqueRepository = banqueRepository;
        this.rapprochementMapper = rapprochementMapper;
        this.pdfReportService = pdfReportService;
    }


    /**
     * Recherche paginée de rapprochements avec filtres.
     */
    @Transactional(readOnly = true)
    public Page<RapprochementResponseDto> searchRapprochements(RapprochementSearchDto searchDto, Pageable pageable) {

        // 1. Créer la Specification à partir du DTO de recherche
        Specification<Rapprochement> spec = RapprochementSpecifications.bySearchDto(searchDto)
                .and(RapprochementSpecifications.hasStatut(StatutRapprochement.CLOTURE));

        // 2. Exécuter la recherche paginée avec les spécifications
        Page<Rapprochement> rapprochementPage = rapprochementRepository.findAll(spec, pageable);

        // 3. Mapper la Page d'Entités en Page de DTOs
        // Le Mapper (RapprochementMapper::toResponseDto) ne définit pas le champ 'transactions',
        // il sera donc null ou vide, ce qui est l'effet désiré pour la liste.
        return rapprochementPage.map(rapprochementMapper::toResponseDto);
    }

    @Transactional
    public RapprochementResponseDto cloturerRapprochement(Long banqueId) {

        // 1. Trouver le rapprochement EN_COURS de cette banque
        Rapprochement rapprochement = rapprochementRepository
                .findByBanqueIdAndStatut(banqueId, StatutRapprochement.EN_COURS)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucun rapprochement EN_COURS trouvé pour la banque id: " + banqueId));

        Banque banque = banqueRepository.findById(banqueId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Banque introuvable avec l'id: " + banqueId));

        // 2. Récupérer toutes les transactions non confirmées de ce rapprochement
        Specification<Transaction> spec = TransactionSpecifications.hasRapprochementId(rapprochement.getId())
                .and(TransactionSpecifications.isConfirmed(false));
        List<Transaction> transactions = transactionRepository.findAll(spec);

        // 3. Calculer les agrégats
        BigDecimal totalDebit = transactions.stream()
                .map(t -> t.getDebit() != null ? t.getDebit() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredit = transactions.stream()
                .map(t -> t.getCredit() != null ? t.getCredit() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalTvaDeductible = transactions.stream()
                .map(Transaction::getTvaDeductible)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalTvaCollectee = transactions.stream()
                .map(Transaction::getTvaCollectee)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal soldeFin = banque.getSoldeActuel();

        // 4. Mettre à jour et clôturer le rapprochement courant
        rapprochement.setStatut(StatutRapprochement.CLOTURE);
        rapprochement.setSoldeFin(soldeFin);
        rapprochement.setTotalDebit(totalDebit);
        rapprochement.setTotalCredit(totalCredit);
        rapprochement.setTvaDeductible(totalTvaDeductible);
        rapprochement.setTvaCollectee(totalTvaCollectee);

        // 5. Confirmer toutes les transactions
        transactions.forEach(t -> t.setConfirmed(true));
        transactionRepository.saveAll(transactions);

        // 6. Réinitialiser le solde de départ de la banque
        banque.setSoldeDepart(banque.getSoldeActuel());
        banqueRepository.save(banque);

        // 7. Sauvegarder le rapprochement clôturé
        Rapprochement rapprochementCloture = rapprochementRepository.save(rapprochement);

        // 8. Créer automatiquement le rapprochement du mois suivant
        Rapprochement prochainRapprochement = new Rapprochement();
        prochainRapprochement.setBanque(banque);
        prochainRapprochement.setDateRapprochement(rapprochement.getDateRapprochement().plusMonths(1));
        prochainRapprochement.setSoldeDebut(soldeFin);
        prochainRapprochement.setStatut(StatutRapprochement.EN_COURS);
        rapprochementRepository.save(prochainRapprochement);

        return rapprochementMapper.toResponseDto(rapprochementCloture);
    }

    @Transactional(readOnly = true)
    public RapprochementResponseDto getRapprochementById(Long id, TransactionSearchDto searchDto, Pageable pageable) {

        Rapprochement rapprochement = rapprochementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rapprochement not found with id: " + id));

        // Construire les spécifications de recherche
        List<Specification<Transaction>> specs = new ArrayList<>();

        // Filtre obligatoire : transactions du rapprochement
        specs.add(TransactionSpecifications.hasRapprochementId(id));

        // Filtres optionnels basés sur searchDto
        if (searchDto.getLibelle() != null && !searchDto.getLibelle().isEmpty()) {
            specs.add(TransactionSpecifications.libelleContains(searchDto.getLibelle()));
        }

        if (searchDto.getObservation() != null && !searchDto.getObservation().isEmpty()) {
            specs.add(TransactionSpecifications.observationContains(searchDto.getObservation()));
        }

        if (searchDto.getMontant() != null) {
            specs.add(TransactionSpecifications.hasMontant(searchDto.getMontant()));
        }

        if (searchDto.getDateOperationDebut() != null || searchDto.getDateOperationFin() != null) {
            specs.add(TransactionSpecifications.hasDateOperationBetween(
                    searchDto.getDateOperationDebut(),
                    searchDto.getDateOperationFin()
            ));
        }

        if (searchDto.getTransactionSources() != null && !searchDto.getTransactionSources().isEmpty()) {
            specs.add(TransactionSpecifications.hasTransactionSources(searchDto.getTransactionSources()));
        }

        // Combiner toutes les spécifications
        Specification<Transaction> combinedSpec = TransactionSpecifications.withFilters(specs);

        // Récupérer les Transactions paginées avec les filtres
        Page<Transaction> transactionPage = transactionRepository.findAll(combinedSpec, pageable);

        // Mapper les transactions vers des DTOs
        Page<TransactionResponseDto> transactionDtos = transactionPage.map(TransactionMapper::toDto);

        // Mapper le Rapprochement vers le DTO de réponse
        RapprochementResponseDto responseDto = rapprochementMapper.toResponseDto(rapprochement);

        // Injecter la page des transactions dans le DTO
        responseDto.setTransactions(transactionDtos);

        return responseDto;
    }

    @Transactional(readOnly = true)
    public ReportDto getReportDataById(Long rapprochementId) {

        Rapprochement rapprochement = rapprochementRepository.findById(rapprochementId)
                .orElseThrow(() -> new ResourceNotFoundException("Rapprochement not found with id: " + rapprochementId));

        // 1. Mapper le Rapprochement vers le DTO de Rapport (ReportDto)
        ReportDto reportDto = rapprochementMapper.toReportDto(rapprochement); // Utilisation du nouveau mapper

        // 2. Récupérer TOUTES les Transactions associées (sans Pageable)
        Specification<Transaction> spec = TransactionSpecifications.hasRapprochementId(rapprochementId);
        List<Transaction> allTransactions = transactionRepository.findAll(spec);

        // 3. Mapper les entités de Transactions vers des DTOs de Transaction
        List<TransactionResponseDto> allTransactionDtos = allTransactions.stream()
                .map(TransactionMapper::toDto)
                .toList();

        // 4. Injecter la liste complète des transactions
        reportDto.setTransactions(allTransactionDtos);

        return reportDto;
    }


    // NOUVELLE MÉTHODE : Génération de Rapport PDF
    // -------------------------------------------------------------------------

    /**
     * Génère le rapport PDF d'un rapprochement spécifique.
     *
     * @param rapprochementId L'ID du rapprochement.
     * @return Le tableau d'octets représentant le fichier PDF.
     */
    @Transactional(readOnly = true)
    public byte[] generateRapprochementPdf(Long rapprochementId) {

        ReportDto reportDto = getReportDataById(rapprochementId);

        try {
            // 2. Appeler le service de génération de rapport avec le ReportDto
            // (Assurez-vous que StaportPdfReportService accepte ReportDto)
            return pdfReportService.generateRapprochementReport(reportDto);

        } catch (Exception e) {
            // DocumentException est une exception vérifiée, donc nous l'encapsulons dans un RuntimeException
            throw new RuntimeException("Erreur lors de la génération du rapport PDF pour le rapprochement ID: " + rapprochementId, e);
        }
    }
}
