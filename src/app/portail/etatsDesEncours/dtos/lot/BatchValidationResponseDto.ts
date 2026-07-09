import {OperationValideeDto} from "./OperationValideeDto";

export interface BatchValidationResponseDto {
  operationsValidees: OperationValideeDto[];
  nombreOperations: number;
  montantTotalDebit: number;
  montantTotalCredit: number;
}
