import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TransactionSource, TransactionSourceLabels } from '../../../transactions/dtos/TransactionSource';

/** Color category for each source type */
const SOURCE_COLOR: Record<TransactionSource, string> = {
  [TransactionSource.CMT]:                    'var(--color-primary)',
  [TransactionSource.LEASING]:                'var(--color-primary)',
  [TransactionSource.PREFINANCEMENT]:         'var(--color-primary)',
  [TransactionSource.FACTORING]:              'var(--color-primary)',

  [TransactionSource.CHEQUE]:                 'var(--color-info)',
  [TransactionSource.LCN]:                    'var(--color-info)',
  [TransactionSource.LCN_DECAISSEMENT]:       'var(--color-info)',
  [TransactionSource.LCN_ENCAISSEMENT]:       'var(--color-info)',
  [TransactionSource.LCN_ESCOMPTE]:           'var(--color-info)',
  [TransactionSource.Virement]:               'var(--color-info)',
  [TransactionSource.VIREMENT_INTERNATIONAL]: 'var(--color-info)',
  [TransactionSource.MiseADisposition]:       'var(--color-info)',
  [TransactionSource.DECOMPTES]:              'var(--color-info)',
  [TransactionSource.DROIT_CONSTATE]:         'var(--color-info)',

  [TransactionSource.FRAIS_BANCAIRE]:         'var(--color-warning)',
  [TransactionSource.COMMISSION]:             'var(--color-warning)',
  [TransactionSource.AGIOS]:                  'var(--color-warning)',
  [TransactionSource.IMPOT_TAXE]:             'var(--color-warning)',
  [TransactionSource.CAUTION]:                'var(--color-warning)',
  [TransactionSource.PRELEVEMENT]:            'var(--color-warning)',

  [TransactionSource.RESTITUTION]:            'var(--color-success)',

  [TransactionSource.IMPAYEE]:                'var(--color-danger)',
  [TransactionSource.OPERATION_ERRONE]:       'var(--color-danger)',

  [TransactionSource.PROVISION]:              'var(--color-info)',
};

@Component({
  selector   : 'app-source-badge',
  standalone : true,
  imports    : [CommonModule],
  template   : `
    <span class="source-badge" [style.--dot-color]="dotColor">
      <span class="source-badge__dot"></span>
      <span class="source-badge__label">{{ label }}</span>
    </span>
  `,
  styles: [`
    .source-badge {
      display    : inline-flex;
      align-items: center;
      gap        : 0.35rem;
      font-size  : 0.72rem;
      font-weight: 500;
      white-space: nowrap;
    }
    .source-badge__dot {
      width        : 6px;
      height       : 6px;
      border-radius: 50%;
      background   : var(--dot-color, #94a3b8);
      flex-shrink  : 0;
    }
    .source-badge__label {
      color: var(--color-text-secondary);
    }
  `]
})
export class SourceBadgeComponent {
  @Input({ required: true }) source!: TransactionSource | string;

  get label(): string {
    return TransactionSourceLabels[this.source as TransactionSource] ?? this.source;
  }

  get dotColor(): string {
    return SOURCE_COLOR[this.source as TransactionSource] ?? 'var(--color-text-muted)';
  }
}
