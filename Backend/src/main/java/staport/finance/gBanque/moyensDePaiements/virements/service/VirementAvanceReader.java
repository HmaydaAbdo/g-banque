package staport.finance.gBanque.moyensDePaiements.virements.service;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.moyensDePaiements.virements.entities.Virement;
import staport.finance.gBanque.moyensDePaiements.virements.repositories.VirementRepository;
import staport.finance.gBanque.moyensDePaiements.lecture.MoyenPaiementReader;
import staport.finance.gBanque.moyensDePaiements.lecture.MoyenPaiementResume;
import staport.finance.gBanque.moyensDePaiements.lecture.TypeMoyenPaiement;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

/** Lecture des virements par un autre domaine (règlement de l'avance d'un contrat de leasing). */
@Component
public class VirementAvanceReader implements MoyenPaiementReader {

    private final VirementRepository virementRepository;

    public VirementAvanceReader(VirementRepository virementRepository) {
        this.virementRepository = virementRepository;
    }

    @Override
    public TypeMoyenPaiement type() {
        return TypeMoyenPaiement.VIREMENT;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MoyenPaiementResume> findById(Long id) {
        return virementRepository.findById(id).map(VirementAvanceReader::toResume);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MoyenPaiementResume> findDecaissementsNonAnnules(String recherche, Collection<Long> idsExclus, Pageable pageable) {
        Specification<Virement> spec = (root, query, cb) -> {
            List<Predicate> predicats = new ArrayList<>();
            predicats.add(cb.equal(root.get("type"), Type.DECAISSEMENT));
            predicats.add(cb.notEqual(root.get("statut"), StatutMoyenPaiement.ANNULE));
            if (!idsExclus.isEmpty()) {
                predicats.add(cb.not(root.get("id").in(idsExclus)));
            }
            if (recherche != null && !recherche.isBlank()) {
                String motif = "%" + recherche.trim().toLowerCase() + "%";
                predicats.add(cb.like(cb.lower(root.get("numero")), motif));
            }
            return cb.and(predicats.toArray(new Predicate[0]));
        };
        Pageable parId = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "id"));
        return virementRepository.findAll(spec, parId).map(VirementAvanceReader::toResume);
    }

    @Override
    @Transactional
    public void modifierTextes(Long id, UnaryOperator<String> libelle, UnaryOperator<String> observation) {
        Virement virement = virementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Virement introuvable avec l'ID: " + id));
        virement.setLibelle(libelle.apply(virement.getLibelle()));
        virement.setObservation(observation.apply(virement.getObservation()));
        virementRepository.save(virement);
    }

    private static MoyenPaiementResume toResume(Virement c) {
        return new MoyenPaiementResume(TypeMoyenPaiement.VIREMENT, c.getId(), c.getNumero(), c.getMontant(),
                c.getStatut(), c.getType() == Type.DECAISSEMENT, c.getDateEmission(), c.getDateOperation(),
                c.getDateValeur(), c.getBeneficiaire(), c.getBanqueEmettrice() != null ? c.getBanqueEmettrice().getNom() : null);
    }
}
