package staport.finance.gBanque.leasing.echeancesLeasing.dto;

import staport.finance.gBanque.moyensDePaiements.lecture.TypeMoyenPaiement;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Chèque, LCN ou virement qui règle (ou peut régler) l'avance d'un contrat de leasing. */
public record MoyenPaiementAvanceDTO(
        TypeMoyenPaiement type,
        Long id,
        String numero,
        BigDecimal montant,
        StatutMoyenPaiement statut,
        LocalDate dateEmission,
        LocalDate dateOperation,
        LocalDate dateValeur,
        String beneficiaire,
        String nomBanque
) {}
