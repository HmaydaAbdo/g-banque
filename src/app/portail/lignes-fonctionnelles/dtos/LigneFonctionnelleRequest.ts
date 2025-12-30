  import {SousLigneRequestDto} from "./SousLigneRequestDto";

  export interface LigneFonctionnelleRequest {
    banqueId: number;
    natureLigneFonctionnelleId: number; // New: ID of the NatureLigneFonctionnelle entity
    marcheId?: number | null;
    plafond: number;
    taux: number;
    dateDepart: Date;
    dateFin: Date;
    sousLignes:SousLigneRequestDto[]
  }
