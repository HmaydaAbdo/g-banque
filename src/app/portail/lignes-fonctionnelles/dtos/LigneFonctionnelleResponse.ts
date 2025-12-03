import {SousLigneResponseDto} from "./SousLigneResponseDto";

export interface LigneFonctionnelleResponse {
  id: number;
  banqueId: number;
  banqueNom: string;
  natureLigneFonctionnelleId: number;
  natureLigneFonctionnelleLabel: string;
  natureLigneFonctionnelleDescription: string;
  plafond: number;
  encours:number
  solde:number
  taux: number;
  dateDepart: Date;
  dateFin: Date;
  dateCreation: Date;
  dateModification: Date;
  actif?: boolean;
  expired:boolean
  ligneMereId:number;
  sousLignes:SousLigneResponseDto[];
  estLigneMere:boolean;
  estSousLigne:boolean;
}
