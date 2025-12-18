export interface PdfFileDTO {
  contenu: Uint8Array | ArrayBuffer | Blob;
  nomFichier: string;
  tailleFichier: number;
  dateUpload: string;
}
