package staport.finance.gBanque.moyensDePaiements.synchronisation.enums;

/**
 * Statut unique d'un moyen de paiement dans G-Banque.
 *
 * ENCOURS : reçu de FunPaiement (statut=1) ou créé manuellement — non encore réglé.
 * ANNULE  : annulé par FunPaiement (statut=3), uniquement si le statut était ENCOURS.
 *           Un moyen déjà REGLE ne peut pas être passé à ANNULE par FunPaiement.
 * REGLE   : réglé/validé manuellement côté G-Banque (remplace l'ancien active=false).
 */
public enum StatutMoyenPaiement {
    ENCOURS,
    ANNULE,
    REGLE
}
