export interface CategorieAmountDto {
  categorie: string;
  montant: number;
}

export interface DecaissementSectionDto {
  categories: CategorieAmountDto[];
  total: number;
}

export interface EncaissementSectionDto {
  categories: CategorieAmountDto[];
  total: number;
}

export interface ResumeSectionDto {
  soldeActuel: number;
  plafondLigne: number;
  disponibilite: number;
  encoursPlus: number;
  encoursMoins: number;
  soldeProjete: number;
}

export interface RecapGeneralDetailleDto {
  banqueId: number;
  banqueNom: string;
  dateRecap: string;
  resume: ResumeSectionDto;
  decaissements: DecaissementSectionDto;
  encaissements: EncaissementSectionDto;
  soldePrevisionnelFinMois: number;
}

export interface RecapGeneralDetailleAllBanquesDto {
  dateRecap: string;
  banques: RecapGeneralDetailleDto[];
}
