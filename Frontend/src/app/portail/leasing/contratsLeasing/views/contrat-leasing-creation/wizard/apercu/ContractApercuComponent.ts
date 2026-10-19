import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { AsyncPipe, DatePipe, DecimalPipe } from '@angular/common';
import { LeasingContractWizardService } from '../../../../services/LeasingContractWizardService';

/**
 * Aperçu en direct du plan du contrat, affiché à côté de chaque étape du wizard : décomposition avance /
 * mensualités / valeur résiduelle, couverture de la durée par les phases, endettement et coût du financement.
 * Présentation seule — tous les calculs viennent de {@link LeasingContractWizardService}.
 */
@Component({
  selector: 'app-contract-apercu',
  standalone: true,
  imports: [AsyncPipe, DatePipe, DecimalPipe],
  templateUrl: './contract-apercu.component.html',
  styleUrls: ['./contract-apercu.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ContractApercuComponent {

  private readonly wizardService = inject(LeasingContractWizardService);

  readonly apercu$ = this.wizardService.apercu$;
}
