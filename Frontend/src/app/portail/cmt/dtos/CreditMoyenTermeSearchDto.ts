export interface CreditMoyenTermeSearchDto {
  banqueId?: number;
  designation?: string;
  completed?: boolean;
  dateDebutMin?: Date;
  dateDebutMax?: Date;
  page?: number;
  size?: number;
  sort?: string;
}
