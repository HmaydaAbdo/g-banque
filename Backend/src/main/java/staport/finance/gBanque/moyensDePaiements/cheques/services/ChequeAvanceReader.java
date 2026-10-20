package staport.finance.gBanque.moyensDePaiements.cheques.services;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.moyensDePaiements.cheques.entities.Cheque;
import staport.finance.gBanque.moyensDePaiements.cheques.repositories.ChequeRepository;
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

/** Lecture des chèques par un autre domaine (règlement de l'avance d'un contrat de leasing). */
@Component
public class ChequeAvanceReader implements MoyenPaiementReader {

    private final ChequeRepository chequeRepository;

    public ChequeAvanceReader(ChequeRepository chequeRepository) {
        this.chequeRepository = chequeRepository;
    }

    @Override
    public TypeMoyenPaiement type() {
        return TypeMoyenPaiement.CHEQUE;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MoyenPaiementResume> findById(Long id) {
        return chequeRepository.findById(id).map(ChequeAvanceReader::toResume);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MoyenPaiementResume> findDecaissementsNonAnnules(String recherche, Collection<Long> idsExclus, Pageable pageable) {
        Specification<Cheque> spec = (root, query, cb) -> {
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
        return chequeRepository.findAll(spec, parId).map(ChequeAvanceReader::toResume);
    }

    @Override
    @Transactional
    public void modifierTextes(Long id, UnaryOperator<String> libelle, UnaryOperator<String> observation) {
        Cheque cheque = chequeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chèque introuvable avec l'ID: " + id));
        cheque.setLibelle(libelle.apply(cheque.getLibelle()));
        cheque.setObservation(observation.apply(cheque.getObservation()));
        chequeRepository.save(cheque);
    }

    private static MoyenPaiementResume toResume(Cheque c) {
        return new MoyenPaiementResume(TypeMoyenPaiement.CHEQUE, c.getId(), c.getNumero(), c.getMontant(),
                c.getStatut(), c.getType() == Type.DECAISSEMENT, c.getDateEmission(), c.getDateOperation(),
                c.getDateValeur(), c.getBeneficiaire(), c.getBanque().getNom());
    }
}
