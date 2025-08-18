
import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common'; // Import DatePipe
import { ContratLeasingResponseDTO } from '../../dtos/contrat-leasing';
import { ContratLeasingPhaseResponseDTO } from '../../dtos/contrat-leasing-phase';

// PrimeNG Modules used in the template
import { TagModule } from 'primeng/tag';
import { DividerModule } from 'primeng/divider';
import { TableModule } from 'primeng/table';

@Component({
  selector: 'app-contrat-leasing-details',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    TagModule,
    DividerModule,
    TableModule
  ],
  templateUrl: './contrat-leasing-details.component.html',

})
export class ContratLeasingDetailsComponent implements OnChanges {
  // This input will receive the full contract details from the parent component
  @Input() contratDetails: ContratLeasingResponseDTO | null = null;
  contratPhases: ContratLeasingPhaseResponseDTO[] = [];

  // This lifecycle hook is used to react to changes in the @Input() properties
  // It's crucial here to populate contratPhases when contratDetails is set or updated.
  ngOnChanges(changes: SimpleChanges): void {
    if (changes['contratDetails'] && this.contratDetails) {
      this.contratPhases = this.contratDetails.phases || [];
    }
  }
}
