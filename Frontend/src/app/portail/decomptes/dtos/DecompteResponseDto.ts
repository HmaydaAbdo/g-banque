import {Status} from "../../prefinancements/enums/Status";

export interface DecompteResponseDto {
  id: number;

  marcheId: number;
  marcheName: string;
  marcheNumero: string;
  marcheBanqueId: number;
  marcheBanqueName: string;

  banqueEncaissementId: number;
  banqueEncaissementName: string;

  dateEmission: string;
  montant: number;
  observation: string;
  dateOperation: string;
  dateValeur: string;
  status: Status;
  libelle: string;

}
