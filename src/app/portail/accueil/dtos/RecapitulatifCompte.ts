
export interface RecapitulatifCompte {
  bankId: number;
  name: string; // Matches backend 'name'
  logoBase64: string; // Matches backend 'logoBase64'
  soldeActuelle: number; // Matches backend 'soldeActuelle'
  plafondFC?: number; // Matches backend 'plafondFC'
}
