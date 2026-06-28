import { CommissionCondition } from './CommissionCondition';
import { TransactionSource } from '../../../transactions/dtos/TransactionSource';

export interface CommissionConfigRequest {
  banqueId: number;
  organismeNom?: string | null;
  montant: number;
  type: TransactionSource;
  condition: CommissionCondition;
}
