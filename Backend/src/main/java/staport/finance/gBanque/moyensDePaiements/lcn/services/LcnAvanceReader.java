package staport.finance.gBanque.moyensDePaiements.lcn.services;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.moyensDePaiements.lcn.entities.Lcn;
import staport.finance.gBanque.moyensDePaiements.lcn.repositories.LcnRepository;
import staport.finance.gBanque.moyensDePaiements.lecture.MoyenPaiementReader;
import staport.finance.gBanque.moyensDePaiements.lecture.MoyenPaiementResume;
import staport.finance.gBanque.moyensDePaiements.lecture.RechercheMoyenPaiement;
import staport.finance.gBanque.moyensDePaiements.lecture.TypeMoyenPaiement;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

/** Lecture des LCN par un autre domaine (règlement de l'avance d'un contrat de leasing). */
@Component
public class LcnAvanceReader implements MoyenPaiementReader {

    private final LcnRepository lcnRepository;

    public LcnAvanceReader(LcnRepository lcnRepository) {
        this.lcnRepository = lcnRepository;
    }

    @Override
    public TypeMoyenPaiement type() {
        return TypeMoyenPaiement.LCN;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MoyenPaiementResume> findById(Long id) {
        return lcnRepository.findById(id).map(LcnAvanceReader::toResume);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MoyenPaiementResume> findDecaissementsNonAnnules(String recherche, Collection<Long> idsExclus, Pageable pageable) {
        Specification<Lcn> spec = (root, query, cb) -> {
            List<Predicate> predicats = new ArrayList<>();
            predicats.add(cb.equal(root.get("type"), Type.DECAISSEMENT));
            predicats.add(cb.notEqual(root.get("statut"), StatutMoyenPaiement.ANNULE));
            if (!idsExclus.isEmpty()) {
                predicats.add(cb.not(root.get("id").in(idsExclus)));
            }
            Predicate recherchee = RechercheMoyenPaiement.parNumeroOuMontant(cb, root.get("numero"), root.get("montant"), recherche);
            if (recherchee != null) {
                predicats.add(recherchee);
            }
            return cb.and(predicats.toArray(new Predicate[0]));
        };
        Pageable parId = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "id"));
        return lcnRepository.findAll(spec, parId).map(LcnAvanceReader::toResume);
    }

    @Override
    @Transactional
    public void modifierTextes(Long id, UnaryOperator<String> libelle, UnaryOperator<String> observation) {
        Lcn lcn = lcnRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LC introuvable avec l'ID: " + id));
        lcn.setLibelle(libelle.apply(lcn.getLibelle()));
        lcn.setObservation(observation.apply(lcn.getObservation()));
        lcnRepository.save(lcn);
    }

    private static MoyenPaiementResume toResume(Lcn c) {
        return new MoyenPaiementResume(TypeMoyenPaiement.LCN, c.getId(), c.getNumero(), c.getMontant(),
                c.getStatut(), c.getType() == Type.DECAISSEMENT, c.getDateEmission(), c.getDateOperation(),
                c.getDateValeur(), c.getBeneficiaire(), c.getBanque().getNom());
    }
}
