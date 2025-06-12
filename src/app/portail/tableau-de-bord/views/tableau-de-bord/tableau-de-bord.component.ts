import {Component, OnInit} from '@angular/core';
import { ChartModule } from 'primeng/chart';
import {CardModule} from "primeng/card";
@Component({
  selector: 'app-tableau-de-bord',
  standalone: true,
  imports: [
    ChartModule,
    CardModule
  ],
  templateUrl: './tableau-de-bord.component.html',
  styleUrl: './tableau-de-bord.component.scss'
})
export class TableauDeBordComponent implements OnInit {

  // Data for the 'Solde Actuel et Plafond de la Facilité de Caisse par Banque' chart
  bankBalanceChartData: any;
  bankBalanceChartOptions: any;

  // Data for the 'Trésorerie Générale Consolidée'
  consolidatedTreasury: number = 0;

  ngOnInit() {
    // Get CSS variables for consistent theming with PrimeNG
    const documentStyle = getComputedStyle(document.documentElement);
    const textColor = documentStyle.getPropertyValue('--text-color');
    const textColorSecondary = documentStyle.getPropertyValue('--text-color-secondary');
    const surfaceBorder = documentStyle.getPropertyValue('--surface-border');

    // --- Mock Data Simulation ---
    // In a real application, this data would come from your Spring Boot backend API
    const mockBanks = [
      { id: 1, nom: 'Attijariwafa Bank', soldeActuel: 1500000, devise: 'MAD' },
      { id: 2, nom: 'Banque Populaire', soldeActuel: 800000, devise: 'MAD' },
      { id: 3, nom: 'BMCE Bank', soldeActuel: 200000, devise: 'MAD' },
      { id: 4, nom: 'Société Générale', soldeActuel: 500000, devise: 'EUR' },
      { id: 5, nom: 'CIH Bank', soldeActuel: 300000, devise: 'MAD' }
    ];

    const mockLignesFonctionnelles = [
      { id: 1, nature: 'Facilité de Caisse', plafond: 200000, taux: 0.05, banque_id: 1 },
      { id: 2, nature: 'Escompte Commercial', plafond: 500000, taux: 0.04, banque_id: 1 },
      { id: 3, nature: 'Facilité de Caisse', plafond: 100000, taux: 0.06, banque_id: 2 },
      { id: 4, nature: 'Cautions Provisoires', plafond: 300000, taux: 0.02, banque_id: 2 },
      { id: 5, nature: 'Facilité de Caisse', plafond: 50000, taux: 0.07, banque_id: 3 },
      { id: 6, nature: 'Facilité de Caisse', plafond: 150000, taux: 0.055, banque_id: 4 },
      { id: 7, nature: 'ASMN', plafond: 700000, taux: 0.03, banque_id: 5 },
      { id: 8, nature: 'Facilité de Caisse', plafond: 75000, taux: 0.06, banque_id: 5 },
    ];
    // --- End Mock Data Simulation ---

    // --- Prepare Data for Chart 1: Solde Actuel et Plafond de la Facilité de Caisse par Banque ---
    const bankNames: string[] = mockBanks.map(bank => bank.nom);
    const actualBalances: number[] = mockBanks.map(bank => bank.soldeActuel);

    // Calculate total 'Facilité de Caisse' plafond for each bank
    const faciliteCaissePlafonds: number[] = mockBanks.map(bank => {
      return mockLignesFonctionnelles
        .filter(line => line.banque_id === bank.id && line.nature === 'Facilité de Caisse')
        .reduce((sum, line) => sum + line.plafond, 0);
    });

    this.bankBalanceChartData = {
      labels: bankNames,
      datasets: [
        {
          label: 'Solde Actuel (MAD)',
          backgroundColor: documentStyle.getPropertyValue('--blue-500'), // Use a distinct blue for actual balance
          borderColor: documentStyle.getPropertyValue('--blue-500'),
          data: actualBalances
        },
        {
          label: 'Plafond Facilité de Caisse (MAD)',
          backgroundColor: documentStyle.getPropertyValue('--green-500'), // Use a distinct green for overdraft facility
          borderColor: documentStyle.getPropertyValue('--green-500'),
          data: faciliteCaissePlafonds
        }
      ]
    };

    this.bankBalanceChartOptions = {
      maintainAspectRatio: false,
      aspectRatio: 0.8,
      plugins: {
        legend: {
          labels: {
            color: textColor
          }
        },
        title: {
          display: true,
          text: 'Solde Actuel et Plafond de la Facilité de Caisse par Banque',
          color: textColor,
          font: {
            size: 16
          }
        }
      },
      scales: {
        x: {
          ticks: {
            color: textColorSecondary,
            font: {
              weight: 500
            }
          },
          grid: {
            color: surfaceBorder,
            drawBorder: false
          }
        },
        y: {
          ticks: {
            color: textColorSecondary
          },
          grid: {
            color: surfaceBorder,
            drawBorder: false
          }
        }
      }
    };

    // --- Prepare Data for Chart 2: Trésorerie Générale Consolidée ---
    // Sum all actual balances from mock banks
    this.consolidatedTreasury = mockBanks.reduce((sum, bank) => sum + bank.soldeActuel, 0);
  }
}
