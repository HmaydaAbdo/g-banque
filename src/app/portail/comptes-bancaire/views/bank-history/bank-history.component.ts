import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { BanqueResponse } from '../../dtos/BanqueResponse';
import { BanqueService } from '../../services/BanqueService';
import { LigneFonctionnelleService } from '../../../lignes-fonctionnelles/services/LigneFonctionnelleService';
import { LigneFonctionnelleResponse } from '../../../lignes-fonctionnelles/dtos/LigneFonctionnelleResponse';
import { TagModule } from 'primeng/tag';
import { CardModule } from 'primeng/card';
import { PanelModule } from 'primeng/panel';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { DividerModule } from 'primeng/divider';
import { CurrencyPipe, DatePipe } from '@angular/common';


@Component({
  selector: 'app-bank-history',
  standalone: true,
  imports: [
    CommonModule,
    TagModule,
    CardModule,
    PanelModule,
    TableModule,
    ButtonModule,
    RippleModule,
    RouterLink,
    DividerModule,
    CurrencyPipe,
    DatePipe
  ],
  templateUrl: './bank-history.component.html',
  styleUrls: ['./bank-history.component.scss']
})
export class BankHistoryComponent implements OnInit {
  bankId!: number;
  bankDetails: BanqueResponse | undefined;
  bankFunctionalLines: LigneFonctionnelleResponse[] = [];
  loading: boolean = true;
  loadingBankDetails: boolean = true;

  constructor(
    private route: ActivatedRoute,
    private banqueService: BanqueService,
    private ligneFonctionnelleService: LigneFonctionnelleService
  ) { }

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.bankId = +id;
        this.loadBankDetails();
        this.loadBankFunctionalLines();
      }
    });
  }

  loadBankDetails(): void {
    this.loadingBankDetails = true;
    this.banqueService.getBanqueById(this.bankId).subscribe({
      next: (data: BanqueResponse) => {
        this.bankDetails = data;
        this.loadingBankDetails = false;
      },
      error: (error) => {
        console.error('Error loading bank details:', error);
        this.loadingBankDetails = false;
        // Optionally, add a message service here for user feedback
      }
    });
  }

  loadBankFunctionalLines(): void {
    this.loading = true;
    this.ligneFonctionnelleService.getLignesFonctionnellesByBanqueId(this.bankId).subscribe({
      next: (data: LigneFonctionnelleResponse[]) => {
        this.bankFunctionalLines = data;
        this.loading = false;
        console.log(data)
      },
      error: (error) => {
        console.error('Error loading bank functional lines:', error);
        this.loading = false;
        // Optionally, add a message service here for user feedback
      }
    });
  }


}
