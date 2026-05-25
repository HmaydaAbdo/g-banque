import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs';
import { CommonModule, DecimalPipe } from '@angular/common';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';

import { BankStateService } from '../../../../comptes-bancaire/services/bank-state.service';
import { RapprochementService } from '../../../../rapprochement/service/rapprochement-service.service';
import { TransactionService } from '../../../../transactions/service/TransactionService';

import { RecapitulatifCompte } from '../../../../accueil/dtos/RecapitulatifCompte';
import { RapprochementActifsDto } from '../../../../rapprochement/dtos/RapprochementActifsDto';
import { AgreratedTransactionResponseDto } from '../../../../transactions/dtos/AgreratedTransactionResponseDto';
import { TransactionSearchDto } from '../../../../transactions/dtos/TransactionSearchDto';

@Component({
  selector: 'app-rapprochement-context-header',
  standalone: true,
  imports: [CommonModule, DecimalPipe, TagModule, TooltipModule],
  templateUrl: './rapprochement-context-header.component.html',
  styleUrl: './rapprochement-context-header.component.scss'
})
export class RapprochementContextHeaderComponent implements OnInit, OnDestroy {

  bankInfo: RecapitulatifCompte | null = null;
  actifs: RapprochementActifsDto | null = null;
  agregats: AgreratedTransactionResponseDto | null = null;

  banqueId!: number;
  rapprochementId: number | null = null;
  isProvisoire = false;

  loading = true;
  private bankInfoInitialized = false;

  private subs: Subscription[] = [];

  constructor(
    private route: ActivatedRoute,
    private bankStateService: BankStateService,
    private rapprochementService: RapprochementService,
    private transactionService: TransactionService
  ) {}

  ngOnInit(): void {
    // banqueId depuis les params de la route parente
    this.subs.push(
      this.route.paramMap.subscribe(params => {
        const id = params.get('banqueId');
        if (id) {
          this.banqueId = +id;
          this.subs.push(
            this.bankStateService.bankInfo$.subscribe(info => {
              if (info && info.bankId === this.banqueId) {
                this.bankInfo = info;
                // Après le premier chargement, chaque nouvelle émission = validation effectuée
                if (this.bankInfoInitialized && this.rapprochementId) {
                  this.loadAggregates();
                }
                this.bankInfoInitialized = true;
              }
            })
          );
          this.bankStateService.loadBankInfo(this.banqueId).subscribe();
        }
      })
    );

    // rapprochementId depuis les queryParams
    this.subs.push(
      this.route.queryParamMap.subscribe(qp => {
        const rid = qp.get('rapprochementId');
        this.rapprochementId = rid ? +rid : null;
        if (this.banqueId) {
          this.loadData();
        }
      })
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

    this.loadAggregates();
  }

  private loadAggregates(): void {
    if (!this.rapprochementId || !this.banqueId) return;
    const searchDto: TransactionSearchDto = { rapprochementId: this.rapprochementId };
    this.transactionService.getTransactionsByBanque(this.banqueId, searchDto, 0, 1, 'createdAt,desc')
      .subscribe({
        next: (data) => { this.agregats = data; },
        error: () => {}
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
