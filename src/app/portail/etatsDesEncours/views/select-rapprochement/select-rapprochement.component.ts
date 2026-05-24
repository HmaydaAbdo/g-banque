import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule, DecimalPipe } from '@angular/common';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { DividerModule } from 'primeng/divider';
import { TooltipModule } from 'primeng/tooltip';
import { RapprochementService } from '../../../rapprochement/service/rapprochement-service.service';
import { RapprochementActifsDto } from '../../../rapprochement/dtos/RapprochementActifsDto';
import { BankDetailsComponent } from '../shared-componanat/bank-details/bank-details.component';

@Component({
  selector: 'app-select-rapprochement',
  standalone: true,
  imports: [
    CommonModule, DecimalPipe, ProgressSpinnerModule, CardModule,
    ButtonModule, TagModule, DividerModule, TooltipModule, BankDetailsComponent
  ],
  providers: [DecimalPipe],
  templateUrl: './select-rapprochement.component.html',
  styleUrl: './select-rapprochement.component.scss'
})
export class SelectRapprochementComponent implements OnInit {
  banqueId!: number;
  actifs: RapprochementActifsDto | null = null;
  loading = true;
  error: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private rapprochementService: RapprochementService
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      this.banqueId = Number(params.get('banqueId'));
      this.loadActifs();
    });
  }

  private loadActifs(): void {
    this.loading = true;
    this.rapprochementService.getActifs(this.banqueId).subscribe({
      next: (data) => {
        this.actifs = data;
        this.loading = false;
      },
      error: (err) => {
        this.error = err.error?.message ?? 'Impossible de charger les rapprochements actifs.';
        this.loading = false;
      }
    });
  }

  selectRapprochement(rapprochementId: number): void {
    this.router.navigate(
      ['/portail/etats-des-encours', this.banqueId],
      { queryParams: { rapprochementId } }
    );
  }

  formatPeriode(periode: string | null): string {
    if (!periode) return '';
    const [year, month] = periode.split('-');
    const months = ['Jan','Fév','Mar','Avr','Mai','Jun','Jul','Aoû','Sep','Oct','Nov','Déc'];
    return `${months[+month - 1]} ${year}`;
  }

  getVariation(soldeFin: number | null, soldeDebut: number | null): number {
    if (soldeFin == null || soldeDebut == null) return 0;
    return soldeFin - soldeDebut;
  }
}
