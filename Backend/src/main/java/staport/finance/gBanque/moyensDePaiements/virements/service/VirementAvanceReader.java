package staport.finance.gBanque.moyensDePaiements.virements.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.moyensDePaiements.lecture.MoyenPaiementReader;
import staport.finance.gBanque.moyensDePaiements.lecture.MoyenPaiementResume;
import staport.finance.gBanque.moyensDePaiements.lecture.TypeMoyenPaiement;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;
import staport.finance.gBanque.moyensDePaiements.virements.entities.Virement;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.moyensDePaiements.virements.repositories.VirementRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

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
    public List<MoyenPaiementResume> findDecaissementsNonAnnulesDeMontant(BigDecimal montant) {
        return virementRepository.findByTypeAndMontantAndStatutNot(Type.DECAISSEMENT, montant, StatutMoyenPaiement.ANNULE)
                .stream().map(VirementAvanceReader::toResume).toList();
    }

    private static MoyenPaiementResume toResume(Virement v) {
        return new MoyenPaiementResume(TypeMoyenPaiement.VIREMENT, v.getId(), v.getNumero(), v.getMontant(),
                v.getStatut(), v.getType() == Type.DECAISSEMENT, v.getDateEmission(), v.getDateOperation(),
                v.getDateValeur(), v.getBeneficiaire(),
                v.getBanqueEmettrice() != null ? v.getBanqueEmettrice().getNom() : null);
    }
}
