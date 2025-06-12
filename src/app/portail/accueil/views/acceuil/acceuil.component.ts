import {Component, OnInit} from '@angular/core';
import {Button} from "primeng/button";
import {RecapitulatifCompte} from "../../dtos/RecapitulatifCompte";
import {TooltipModule} from "primeng/tooltip";
import { CommonModule } from '@angular/common';
import {BanqueService} from "../../../comptes-bancaire/services/BanqueService";
import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { provideAnimations } from '@angular/platform-browser/animations';
import {RouterLink} from "@angular/router";


@Component({
  selector: 'app-accueil',
  standalone: true,
  imports: [
    Button,
    TooltipModule,
    CommonModule,
    ToastModule,
    RouterLink
  ],
  templateUrl: './acceuil.component.html',
  styleUrl: './acceuil.component.scss',
  providers: [MessageService, provideAnimations()]
})
export class AcceuilComponent  implements OnInit{

  recapitulatifComptes: RecapitulatifCompte[] = [];

  constructor(
    private banqueService: BanqueService,
    private messageService: MessageService
  ) {}

  ngOnInit(): void {
    this.fetchAccueilBankInfo();
  }

  fetchAccueilBankInfo(): void {
    this.banqueService.getAccueilBankInfo().subscribe({
      next: (data) => {
        this.recapitulatifComptes = data;

        // Show success toast
        this.messageService.add({
          severity: 'success',
          summary: 'Succès',
          detail: 'Les informations des banques ont été chargées.',
          life: 3000
        });
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
