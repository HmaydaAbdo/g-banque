import {ChartDataset} from "chart.js";

export interface LigneFonctionnelleChartData {
  labels: string[]; // e.g., ['2024', '2025', '2026']
  datasets: ChartDataset[]; // Array of datasets (Taux, Plafond)
}
