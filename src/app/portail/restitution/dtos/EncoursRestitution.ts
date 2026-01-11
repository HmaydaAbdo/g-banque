import {PageResponse} from "../../global/types/PageResponse";
import {RestitutionResponseDto} from "./RestitutionResponseDto";

export interface EncoursRestitution {
  totalDesEncours: number;
  page: PageResponse<RestitutionResponseDto>;
}
