import { CommissionCondition } from './CommissionCondition';
import { TransactionSource } from '../../../transactions/dtos/TransactionSource';

export interface CommissionConfigResponse {
  id: number;
  banqueId: number;
  banqueNom: string;
  organismeNom?: string | null;
  montant: number;
  type: TransactionSource;
  condition: CommissionCondition;
}
