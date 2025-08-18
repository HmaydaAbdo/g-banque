import {Component, OnInit} from '@angular/core';
import {LigneFonctionnelleChartData} from "../../../lignes-fonctionnelles/dtos/LigneFonctionnelleChartData";
import {RecapitulatifCompte} from "../../../accueil/dtos/RecapitulatifCompte";
import {NatureLigneFonctionnelleResponse} from "../../../lignes-fonctionnelles/dtos/nature-ligne-fonctionnelle/NatureLigneFonctionnelleResponse";
import {LigneFonctionnelleService} from "../../../lignes-fonctionnelles/services/LigneFonctionnelleService";
import {BanqueService} from "../../../comptes-bancaire/services/BanqueService";
import {NatureLigneFonctionnelleService} from "../../../lignes-fonctionnelles/services/nature-ligne-fonctionnelle.service";
import {catchError} from "rxjs/operators";
import {of} from "rxjs";
import {LigneFonctionnelleChartHistoryRequest} from "../../../lignes-fonctionnelles/dtos/LigneFonctionnelleChartHistoryRequest";
import {ChartDataset} from "chart.js";
import {ChartModule} from "primeng/chart";
import {CheckboxModule} from "primeng/checkbox";
import {DropdownModule} from "primeng/dropdown";
import {FormsModule} from "@angular/forms";
import {NgClass, NgIf} from "@angular/common";
import {BreadcrumbModule} from "primeng/breadcrumb";
import {MenuItem} from "primeng/api";

@Component({
  selector: 'app-evol-taux-plafond',
  standalone: true,
  imports: [
    ChartModule,
    CheckboxModule,
    DropdownModule,
    FormsModule,
    NgIf,
    BreadcrumbModule,
    NgClass
  ],
  templateUrl: './evol-taux-plafond.component.html',
  styleUrl: './evol-taux-plafond.component.scss'
})
export class EvolTauxPlafondComponent implements OnInit {

  historicalChartData: LigneFonctionnelleChartData | undefined;
  historicalChartOptions: any;
  historicalChartTitle: string = 'Please select a Bank and a Functional Line Nature.'; // Initial title
  originalHistoricalChartData: LigneFonctionnelleChartData | undefined;
  showTaux: boolean = true;
  showPlafond: boolean = true;

  // Properties for the bank dropdown
  banks: RecapitulatifCompte[] = [];
  selectedBank: RecapitulatifCompte | undefined;


  naturesLigneFonctionnelle: NatureLigneFonctionnelleResponse[] = [];
  selectedNatureLigneFonctionnelle: NatureLigneFonctionnelleResponse | undefined;
  items: MenuItem[] | undefined;


  constructor(
    private ligneFonctionnelleService: LigneFonctionnelleService, // Re-inject this service here
    private banqueService: BanqueService,
    private natureLigneFonctionnelleService: NatureLigneFonctionnelleService
  ) {}

  ngOnInit() {
    this.loadBanks();
    this.loadNaturesLigneFonctionnelle();
    this.items = [
      {label:"Accueil", route: '/portail/accueil' , icon: 'pi pi-home' },
      { label: 'Tableau de bord' ,route: '/portail/tableau-de-bord', icon:'pi pi-chart-bar'},
      { label: 'Évolution Taux & Plafond' ,route: '/portail/evolution-taux-plafond', icon:'pi pi-chart-line'}
    ]
  }

  /**
   * Loads the list of banks for the dropdown.
   */
  loadBanks(): void {
    this.banqueService.getAccueilBankInfo().pipe(
      catchError(error => {
        console.error('Error fetching bank information:', error);
        return of([]);
      })
    ).subscribe(data => {
      this.banks = data;
      if (this.banks.length > 0) {
        this.selectedBank = this.banks[0];
        this.loadChartDataIfReady();
      }
    });
  }

  /**
   * Loads the list of NatureLigneFonctionnelle for the dropdown.
   */
  loadNaturesLigneFonctionnelle(): void {
    this.natureLigneFonctionnelleService.getAllNatureLigneFonctionnellesList().pipe(
      catchError(error => {
        console.error('Error fetching nature ligne fonctionnelle information:', error);
        return of([]);
      })
    ).subscribe(data => {
      this.naturesLigneFonctionnelle = data;
      if (this.naturesLigneFonctionnelle.length > 0) {
        this.selectedNatureLigneFonctionnelle = this.naturesLigneFonctionnelle[0];
        this.loadChartDataIfReady();
      }
    });
  }

  /**
   * Called when a bank is selected from the dropdown or when nature changes.
   * This method ensures both bank and nature are selected before fetching chart data.
   */
  onSelectionChange(): void {
    this.loadChartDataIfReady();
  }

  /**
   * Helper function to load chart data only if both bank and nature are selected.
   */
  private loadChartDataIfReady(): void {
    if (this.selectedBank && this.selectedNatureLigneFonctionnelle) {
      console.log('Selected Bank:', this.selectedBank.name, 'ID:', this.selectedBank.bankId);
      console.log('Selected Nature Ligne Fonctionnelle:', this.selectedNatureLigneFonctionnelle.label, 'ID:', this.selectedNatureLigneFonctionnelle.id);

      const request: LigneFonctionnelleChartHistoryRequest = {
        banqueId: this.selectedBank.bankId,
        natureLigneFonctionnelleId: this.selectedNatureLigneFonctionnelle.id
      };

      this.ligneFonctionnelleService.getLigneFonctionnelleChartHistory(request)
        .pipe(
          catchError(error => {
            console.error('Error fetching chart history:', error);
            this.historicalChartData = { labels: [], datasets: [] };
            this.originalHistoricalChartData = undefined;
            this.historicalChartTitle = 'Error loading chart data. Please try again.';
            return of({ labels: [], datasets: [] } as LigneFonctionnelleChartData);
          })
        )
        .subscribe(data => {

          const documentStyle = getComputedStyle(document.documentElement);
          const blue500 = documentStyle.getPropertyValue('--blue-500'); // Example color for Taux
          const green500 = documentStyle.getPropertyValue('--green-500'); // Example color for Plafond
          const blue500_rgba = 'rgba(63, 81, 181, 0.4)'; // Example background for Taux
          const green500_rgba = 'rgba(76, 175, 80, 0.4)'; // Example background for Plafond
          // --- FIX for COLORS END ---

          this.originalHistoricalChartData = {
            labels: data.labels,
            datasets: data.datasets.map(dataset => {
              // Ensure yAxisID and assign colors
              if (dataset.label === 'Taux') {
                return {
                  ...dataset,
                  yAxisID: 'y-axis-taux',
                  borderColor: blue500,        // Line color
                  backgroundColor: blue500_rgba, // Point border color / fill area color if 'fill: true'
                  pointBackgroundColor: blue500, // Point background color
                  tension: 0.4, // Smoothness of the line
                  fill: false // Do not fill area under the line
                };
              } else if (dataset.label === 'Plafond') {
                return {
                  ...dataset,
                  yAxisID: 'y-axis-plafond',
                  borderColor: green500,        // Line color
                  backgroundColor: green500_rgba, // Point border color / fill area color if 'fill: true'
                  pointBackgroundColor: green500, // Point background color
                  tension: 0.4, // Smoothness of the line
                  fill: false // Do not fill area under the line
                };
              }
              return dataset;
            })
          };

          this.setHistoricalChartOptions(
            this.selectedBank!.name,
            this.selectedNatureLigneFonctionnelle!.label
          );
          this.updateChartDatasets();
        });
    } else {
      this.historicalChartData = undefined;
      this.originalHistoricalChartData = undefined;
      this.historicalChartTitle = 'Please select a Bank and a Functional Line Nature.';
      console.log('Please select both a bank and a functional line nature to view chart history.');
    }
  }


  /**
   * Sets the options for the historical Taux/Plafond chart, including a dynamic title
   * and specific Y-axis formatting for percentage and currency.
   * @param bankName The name of the bank.
   * @param natureName The name of the functional line nature.
   */
  setHistoricalChartOptions(bankName: string, natureName: string): void {
    const documentStyle = getComputedStyle(document.documentElement);
    const textColor = documentStyle.getPropertyValue('--text-color');
    const textColorSecondary = documentStyle.getPropertyValue('--text-color-secondary');
    const surfaceBorder = documentStyle.getPropertyValue('--surface-border');

    this.historicalChartTitle = `Évolution du Plafond et du Taux de la Ligne Fonctionnelle ${natureName} chez ${bankName}`;

    this.historicalChartOptions = {
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
          text: this.historicalChartTitle,
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
              if (context.dataset.yAxisID === 'y-axis-taux') {
                // If backend sends 8.0 for 8%, use context.raw + '%'
                // If backend sends 0.08 for 8%, use (context.raw * 100).toFixed(2) + '%'
                label += context.raw + '%';
              } else if (context.dataset.yAxisID === 'y-axis-plafond') {
                label += context.raw.toLocaleString('fr-MA', { style: 'currency', currency: 'MAD', minimumFractionDigits: 0, maximumFractionDigits: 0 });
              } else {
                label += context.raw;
              }
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
        'y-axis-plafond': {
          type: 'linear',
          position: 'left',
          ticks: {
            color: textColorSecondary,
            callback: function(value: any) {
              return value.toLocaleString('fr-MA', { style: 'currency', currency: 'MAD', minimumFractionDigits: 0, maximumFractionDigits: 0 });
            }
          },
          grid: {
            color: surfaceBorder,
            drawBorder: false
          },
          title: {
            display: true,
            text: 'Plafond (MAD)',
            color: textColor
          }
        },
        'y-axis-taux': {
          type: 'linear',
          position: 'right',
          ticks: {
            color: textColorSecondary,
            callback: function(value: any) {

              return (value) + '%';
            }
          },
          grid: {
            drawOnChartArea: false,
            color: surfaceBorder,
            drawBorder: false
          },
          title: {
            display: true,
            text: 'Taux (%)',
            color: textColor
          }
        }
      }
    };
  }

  /**
   * Updates the historical chart's datasets based on the showTaux and showPlafond flags.
   */
  updateChartDatasets(): void {
    if (!this.originalHistoricalChartData) {
      this.historicalChartData = { labels: [], datasets: [] };
      return;
    }

    const filteredDatasets: ChartDataset[] = [];

    if (this.showTaux) {
      const tauxDataset = this.originalHistoricalChartData.datasets.find(ds => ds.label === 'Taux');
      if (tauxDataset) {
        filteredDatasets.push(tauxDataset);
      }
    }
    if (this.showPlafond) {
      const plafondDataset = this.originalHistoricalChartData.datasets.find(ds => ds.label === 'Plafond');
      if (plafondDataset) {
        filteredDatasets.push(plafondDataset);
      }
    }

    this.historicalChartData = {
      labels: this.originalHistoricalChartData.labels,
      datasets: filteredDatasets
    };
  }
}
