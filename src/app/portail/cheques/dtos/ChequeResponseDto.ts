import { Type } from '../../Virements/Type';
import { StatutMoyenPaiement } from '../../global/enums/StatutMoyenPaiement';

export interface ChequeResponseDto {
  id: number;
  type: Type;
  emetteur: string;
  numero: string;
  dateEmission: string;
  dateEcheance: string;
  beneficiaire: string;
  montant: number;
  statut: StatutMoyenPaiement;
  nomBanque: string;
  banqueId: number;
  dateOperation: string;
  dateValeur: string;
  libelle: string;
  observation: string;
}
