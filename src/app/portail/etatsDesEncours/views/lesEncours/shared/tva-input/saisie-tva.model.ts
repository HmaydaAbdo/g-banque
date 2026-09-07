import {ValidationErrors} from '@angular/forms';

/**
 * Saisie de la TVA dans le formulaire de validation (rapprochement).
 *
 * Les deux champs sont mutuellement exclusifs — exactement l'un des deux est renseigné :
 * l'utilisateur choisit un taux dans la liste déroulante, ou saisit directement le montant.
 * La conversion taux → montant reste serveur (voir `TvaResolver` côté backend) : le frontend
 * ne calcule jamais un montant de TVA à partir d'un taux.
 */
export interface SaisieTva {
  /** Taux choisi, en pourcentage (ex : 20 pour 20 %). `null` si un montant a été saisi. */
  tauxTva: number | null;
  /** Montant de TVA saisi directement, en MAD. `null` si un taux a été choisi. */
  montantTva: number | null;
}

/** Taux de TVA en vigueur au Maroc, proposés dans la liste déroulante. */
export const TAUX_TVA: readonly number[] = [0, 10, 14, 20];

/** Valeur sentinelle de la liste déroulante ouvrant la saisie libre du montant. */
export const MODE_SAISIE_MONTANT = 'MONTANT';

/** Saisie par défaut d'un formulaire de validation : taux à 0 %. */
export function saisieTvaParDefaut(): SaisieTva {
  return {tauxTva: 0, montantTva: null};
}

/** Libellé lisible pour les récapitulatifs de confirmation : « 20 % » ou « 246,00 MAD ». */
export function decrireSaisieTva(saisie: SaisieTva | null | undefined): string {
  if (!saisie) {
    return '-';
  }
  if (saisie.tauxTva !== null && saisie.tauxTva !== undefined) {
    return `${saisie.tauxTva} %`;
  }
  if (saisie.montantTva !== null && saisie.montantTva !== undefined) {
    const montant = saisie.montantTva.toLocaleString('fr-FR', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    });
    return `${montant} MAD`;
  }
  return '-';
}

/** Message d'erreur à afficher sous le champ TVA, selon l'erreur remontée par le composant. */
export function messageErreurTva(errors: ValidationErrors | null | undefined): string {
  if (!errors) {
    return '';
  }
  if (errors['montantTvaSuperieurAuTotal']) {
    return 'Le montant de TVA ne peut pas dépasser le montant de l\'opération';
  }
  if (errors['montantTvaNegatif']) {
    return 'Le montant de TVA ne peut pas être négatif';
  }
  if (errors['montantTvaRequis']) {
    return 'Saisir le montant de TVA';
  }
  return 'La TVA est obligatoire';
}
