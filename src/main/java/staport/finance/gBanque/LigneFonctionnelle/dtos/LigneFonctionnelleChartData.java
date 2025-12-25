package staport.finance.gBanque.LigneFonctionnelle.dtos;



import java.math.BigDecimal;
import java.util.List;


public class LigneFonctionnelleChartData {
    private List<String> labels; // e.g., ['2024', '2025', '2026']
    private List<ChartDataset> datasets; // Contains 'Taux' and 'Plafond' datasets

    // Constructors, Getters, and Setters
    public LigneFonctionnelleChartData() {
    }

    public LigneFonctionnelleChartData(List<String> labels, List<ChartDataset> datasets) {
        this.labels = labels;
        this.datasets = datasets;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public List<ChartDataset> getDatasets() {
        return datasets;
    }

    public void setDatasets(List<ChartDataset> datasets) {
        this.datasets = datasets;
    }

    // Inner class for Chart Dataset
    public static class ChartDataset {
        private String label; // e.g., 'Taux' or 'Plafond'
        private List<BigDecimal> data; // The actual data points
        private boolean fill;
        private String borderColor;
        private double tension;

        public ChartDataset() {
        }

        public ChartDataset(String label, List<BigDecimal> data, boolean fill, String borderColor, double tension) {
            this.label = label;
            this.data = data;
            this.fill = fill;
            this.borderColor = borderColor;
            this.tension = tension;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public List<BigDecimal> getData() {
            return data;
        }

        public void setData(List<BigDecimal> data) {
            this.data = data;
        }

        public boolean isFill() {
            return fill;
        }

        public void setFill(boolean fill) {
            this.fill = fill;
        }

        public String getBorderColor() {
            return borderColor;
        }

        public void setBorderColor(String borderColor) {
            this.borderColor = borderColor;
        }

        public double getTension() {
            return tension;
        }

        public void setTension(double tension) {
            this.tension = tension;
        }
    }
}