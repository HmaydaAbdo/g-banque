import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Subscription, debounceTime } from 'rxjs';
import { CommonModule, DecimalPipe } from '@angular/common';
import { TagModule } from 'primeng/tag';

import { BankStateService } from '../../../../comptes-bancaire/services/bank-state.service';
import { RapprochementService } from '../../../../rapprochement/service/rapprochement-service.service';

import { RecapitulatifCompte } from '../../../../accueil/dtos/RecapitulatifCompte';
import { RapprochementActifsDto } from '../../../../rapprochement/dtos/RapprochementActifsDto';

@Component({
  selector: 'app-rapprochement-context-header',
  standalone: true,
  imports: [CommonModule, DecimalPipe, TagModule],
  templateUrl: './rapprochement-context-header.component.html',
  styleUrl: './rapprochement-context-header.component.scss'
})
export class RapprochementContextHeaderComponent implements OnInit, OnDestroy {

  bankInfo: RecapitulatifCompte | null = null;
  actifs: RapprochementActifsDto | null = null;

  banqueId!: number;
  rapprochementId: number | null = null;
  isProvisoire = false;

  loading = true;

  private subs: Subscription[] = [];

  private readonly route = inject(ActivatedRoute);
  private readonly bankStateService = inject(BankStateService);
  private readonly rapprochementService = inject(RapprochementService);

  ngOnInit(): void {
    // banqueId et rapprochementId sont tous les deux des path params (paramsInheritanceStrategy: 'always')
    this.subs.push(
      this.route.paramMap.subscribe(params => {
        const bid = params.get('banqueId');
        const rid = params.get('rapprochementId');

        if (bid) {
          this.banqueId = +bid;
          this.subs.push(
            this.bankStateService.bankInfo$.subscribe(info => {
              if (info && info.banqueId === this.banqueId) {
                this.bankInfo = info;
              }
            })
          );
          this.bankStateService.loadBankInfo(this.banqueId).subscribe();
        }

        if (rid) {
          this.rapprochementId = +rid;
        }

        if (this.banqueId && this.rapprochementId) {
          this.loadData();
        }
      })
    );

    // Rafraîchir le header après chaque validation réussie dans un composant enfant
    this.subs.push(
      this.bankStateService.validationDone$
        .pipe(debounceTime(300))
        .subscribe(() => this.loadData())
    );
  }

  private loadData(): void {
    if (!this.banqueId || !this.rapprochementId) return;
    this.loading = true;

    // Charger actifs pour statut + soldes
    this.rapprochementService.getActifs(this.banqueId).subscribe({
      next: (actifs) => {
        this.actifs = actifs;
        this.isProvisoire = !!actifs.provisoireId && this.rapprochementId === actifs.provisoireId;
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  getSoldeDebut(): number {
    if (!this.actifs) return 0;
    return this.isProvisoire
      ? (this.actifs.provisoireSoldeDebut ?? 0)
      : this.actifs.enCoursSoldeDebut;
  }

  getSoldeFin(): number {
    if (!this.actifs) return 0;
    return this.isProvisoire
      ? (this.actifs.provisoireSoldeFin ?? 0)
      : this.actifs.enCoursSoldeFin;
  }

  getVariation(): number {
    return this.getSoldeFin() - this.getSoldeDebut();
  }

  getPeriode(): string {
    if (!this.actifs) return '';
    const raw = this.isProvisoire ? this.actifs.provisoirePeriode : this.actifs.enCoursPeriode;
    return this.formatPeriode(raw);
  }

  formatPeriode(periode: string | null | undefined): string {
    if (!periode) return '';
    const [year, month] = periode.split('-');
    const months = ['Jan','Fév','Mar','Avr','Mai','Jun','Jul','Aoû','Sep','Oct','Nov','Déc'];
    return `${months[+month - 1]} ${year}`;
  }

  ngOnDestroy(): void {
    this.subs.forEach(s => s.unsubscribe());
  }
}
