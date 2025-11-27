package staport.finance.gBanque.virements.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.virements.dtos.*;
import staport.finance.gBanque.virements.entities.Virement;
import staport.finance.gBanque.virements.enums.Type;
import staport.finance.gBanque.virements.mapper.VirementMapper;
import staport.finance.gBanque.virements.repositories.VirementRepository;
import staport.finance.gBanque.virements.specs.VirementSpec;

import java.math.BigDecimal;
import java.util.NoSuchElementException;

@Service
@Transactional(readOnly = true)
public class VirementService {

    private final VirementRepository virementRepository;
    private final BanqueRepository banqueRepository;
    private final VirementMapper virementMapper;


    public VirementService(VirementRepository virementRepository, BanqueRepository banqueRepository, VirementMapper virementMapper) {
        this.virementRepository = virementRepository;
        this.banqueRepository = banqueRepository;
        this.virementMapper = virementMapper;
    }


    @Transactional
    public VirementResponseDto createEncaissementVirement(VirementRequestDto dto) {

        // 1. Convertir le DTO en entité Virement
        Virement virement = this.virementMapper.toEntity(dto);

        // 2. Définir le statut initial (doit être actif pour les nouvelles créations)
        virement.setActive(true);

        // 4. Sauvegarder l'entité
        virement = virementRepository.save(virement);

        // 5. Mapper et retourner le DTO de réponse
        return this.virementMapper.toDto(virement);
    }

    @Transactional
    public VirementResponseDto editEncaissementVirement(Long id, VirementRequestDto dto) {
        Virement existingVirement = virementRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Virement with id " + id + " not found."));

        // Règle 1 : Vérifier le type (doit être ENCAISSEMENT)
        if (existingVirement.getType() != Type.ENCAISSEMENT) {
            throw new IllegalArgumentException("Modification interdite: le virement with id " + id + " n'est pas de type ENCAISSEMENT.");
        }

        // Règle 2 : Vérifier le statut (doit être actif)
        if (!existingVirement.isActive()) {
            throw new IllegalArgumentException("Modification interdite: le virement with id  " + id + " est déja réglé.");
        }

        // Règle 3 : Mettre à jour les champs modifiables
        // Montant
        if (dto.getMontant() != null) {
            existingVirement.setMontant(dto.getMontant());
        }
        // Date de réception (DateEmission dans l'entité)
        if (dto.getDateReception() != null) {
            existingVirement.setDateEmission(dto.getDateReception());
        }
        // Émetteur
        if (dto.getEmetteur() != null) {
            existingVirement.setEmetteur(dto.getEmetteur());
        }
        // Banque Bénéficiaire (BanqueBeneficiare dans l'entité)
        if (dto.getBanqueBeneficiaireId() != null) {
            Banque banque=banqueRepository.findById(dto.getBanqueBeneficiaireId()).orElseThrow(()->new ResourceNotFoundException("Banque with id: "+dto.getBanqueBeneficiaireId()+" not found"));
            existingVirement.setBanqueBeneficiare(banque);
        }

        // Les champs 'type' et 'beneficiaire' sont immuables, donc on ne les modifie pas.

        // Sauvegarder et retourner
        existingVirement = virementRepository.save(existingVirement);
        return this.virementMapper.toDto(existingVirement);
    }

    @Transactional
    public VirementResponseDto getVirementById(Long id) {
        Virement virement = virementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Virement with id " + id + " not found."));
        return this.virementMapper.toDto(virement);
    }
    @Transactional(readOnly = true)
    public Page<VirementResponseDto> searchVirements(VirementSearchDto searchDto, Pageable pageable) {
        // Build the combined specification using the static methods from VirementSpec
        Specification<Virement> spec = VirementSpec.bySearchDto(searchDto);

        // Execute the search with the combined specification and pagination
        Page<Virement> virementPage = virementRepository.findAll(spec, pageable);

        // Map the result Page<Virement> to Page<VirementResponseDto>
        return virementPage.map(this.virementMapper::toDto);
    }

    @Transactional
    public VirementResponseDto toggleVirementStatus(Long id) {
        Virement virement = virementRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Virement with id " + id + " not found."));

        // Toggle the 'active' status
        virement.setActive(!virement.isActive());

        // Save the updated virement
        virement = virementRepository.save(virement);

        // Map and return the updated DTO
        return this.virementMapper.toDto(virement);
    }


    @Transactional(readOnly = true)
    public EncoursVirementResponseDto getEncoursDecaissementForVirements(EncoursVirementsSearchDto searchDto, Long banqueId, Pageable pageable) {
        // Build the specification for active virements for the specified bank in the current month
        Specification<Virement> spec = VirementSpec.encoursDecaissement(banqueId)
                .and(VirementSpec.hasNumero(searchDto.getNumero()))
                .and(VirementSpec.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(VirementSpec.hasMontant(searchDto.getMontant()));

        // Execute the search with the specification and pagination
        Page<Virement> virementPage = virementRepository.findAll(spec, pageable);

        BigDecimal totalDesEncours = virementRepository.findAll(spec).stream().map(Virement::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
        Page<VirementResponseDto> result = virementPage.map(this.virementMapper::toDto);
        // Map the result to DTOs and return
        return new EncoursVirementResponseDto(totalDesEncours,result);
    }


    @Transactional(readOnly = true)
    public EncoursVirementResponseDto getEncoursEncaissementForVirements(EncoursVirementsSearchDto searchDto, Long banqueId, Pageable pageable) {

        Banque banqueBeneficiaire = banqueRepository.findById(banqueId).orElseThrow(() -> new NoSuchElementException("Bank with id " + banqueId + " not found."));

        // Build the specification for active virements for the specified bank in the current month
        Specification<Virement> spec = VirementSpec.encoursEncaissement(banqueBeneficiaire.getNom())
                .and(VirementSpec.hasNumero(searchDto.getNumero()))
                .and(VirementSpec.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(VirementSpec.hasMontant(searchDto.getMontant()));

        // Execute the search with the specification and pagination
        Page<Virement> virementPage = virementRepository.findAll(spec, pageable);

        BigDecimal totalDesEncours = virementRepository.findAll(spec).stream().map(Virement::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
        Page<VirementResponseDto> result = virementPage.map(this.virementMapper::toDto);
        // Map the result to DTOs and return
        return new EncoursVirementResponseDto(totalDesEncours,result);
    }


    @Transactional
    public void deleteEncaissementVirement(Long id) {
        Virement virement = virementRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Virement with id " + id + " not found."));

        // Règle 1 : Vérifier le type (doit être ENCAISSEMENT)
        if (virement.getType() != Type.ENCAISSEMENT) {
            throw new IllegalArgumentException("Suppression interdite: le virement with id " + id + " n'est pas de type ENCAISSEMENT.");
        }

        // Règle 2 : Vérifier le statut (doit être actif)
        if (!virement.isActive()) {
            throw new IllegalArgumentException("Suppression interdite: le virement with id " + id + " est déja réglé.");
        }

        // Supprimer l'entité
        virementRepository.delete(virement);
    }
}