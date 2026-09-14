export type StatutContratCadre = 'ACTIF' | 'EXPIRE' | 'ARCHIVE';

export interface ContratCadreResponse {
  id: number;
  organismeId: number;
  organismeNom: string;
  plafond: number;
  dateDebut: string;
  dateFin: string;
  statut: StatutContratCadre;
  dateCreation: string;
  dateModification: string;
}
