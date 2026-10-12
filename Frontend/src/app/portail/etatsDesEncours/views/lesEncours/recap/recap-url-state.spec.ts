import { convertToParamMap } from '@angular/router';
import { TransactionSource } from '../../../../transactions/dtos/TransactionSource';
import { SensOperation } from '../../../../transactions/dtos/SensOperation';
import {
  etatVersQueryParams,
  filtresGlobauxVides,
  filtresSourceVides,
  queryParamsVersEtat,
  RecapEtatUrl,
  serialiserParamMap,
  serialiserParams,
  tableauParDefaut,
} from './recap-url-state';

describe('recap-url-state', () => {
  const etatVide = (): RecapEtatUrl => ({
    source: null,
    globaux: filtresGlobauxVides(),
    locaux: filtresSourceVides(),
    tableau: tableauParDefaut(),
    focus: null,
  });

  it('un état vierge ne produit aucun paramètre', () => {
    expect(etatVersQueryParams(etatVide())).toEqual({});
  });

  it('une adresse vide redonne l\'état vierge', () => {
    expect(queryParamsVersEtat(convertToParamMap({}))).toEqual(etatVide());
  });

  it('restitue source, filtres globaux, filtres de la source, page, tri et ligne éditée', () => {
    const etat: RecapEtatUrl = {
      source: TransactionSource.CHEQUE,
      globaux: {
        transactionSources: [TransactionSource.CHEQUE, TransactionSource.Virement],
        sens: SensOperation.ENCAISSEMENT,
      },
      locaux: {
        ...filtresSourceVides(),
        libelleOuObservation: 'remise',
        numero: '616298',
        montant: 1250.5,
        dateReferenceDepuis: new Date(2026, 9, 1),
        dateReferenceJusqua: new Date(2026, 9, 31),
      },
      tableau: { first: 20, rows: 10, sortField: 'montant', sortOrder: 1 },
      focus: 'CHEQUE:4812',
    };

    const params = etatVersQueryParams(etat);
    expect(params['page']).toBe(3);
    expect(params['sort']).toBe('montant,asc');
    expect(params['du']).toBe('2026-10-01');

    const lu = queryParamsVersEtat(convertToParamMap(
      Object.fromEntries(Object.entries(params).map(([k, v]) => [k, String(v)]))));
    expect(lu).toEqual(etat);
  });

  it('omet page, taille et tri quand ils valent leur valeur par défaut', () => {
    const params = etatVersQueryParams({ ...etatVide(), source: TransactionSource.COMMISSION });
    expect(params).toEqual({ source: 'COMMISSION' });
  });

  it('ignore les filtres et le tableau d\'une source quand aucune source n\'est ouverte', () => {
    const etat = queryParamsVersEtat(convertToParamMap({ q: 'x', montant: '5', page: '4', sort: 'montant,asc', sens: 'DECAISSEMENT' }));
    expect(etat.source).toBeNull();
    expect(etat.globaux.sens).toBe(SensOperation.DECAISSEMENT);
    expect(etat.locaux).toEqual(filtresSourceVides());
    expect(etat.tableau).toEqual(tableauParDefaut());
  });

  it('retombe sur les valeurs neutres pour toute valeur inconnue ou illisible', () => {
    const etat = queryParamsVersEtat(convertToParamMap({
      source: 'CHEQUE',
      sens: 'NIMPORTEQUOI',
      types: 'CHEQUE,INCONNU',
      montant: 'abc',
      rows: '7',
      page: '-3',
      sort: 'password,asc',
      motif: 'TVA',
    }));
    expect(etat.globaux.sens).toBeNull();
    expect(etat.globaux.transactionSources).toEqual([TransactionSource.CHEQUE]);
    expect(etat.locaux.montant).toBeNull();
    expect(etat.tableau).toEqual(tableauParDefaut());
    expect(etat.locaux.motif).toBeNull();
  });

  it('n\'accepte un motif que pour Commission et Impôt/Taxe', () => {
    const impot = queryParamsVersEtat(convertToParamMap({ source: 'IMPOT_TAXE', motif: 'TVA' }));
    expect(impot.locaux.motif).toBe('TVA');
    const commission = queryParamsVersEtat(convertToParamMap({ source: 'COMMISSION', motif: 'CHEQUE' }));
    expect(commission.locaux.motif).toBe('CHEQUE');
    const autre = queryParamsVersEtat(convertToParamMap({ source: 'CHEQUE', motif: 'TVA' }));
    expect(autre.locaux.motif).toBeNull();
  });

  it('reconnaît ses propres écritures d\'adresse quel que soit le type des valeurs', () => {
    const params = { montant: 12.5, source: 'CHEQUE', page: 2 };
    const map = convertToParamMap({ montant: '12.5', source: 'CHEQUE', page: '2' });
    expect(serialiserParamMap(map)).toBe(serialiserParams(params));
  });
});
