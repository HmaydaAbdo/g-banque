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

  it("démarre sur la vue d'ensemble quand l'adresse ne porte aucune source", () => {
    expect(creer().openCard).toBeNull();
  });

  it("rouvre la source, les filtres et la page portés par l'adresse (retour d'un formulaire)", () => {
    const c = creer({ source: 'CHEQUE', sens: 'ENCAISSEMENT', numero: '616298', page: '3', sort: 'montant,asc' });
    expect(c.openCard).toBe(TransactionSource.CHEQUE);
    expect(c.searchForm.value.sens).toBe(SensOperation.ENCAISSEMENT);
    expect(c.sourceFilterForm.value.numero).toBe('616298');
    expect(c.tableauInitial).toEqual({ first: 20, rows: 10, sortField: 'montant', sortOrder: 1 });
    expect(c.sortParDefaut).toBeFalse();
  });

  it('le tableau combine le sens et les types globaux avec les filtres de la source', () => {
    const c = creer({ source: 'CHEQUE', sens: 'ENCAISSEMENT', q: 'remise', montant: '120' });
    expect(dto(c).sens).toBe(SensOperation.ENCAISSEMENT);
    expect(dto(c).libelleOuObservation).toBe('remise');
    expect(dto(c).montant).toBe(120);
    expect(dto(c).transactionSources).toEqual([TransactionSource.CHEQUE]);
  });

  it('les cartes ne tiennent compte que des filtres globaux (sens, types)', () => {
    const c = creer({ source: 'CHEQUE', sens: 'ENCAISSEMENT', q: 'remise' });
    const cartes = (c as any).buildFilterDto();
    expect(cartes.sens).toBe(SensOperation.ENCAISSEMENT);
    expect(cartes.libelleOuObservation).toBe('');
  });

  it("l'horizon de l'adresse (fin de trimestre) part vers les cartes et le tableau ; fin de mois par défaut", () => {
    const trimestre = creer({ source: 'CHEQUE', horizon: 'FIN_DE_TRIMESTRE' });
    expect((trimestre as any).buildFilterDto().periode).toBe('FIN_DE_TRIMESTRE');
    expect(dto(trimestre).periode).toBe('FIN_DE_TRIMESTRE');
    expect(trimestre.libelleHorizon).toContain('fin du trimestre');

    TestBed.resetTestingModule();
    expect(dto(creer({ source: 'CHEQUE' })).periode).toBe('FIN_DE_MOIS');
  });

  it("« Effacer » vide le sens et les types mais garde l'horizon", () => {
    const c = creer({ horizon: 'FIN_DE_TRIMESTRE', sens: 'ENCAISSEMENT' });
    c.clearSearch();
    expect(c.searchForm.value.sens).toBeNull();
    expect(c.searchForm.value.periode).toBe('FIN_DE_TRIMESTRE');
  });

  it("la fenêtre « Ajouter » ne propose pas Importation (simple motif de commission)", () => {
    const sources = creer().ajouterItems.map(i => i.source);
    expect(sources).not.toContain(TransactionSource.Importation);
    expect(sources).toContain(TransactionSource.COMMISSION);
  });

  it('envoie le motif de la source vers le bon critère', () => {
    const impot = creer({ source: 'IMPOT_TAXE', motif: 'TVA' });
    expect(dto(impot).impotEtTaxeMotif).toBe('TVA');
    expect(dto(impot).commissionOperation).toBeUndefined();
  });

  it('ne propose N° de pièce que pour les sources qui en ont, et le motif que pour Commission / Impôt', () => {
    const cheque = creer({ source: 'CHEQUE' });
    expect(cheque.filtreNumeroDisponible).toBeTrue();
    expect(cheque.motifOptionsSource.length).toBe(0);

    TestBed.resetTestingModule();
    const commission = creer({ source: 'COMMISSION' });
    expect(commission.filtreNumeroDisponible).toBeFalse();
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

  it("écrit la source ouverte dans l'adresse, sans ajouter d'entrée d'historique, et reconnaît sa propre écriture", async () => {
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

  it("revient à la vue d'ensemble et vide les paramètres de source", async () => {
    const { harness, component, router } = await ouvrir('/b/1/recap?source=CHEQUE&page=2&q=remise');
    expect(component.openCard).toBe(TransactionSource.CHEQUE);

    component.closeSource();
    await harness.fixture.whenStable();

    expect(component.openCard).toBeNull();
    expect(router.url).not.toContain('source=');
    expect(router.url).not.toContain('q=remise');
  });

  it("relit l'état quand l'adresse change de l'extérieur (menu latéral, historique)", async () => {
    const { harness, component, router } = await ouvrir('/b/1/recap?source=CHEQUE');
    await router.navigateByUrl('/b/1/recap');
    harness.detectChanges();
    expect(component.openCard).toBeNull();
  });

  // Régression : AGIOS était éditable (bouton actif) sans route d'édition, le clic ne faisait rien.
  describe('Éditer depuis le récapitulatif', () => {
    const tables = RecapitulatifComponent as any;

    it("toute source éditable a une route d'édition, et toute route d'édition correspond à une source éditable", () => {
      const editables = Object.keys(tables.CREATE_EDIT_ELIGIBILITY);
      const routes = Object.keys(tables.EDIT_ROUTE);

      expect(editables.filter(s => !routes.includes(s))).toEqual([]);
      expect(routes.filter(s => !editables.includes(s))).toEqual([]);
    });

    it("la route d'édition d'un agios ouvre /portail/agios/edit/:id", () => {
      expect(tables.EDIT_ROUTE[TransactionSource.AGIOS](42)).toEqual(['/portail/agios/edit', 42]);
    });

    it("toute route d'édition porte l'identifiant de la ligne en dernier segment", () => {
      for (const [source, construire] of Object.entries<(id: number) => any[]>(tables.EDIT_ROUTE)) {
        const route = construire(7);
        expect(route[route.length - 1]).withContext(source).toBe(7);
        expect(String(route[0])).withContext(source).toMatch(/^\/portail\/.+\/edit$/);
      }
    });
  });
});
