import {Status} from "../../prefinancements/enums/Status";
import { TypeAgios } from '../../agios-calcul/enums/TypeAgios';

export type NatureEcartAgios = 'RECLAMATION' | 'BENEFIQUE' | 'AUCUN_ECART';

export interface AgiosResponseDto {
  id: number;
  libelle: string;
  observation: string;
  montant: number;
  banqueId: number;
  banqueNom: string;
  dateEmission: string;
  dateValeur: string;
  dateOperation: string;
  status: Status;
  typeAgios: TypeAgios | null;
  periodicite: 'TRIMESTRIEL' | 'MENSUEL' | null;
  periodeNumero: number | null;
  annee: number | null;
  montantFacture: number | null;
  montantCalcule: number | null;
  /** Facturé moins calculé, en DH, calculé à la lecture et jamais stocké. */
  ecart: number | null;
  natureEcart: NatureEcartAgios | null;
}
