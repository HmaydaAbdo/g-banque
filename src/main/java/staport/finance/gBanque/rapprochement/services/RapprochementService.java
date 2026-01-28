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
import staport.finance.gBanque.rapprochement.repository.RapprochementRepository;
import staport.finance.gBanque.rapprochement.mapper.RapprochementMapper;
import staport.finance.gBanque.rapprochement.specs.RapprochementSpecifications;
import staport.finance.gBanque.reports.service.StaportPdfReportService;
import staport.finance.gBanque.transactions.dtos.TransactionResponseDto;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.repositories.TransactionRepository;
import staport.finance.gBanque.transactions.mapper.TransactionMapper;
import staport.finance.gBanque.transactions.spec.TransactionSpecifications;

import java.math.BigDecimal;
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
        Specification<Rapprochement> spec = RapprochementSpecifications.bySearchDto(searchDto);

        // 2. Exécuter la recherche paginée avec les spécifications
        Page<Rapprochement> rapprochementPage = rapprochementRepository.findAll(spec, pageable);

        // 3. Mapper la Page d'Entités en Page de DTOs
        // Le Mapper (RapprochementMapper::toResponseDto) ne définit pas le champ 'transactions',
        // il sera donc null ou vide, ce qui est l'effet désiré pour la liste.
        return rapprochementPage.map(rapprochementMapper::toResponseDto);
    }

    @Transactional()
    public RapprochementResponseDto addRapprochement(RapprochementRequestDto requestDto) {

        Banque banque=banqueRepository.findById(requestDto.getBanqueId()).orElseThrow(()->new ResourceNotFoundException("Banque not found with id: "+requestDto.getBanqueId()));

        Specification<Transaction> specification=isConfirmed(false).and(hasBanqueId(requestDto.getBanqueId()));
        List<Transaction> transactions=transactionRepository.findAll(specification);

        // --- Calculs des totaux ---
        BigDecimal totalDebit = transactions.stream()
                .map(Transaction::getDebit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredit = transactions.stream()
                .map(Transaction::getCredit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalTvaDeductible = transactions.stream()
                .map(Transaction::getTvaDeductible)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalTvaCollectee = transactions.stream()
                .map(Transaction::getTvaCollectee)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Rapprochement rapprochement=new Rapprochement();

        rapprochement.setBanque(banque);
        rapprochement.setDateRapprochement(requestDto.getDateRapprochement());
        rapprochement.setSoldeDebut(banque.getSoldeDepart());
        rapprochement.setSoldeFin(banque.getSoldeActuel());
        rapprochement.setTotalDebit(totalDebit);
        rapprochement.setTotalCredit(totalCredit);
        rapprochement.setTvaDeductible(totalTvaDeductible);
        rapprochement.setTvaCollectee(totalTvaCollectee);

        Rapprochement savedRapprochement = rapprochementRepository.save(rapprochement);

        banque.setSoldeDepart(banque.getSoldeActuel());
        banqueRepository.save(banque);

        transactions.forEach((transaction) -> {
            transaction.setConfirmed(true);
            transaction.setRapprochement(savedRapprochement);
        });
        transactionRepository.saveAll(transactions);

        return rapprochementMapper.toResponseDto(savedRapprochement);
    }

    @Transactional(readOnly = true)
    public RapprochementResponseDto getRapprochementById(Long id, Pageable pageable) {

        Rapprochement rapprochement = rapprochementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rapprochement not found with id: " + id));

        // Récupérer les Transactions paginées associées
        Specification<Transaction> spec = TransactionSpecifications.hasRapprochementId(id);

        Page<Transaction> transactionPage = transactionRepository.findAll(spec, pageable);

        // Mapper les transactions vers des DTOs
        Page<TransactionResponseDto> transactionDtos =
                transactionPage.map(TransactionMapper::toDto);

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
