import {Component, Input, numberAttribute, OnInit} from '@angular/core';
import {RecapitulatifCompte} from "../../../../accueil/dtos/RecapitulatifCompte";
import {BanqueService} from "../../../../comptes-bancaire/services/BanqueService";
import {DecimalPipe, NgIf} from "@angular/common";

@Component({
  selector: 'app-bank-details',
  standalone: true,
  imports: [
    NgIf,
    DecimalPipe
  ],
  templateUrl: './bank-details.component.html',
  styleUrl: './bank-details.component.scss'
})
export class BankDetailsComponent implements OnInit{

  @Input({transform: numberAttribute}) banqueId!: number;

  bankInfo?: RecapitulatifCompte;
  loading = true;

  constructor(private bankService: BanqueService) {}

  ngOnInit() {
    if (!this.banqueId) {
      console.error('banqueId is required for BankInfoComponent');
      this.loading = false;
      return;
    }

    this.bankService.getAccueilBankInfoById(this.banqueId).subscribe({
      next: (data) => {
        this.bankInfo = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Erreur lors du chargement de la banque', err);
        this.loading = false;
      }
    });
  }
}
