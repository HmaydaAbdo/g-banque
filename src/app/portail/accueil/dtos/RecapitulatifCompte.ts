
export interface RecapitulatifCompte {
  bankId: number;
  name: string;
  logoBase64: string;
  soldeActuelle: number;
  soldeDepart:number
  plafondFC?: number;
}
