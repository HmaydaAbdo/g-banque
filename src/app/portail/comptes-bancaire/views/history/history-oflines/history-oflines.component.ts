import { Component, inject, OnInit } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import {ActivatedRoute, RouterLink, RouterLinkActive, RouterOutlet} from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';



// PrimeNG Modules
import { CardModule } from 'primeng/card';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { TabViewModule } from 'primeng/tabview'; // Import TabViewModule
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { MenuItem, MessageService } from 'primeng/api';
import {BanqueResponse} from "../../../dtos/BanqueResponse";
import {BanqueService} from "../../../services/BanqueService";
import {GeneralLignesByBankComponent} from "../general-lines-by-bank/general-lignes-by-bank.component";
import {SpecificLignesByBankComponent} from "../specific-lines-by-bank/specific-lignes-by-bank.component";

@Component({
  selector: 'app-history-oflines',
  standalone: true,
  imports: [
    CommonModule,
    CardModule,
    BreadcrumbModule,
    TabViewModule, // Add TabViewModule
    ToastModule,
    ConfirmDialogModule,
    RouterLink,
    RouterLinkActive,
    RouterOutlet, // Add RouterOutlet
    DecimalPipe,
    GeneralLignesByBankComponent,
    SpecificLignesByBankComponent
  ],
  providers: [MessageService],
  templateUrl: './history-oflines.component.html',
  styleUrl: './history-oflines.component.scss'
})
export class HistoryOflinesComponent implements OnInit {

  bankId!: number;
  bankDetails: BanqueResponse | undefined;
  loadingBankDetails: boolean = true;
  items: MenuItem[] | undefined;

  private route = inject(ActivatedRoute);
  private banqueService = inject(BanqueService);
  private messageService = inject(MessageService);

  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label:"Accueil" },
      { icon: 'pi pi-wallet', route: '/portail/comptes-bancaires', label:"Comptes bancaires" },
    ];

    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.bankId = +id;
        this.loadBankDetails();
      }
    });
  }

  loadBankDetails(): void {
    this.loadingBankDetails = true;
    this.banqueService.getBanqueById(this.bankId).subscribe({
      next: (data: BanqueResponse) => {
        this.bankDetails = data;
        this.items?.push({ label: data.nom, route: `/portail/comptes-bancaires/history/${data.id}` });
        this.loadingBankDetails = false;
      },
      error: (error: HttpErrorResponse) => {
        console.error('Error loading bank details:', error);
        this.loadingBankDetails = false;
      }
    });
  }
}
