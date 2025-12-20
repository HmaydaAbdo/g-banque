

export interface EncoursCMTRequestDTO {
  banqueId: number;
  designation:string
  montant:number
  page?: number;
  size?: number;
  sort?: string;
}
