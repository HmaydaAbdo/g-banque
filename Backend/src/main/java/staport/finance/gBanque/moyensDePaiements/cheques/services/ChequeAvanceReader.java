package staport.finance.gBanque.moyensDePaiements.cheques.services;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.moyensDePaiements.cheques.entities.Cheque;
import staport.finance.gBanque.moyensDePaiements.cheques.repositories.ChequeRepository;
import staport.finance.gBanque.moyensDePaiements.lecture.MoyenPaiementReader;
import staport.finance.gBanque.moyensDePaiements.lecture.MoyenPaiementResume;
import staport.finance.gBanque.moyensDePaiements.lecture.TypeMoyenPaiement;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

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
    public List<MoyenPaiementResume> findDecaissementsNonAnnulesDeMontant(BigDecimal montant) {
        return chequeRepository.findByTypeAndMontantAndStatutNot(Type.DECAISSEMENT, montant, StatutMoyenPaiement.ANNULE)
                .stream().map(ChequeAvanceReader::toResume).toList();
    }

    private static MoyenPaiementResume toResume(Cheque c) {
        return new MoyenPaiementResume(TypeMoyenPaiement.CHEQUE, c.getId(), c.getNumero(), c.getMontant(),
                c.getStatut(), c.getType() == Type.DECAISSEMENT, c.getDateEmission(), c.getDateOperation(),
                c.getDateValeur(), c.getBeneficiaire(), c.getBanque().getNom());
    }
}
