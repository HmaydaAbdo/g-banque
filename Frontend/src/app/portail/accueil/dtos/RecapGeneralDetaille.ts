import {PeriodeRecap} from "../../etatsDesEncours/views/lesEncours/recap/dtos/PeriodeRecap";

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
  /** Horizon de la partie « à venir » : fin de mois (défaut) ou fin de trimestre. */
  periode: PeriodeRecap;
  dateHorizon: string;
  resume: ResumeSectionDto;
  decaissements: DecaissementSectionDto;
  encaissements: EncaissementSectionDto;
  soldePrevisionnel: number;
}

export interface RecapGeneralDetailleAllBanquesDto {
  dateRecap: string;
  periode: PeriodeRecap;
  dateHorizon: string;
  banques: RecapGeneralDetailleDto[];
}
