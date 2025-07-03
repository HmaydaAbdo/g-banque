import { Component, OnInit } from '@angular/core';
import { ChartModule } from 'primeng/chart'; // For p-chart
import { NgIf, NgFor } from '@angular/common'; // For structural directives
import { catchError, of } from 'rxjs';
import {BanqueService} from "../../../comptes-bancaire/services/BanqueService";

@Component({
  selector: 'app-solde-par-banque',
  standalone: true,
  imports: [
    ChartModule, // Import PrimeNG ChartModule
    NgIf,
    NgFor
  ],
  templateUrl: './solde-par-banque.component.html',
  styleUrl: './solde-par-banque.component.scss'
})
export class SoldeParBanqueComponent implements OnInit {

  chartData: any; // Data for the PrimeNG Chart
  chartOptions: any; // Options for the PrimeNG Chart
  loading: boolean = true; // To show loading state
  errorMessage: string | undefined; // To display errors

  constructor(private banqueService: BanqueService) { }

  ngOnInit() {
    this.loadBankBalances();
  }

  /**
   * Loads all bank balances and prepares data for the bar chart.
   */
  loadBankBalances(): void {
    this.loading = true;
    this.errorMessage = undefined;

    this.banqueService.getAllBanques({}) // Fetch all banks without specific criteria, page, size, or sort
      .pipe(
        catchError(error => {
          console.error('Error fetching bank balances:', error);
          this.errorMessage = 'Failed to load bank balances. Please try again later.';
          this.loading = false;
          return of({ content: [], totalElements: 0, totalPages: 0, size: 0, number: 0 }); // Return an empty page response
        })
      )
      .subscribe(pageResponse => {
        const banks = pageResponse.content;

        if (banks && banks.length > 0) {
          // Extract bank names for labels and balances for data
          const labels = banks.map(bank => bank.nom);
          const data = banks.map(bank => bank.soldeActuel);

          // Get colors from document styles (similar to your example)
          const documentStyle = getComputedStyle(document.documentElement);
          const textColor = documentStyle.getPropertyValue('--text-color');
          const textColorSecondary = documentStyle.getPropertyValue('--text-color-secondary');
          const surfaceBorder = documentStyle.getPropertyValue('--surface-border');
          const blue500 = documentStyle.getPropertyValue('--blue-500'); // Example color for bars

          this.chartData = {
            labels: labels,
            datasets: [
              {
                label: 'Solde Actuel',
                data: data,
                backgroundColor: [blue500], // Use a single color or an array of colors for each bar
                borderColor: [blue500],
                borderWidth: 1
              }
            ]
          };

          this.chartOptions = {
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
                text: 'Solde Actuel par Banque',
                color: textColor,
                font: {
                  size: 16
                }
              },
              tooltip: {
                callbacks: {
                  label: function(context: any) {
                    let label = context.dataset.label || '';
                    if (label) {
                      label += ': ';
                    }
                    // Format the balance as currency
                    label += context.raw.toLocaleString('fr-MA', { style: 'currency', currency: 'MAD', minimumFractionDigits: 2, maximumFractionDigits: 2 });
                    return label;
                  }
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
                  color: textColorSecondary,
                  callback: function(value: any) {
                    // Format Y-axis labels as currency
                    return value.toLocaleString('fr-MA', { style: 'currency', currency: 'MAD', minimumFractionDigits: 0, maximumFractionDigits: 0 });
                  }
                },
                grid: {
                  color: surfaceBorder,
                  drawBorder: false
                },
                title: {
                  display: true,
                  text: 'Solde (MAD)',
                  color: textColor
                }
              }
            }
          };
        } else {
          this.errorMessage = 'No bank data available to display.';
          this.chartData = undefined; // Clear chart data if no banks
        }
        this.loading = false;
      });
  }
}
