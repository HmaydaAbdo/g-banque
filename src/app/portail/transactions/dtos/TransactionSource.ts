export enum TransactionSource {
  CMT = "CMT",
  LEASING = "LEASING",
  CHEQUE = "CHEQUE",
  LCN = "LCN",
  Virement = "Virement",
  MiseADisposition = "MiseADisposition",
  PREFINANCEMENT = "PREFINANCEMENT",
  DROIT_CONSTATE = "DROIT_CONSTATE",
  DECOMPTES = "DECOMPTES",
  COMMISSION = "COMMISSION",
  AGIOS = "AGIOS",
  FRAIS_BANCAIRE = "FRAIS_BANCAIRE",
  FACTORING = "FACTORING",
  PRELEVEMENT = "PRELEVEMENT",
  CAUTION = "CAUTION",
  IMPOT_TAXE = "IMPOT_TAXE",
  RESTITUTION = "RESTITUTION",
  OPERATION_ERRONE = "OPERATION_ERRONE",
  VIREMENT_INTERNATIONAL="VIREMENT_INTERNATIONAL",
  IMPAYEE="IMPAYEE"
}


export const TransactionSourceLabels: Record<TransactionSource, string> = {
  [TransactionSource.CMT]: "Crédit Moyen Terme",
  [TransactionSource.LEASING]: "Leasing",
  [TransactionSource.CHEQUE]: "Chèque",
  [TransactionSource.LCN]: "LCN",
  [TransactionSource.Virement]: "Virement",
  [TransactionSource.MiseADisposition]: "Mise à disposition",
  [TransactionSource.PREFINANCEMENT]: "Préfinancement",
  [TransactionSource.DROIT_CONSTATE]: "Droit constaté",
  [TransactionSource.DECOMPTES]: "Décomptes",
  [TransactionSource.COMMISSION]: "Commission",
  [TransactionSource.AGIOS]: "Agios",
  [TransactionSource.FRAIS_BANCAIRE]: "Frais bancaire",
  [TransactionSource.FACTORING]: "Factoring",
  [TransactionSource.PRELEVEMENT]: "Prélèvement",
  [TransactionSource.CAUTION]: "Caution",
  [TransactionSource.IMPOT_TAXE]: "Impôt / Taxe",
  [TransactionSource.RESTITUTION]: "Restitution",
  [TransactionSource.OPERATION_ERRONE]: "Opération erronée",
  [TransactionSource.VIREMENT_INTERNATIONAL]: "Virement international",
  [TransactionSource.IMPAYEE]: "Impayé"
};



