package staport.finance.gBanque.moyensDePaiements.lcn.services;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.moyensDePaiements.lcn.entities.Lcn;
import staport.finance.gBanque.moyensDePaiements.lcn.repositories.LcnRepository;
import staport.finance.gBanque.moyensDePaiements.lecture.MoyenPaiementReader;
import staport.finance.gBanque.moyensDePaiements.lecture.MoyenPaiementResume;
import staport.finance.gBanque.moyensDePaiements.lecture.TypeMoyenPaiement;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

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
    public List<MoyenPaiementResume> findDecaissementsNonAnnulesDeMontant(BigDecimal montant) {
        return lcnRepository.findByTypeAndMontantAndStatutNot(Type.DECAISSEMENT, montant, StatutMoyenPaiement.ANNULE)
                .stream().map(LcnAvanceReader::toResume).toList();
    }

    private static MoyenPaiementResume toResume(Lcn l) {
        return new MoyenPaiementResume(TypeMoyenPaiement.LCN, l.getId(), l.getNumero(), l.getMontant(),
                l.getStatut(), l.getType() == Type.DECAISSEMENT, l.getDateEmission(), l.getDateOperation(),
                l.getDateValeur(), l.getBeneficiaire(), l.getBanque().getNom());
    }
}
