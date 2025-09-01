import {Component, OnInit} from '@angular/core';
import {Button} from "primeng/button";
import {RecapitulatifCompte} from "../../dtos/RecapitulatifCompte";
import {TooltipModule} from "primeng/tooltip";
import { CommonModule } from '@angular/common';
import {BanqueService} from "../../../comptes-bancaire/services/BanqueService";
import {MenuItem, MessageService} from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { provideAnimations } from '@angular/platform-browser/animations';
import {RouterLink, RouterLinkActive} from "@angular/router";
import {BreadcrumbModule} from "primeng/breadcrumb";


@Component({
  selector: 'app-accueil',
  standalone: true,
  imports: [
    Button,
    TooltipModule,
    CommonModule,
    ToastModule,
    RouterLink,
    BreadcrumbModule,
    RouterLinkActive
  ],
  templateUrl: './acceuil.component.html',
  styleUrl: './acceuil.component.scss',
  providers: [MessageService, provideAnimations()]
})
export class AcceuilComponent  implements OnInit{

  recapitulatifComptes: RecapitulatifCompte[] = [];
  items: MenuItem[] | undefined;
  constructor(
    private banqueService: BanqueService,
    private messageService: MessageService
  ) {}

  ngOnInit(): void {
    this.items = [{ icon: 'pi pi-home', route: '/portail/accueil', label:"Accueil" } ]
    this.fetchAccueilBankInfo();
  }

  fetchAccueilBankInfo(): void {
    this.banqueService.getAccueilBankInfo().subscribe({
      next: (data) => {
        this.recapitulatifComptes = data;

      },
      error: (err) => {
        console.error('Error fetching bank accueil info:', err);
        // Show error toast
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Échec du chargement des informations des banques: ' + (err.error?.message || err.message),
          life: 5000
        });
      }
    });
  }
}
