import {Type} from "../../Virements/Type";
import {StatutMoyenPaiement} from "../../global/enums/StatutMoyenPaiement";

/**
 * Interface representing the data for a single MiseADisposition resource
 * returned from the API.
 */
export interface MiseADispositionResponseDto {
  id:number
  banqueId:number
  emetteur:string
  type:Type
  numero: string;
  dateEmission: string;
  dateOperation:string;
  dateValeur:string;
  libelle:string;
  observation:string;
  beneficiaire: string;
  montant: number;
  banqueName: string;
  cinBeneficiaire: string;
  statut: StatutMoyenPaiement;
}
