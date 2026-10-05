/**
 * Colonnes exportables (PDF et Excel) des listes de contrats leasing — clés et ordre alignés sur
 * `LEASING_EXPORT_COLUMNS` côté backend (`ContratLeasingServiceImpl`).
 */
export const CONTRAT_LEASING_EXPORT_COLUMNS: ReadonlyArray<{ key: string; label: string }> = [
  { key: 'numeroContrat',   label: 'N° Contrat' },
  { key: 'banque',          label: 'Banque' },
  { key: 'organisme',       label: 'Organisme Leasing' },
  { key: 'immobilisation',  label: 'Immobilisation' },
  { key: 'type',            label: 'Type' },
  { key: 'montantAchatTTC', label: "Montant d'achat TTC" },
  { key: 'plafond',         label: 'Plafond organisme' },
  { key: 'echeances',       label: 'Échéances' },
  { key: 'dateDebut',       label: 'Date début' },
  { key: 'dateFin',         label: 'Date fin' },
  { key: 'duree',           label: 'Durée' },
  { key: 'statut',          label: 'Statut' },
  { key: 'endettement',     label: 'Endettement' },
  { key: 'resteAPayer',     label: 'Reste à payer' },
  { key: 'dejaPaye',        label: 'Déjà payé' },
];

/** Format d'export choisi dans le panneau d'export. */
export type ContratLeasingExportFormat = 'pdf' | 'excel';
