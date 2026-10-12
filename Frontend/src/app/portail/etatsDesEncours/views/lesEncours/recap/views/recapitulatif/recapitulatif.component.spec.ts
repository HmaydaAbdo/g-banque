import { TestBed } from '@angular/core/testing';

import { RecapitulatifComponent } from './recapitulatif.component';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';
import { testProviders } from '../../../../../../../../testing/test-providers';
import { TransactionSource } from '../../../../../../transactions/dtos/TransactionSource';
import { SensOperation } from '../../../../../../transactions/dtos/SensOperation';

describe('RecapitulatifComponent', () => {
  /** Crée l'écran comme le routeur le ferait, avec l'état de l'écran dans les queryParams. */
  function creer(queryParams: Record<string, string> = {}): RecapitulatifComponent {
    TestBed.configureTestingModule({
      providers: [
        ...testProviders,
        // Écran enfant de /etats-des-encours/:banqueId : la banque est lue sur la route parente
        // et l'état de l'écran (source, filtres, page) dans les queryParams de la route
        {
          provide: ActivatedRoute,
          useValue: {
            parent: { paramMap: of(convertToParamMap({ banqueId: '1' })) },
            snapshot: { queryParamMap: convertToParamMap(queryParams) },
            queryParamMap: of(convertToParamMap(queryParams)),
          },
        },
      ],
      imports: [RecapitulatifComponent]
    });
    const fixture = TestBed.createComponent(RecapitulatifComponent);
    fixture.detectChanges();
    return fixture.componentInstance;
  }

  // Les critères réellement envoyés à l'API pour le tableau de la source ouverte.
  const dto = (c: RecapitulatifComponent) => (c as any).buildSearchDto();

  it('should create', () => {
    expect(creer()).toBeTruthy();
  });

  it('démarre sur la vue d\'ensemble quand l\'adresse ne porte aucune source', () => {
    expect(creer().openCard).toBeNull();
  });

  it('rouvre la source, les filtres et la page portés par l\'adresse (retour d\'un formulaire)', () => {
    const c = creer({ source: 'CHEQUE', q: 'loyer', 's.numero': '616298', page: '3', sort: 'montant,asc' });
    expect(c.openCard).toBe(TransactionSource.CHEQUE);
    expect(c.searchForm.value.libelleOuObservation).toBe('loyer');
    expect(c.sourceFilterForm.value.numero).toBe('616298');
    expect(c.tableauInitial).toEqual({ first: 20, rows: 10, sortField: 'montant', sortOrder: 1 });
    expect(c.sortParDefaut).toBeFalse();
  });

  it('le filtre de la source remplace le filtre global équivalent, les autres restent globaux', () => {
    const c = creer({ source: 'CHEQUE', q: 'loyer', sens: 'ENCAISSEMENT', 's.q': 'remise' });
    expect(dto(c).libelleOuObservation).toBe('remise');
    expect(dto(c).sens).toBe(SensOperation.ENCAISSEMENT);
    expect(dto(c).transactionSources).toEqual([TransactionSource.CHEQUE]);
    expect(c.surchargesActives).toEqual(['Libellé / Observation']);
  });

  it('envoie le motif de la source vers le bon critère', () => {
    const impot = creer({ source: 'IMPOT_TAXE', 's.motif': 'TVA' });
    expect(dto(impot).impotEtTaxeMotif).toBe('TVA');
    expect(dto(impot).commissionOperation).toBeUndefined();
  });

  it('ne propose N° de pièce et Sens que pour les sources qui en ont', () => {
    const cheque = creer({ source: 'CHEQUE' });
    expect(cheque.filtreNumeroDisponible).toBeTrue();
    expect(cheque.filtreSensDisponible).toBeTrue();
    expect(cheque.filtresAbsents).toBe('');

    TestBed.resetTestingModule();
    const commission = creer({ source: 'COMMISSION' });
    expect(commission.filtreNumeroDisponible).toBeFalse();
    expect(commission.filtreSensDisponible).toBeFalse();
    expect(commission.motifOptionsSource.length).toBeGreaterThan(0);
  });
});

describe('RecapitulatifComponent — adresse (routeur réel)', () => {
  async function ouvrir(url: string) {
    TestBed.configureTestingModule({
      providers: [
        ...testProviders,
        // Routes réelles, déclarées après testProviders : elles remplacent son routeur vide
        provideRouter([
          {
            path: 'b/:banqueId',
            children: [{ path: 'recap', component: RecapitulatifComponent }],
          },
        ]),
      ],
    });
    const harness = await RouterTestingHarness.create();
    const component = await harness.navigateByUrl(url, RecapitulatifComponent);
    harness.detectChanges();
    return { harness, component, router: TestBed.inject(Router) };
  }

  it('écrit la source ouverte dans l\'adresse, sans ajouter d\'entrée d\'historique, et reconnaît sa propre écriture', async () => {
    const { harness, component, router } = await ouvrir('/b/1/recap');
    const entreesAvant = history.length;

    component.openSource(TransactionSource.CHEQUE);
    await harness.fixture.whenStable();

    expect(router.url).toContain('source=CHEQUE');
    expect(component.openCard).toBe(TransactionSource.CHEQUE);
    expect(history.length).toBe(entreesAvant);

    component.openSource(TransactionSource.Virement);
    await harness.fixture.whenStable();
    expect(router.url).toContain('source=Virement');
    expect(router.url).not.toContain('CHEQUE');
  });

  it('revient à la vue d\'ensemble et vide les paramètres de source', async () => {
    const { harness, component, router } = await ouvrir('/b/1/recap?source=CHEQUE&page=2&s.q=remise');
    expect(component.openCard).toBe(TransactionSource.CHEQUE);

    component.closeSource();
    await harness.fixture.whenStable();

    expect(component.openCard).toBeNull();
    expect(router.url).not.toContain('source=');
    expect(router.url).not.toContain('s.q');
  });

  it('relit l\'état quand l\'adresse change de l\'extérieur (menu latéral, historique)', async () => {
    const { harness, component, router } = await ouvrir('/b/1/recap?source=CHEQUE');
    await router.navigateByUrl('/b/1/recap');
    harness.detectChanges();
    expect(component.openCard).toBeNull();
  });
});
