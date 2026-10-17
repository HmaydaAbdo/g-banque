import { TestBed } from '@angular/core/testing';
import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { HttpTestingController } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { of } from 'rxjs';
import { testProviders } from '../../../../../testing/test-providers';
import { PlanBanqueComponent } from './plan-banque.component';
import { planTresorerieFixture } from '../../testing/plan-tresorerie.fixture';
import { EncoursNavigationService } from '../../services/encours-navigation.service';
import { RecitBanqueComponent } from '../recit-banque/recit-banque.component';
import { TableauBanqueComponent } from '../tableau-banque/tableau-banque.component';

registerLocaleData(localeFr, 'fr-FR');

describe('PlanBanqueComponent', () => {
  let http: HttpTestingController;

  /** Deux opérations de la Banque A : un chèque reçu aujourd'hui, le leasing du 20/11 (point bas). */
  const operations = {
    totalDebit: 0, totalCredit: 0,
    page: {
      content: [
        { id: 7, dateEmission: '2026-10-14', dateEcheance: null, dateReference: '2026-10-14', libelle: 'Client Atlas', numero: 'CHQ 12',
          sens: 'ENCAISSEMENT', debit: null, credit: 1000, observations: '', source: 'CHEQUE' },
        { id: 8, dateEmission: '2026-11-01', dateEcheance: '2026-11-20', dateReference: '2026-11-20', libelle: 'Loyer engins', numero: null,
          sens: 'DECAISSEMENT', debit: 6000, credit: null, observations: '', source: 'LEASING' },
      ],
      page: { size: 2000, number: 0, totalElements: 2, totalPages: 1 },
    },
  };

  function creer(queryParams: Record<string, string> = {}) {
    const params = convertToParamMap(queryParams);
    TestBed.configureTestingModule({
      imports: [PlanBanqueComponent],
      providers: [
        ...testProviders,
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: params }, paramMap: of(convertToParamMap({ banqueId: '1' })) },
        },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    const navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    const fixture = TestBed.createComponent(PlanBanqueComponent);
    fixture.detectChanges();
    return { fixture, navigate };
  }

  function repondre(fixture: ReturnType<typeof creer>['fixture']) {
    http.expectOne(r => r.url.endsWith('tresorerie-previsionnelle')).flush(planTresorerieFixture());
    http.expectOne(r => r.url.includes('/recapitulatif/banque/1')).flush(operations);
    fixture.detectChanges();
  }

  afterEach(() => http.verify());

  it("charge le plan et les opérations de la banque à l'horizon de l'adresse, puis raconte le mois", () => {
    const { fixture } = creer({ horizon: 'FIN_DE_TRIMESTRE' });
    const plan = http.expectOne(r => r.url.endsWith('tresorerie-previsionnelle'));
    expect(plan.request.params.get('periode')).toBe('FIN_DE_TRIMESTRE');
    const recap = http.expectOne(r => r.url.includes('/recapitulatif/banque/1'));
    expect(recap.request.params.get('size')).toBe('2000');
    expect(recap.request.params.get('periode')).toBe('FIN_DE_TRIMESTRE');
    plan.flush(planTresorerieFixture());
    recap.flush(operations);
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.page__titre')!.textContent).toContain('Banque A');
    expect(el.querySelector('app-recit-banque')).not.toBeNull();
    expect(el.querySelector('app-tableau-banque')).toBeNull();
    // Une seule barre réunit filtres, horizon et export
    expect(el.querySelectorAll('.barre p-button').length).toBe(1);
    expect(el.querySelector('.resume')!.textContent).toContain('1 sortie');
  });

  it("ouvre par défaut l'étape du point bas ; un jalon l'ouvre et surligne sa cause", () => {
    const { fixture } = creer();
    repondre(fixture);
    const c = fixture.componentInstance;
    expect([...c.ouvertes]).toEqual(['P1']);

    const recit = fixture.debugElement.query(d => d.componentInstance instanceof RecitBanqueComponent).componentInstance as RecitBanqueComponent;
    expect(recit.jalons.map(j => j.code)).toContain('POINT_BAS');
    expect(recit.frise.filter(e => e.type === 'etape').length).toBe(2);

    c.toutOuvrir(false);
    fixture.detectChanges();
    expect(c.ouvertes.size).toBe(0);
    const pointBas = recit.jalons.find(j => j.code === 'POINT_BAS')!;
    recit.allerAuJalon(pointBas);
    fixture.detectChanges();
    expect([...c.ouvertes]).toEqual(['P1']);
  });

  it("les filtres s'écrivent dans l'adresse, s'affichent en pastilles et laissent les soldes intacts", () => {
    const { fixture, navigate } = creer();
    repondre(fixture);
    const c = fixture.componentInstance;

    c.appliquer({ sens: 'DECAISSEMENT', operateur: '>', montant: 5000 });
    fixture.detectChanges();
    expect(navigate.calls.mostRecent().args[1]!.queryParams).toEqual(jasmine.objectContaining({ sens: 'DECAISSEMENT', op: '>', montant: 5000 }));
    expect(c.pastilles.map(p => p.libelle)).toEqual(['Sorties', 'Montant > 5 000,00']);
    expect(c.resumeFiltre).toEqual({ nombre: 1, entrees: 0, sorties: 6000 });
    expect(c.plan!.soldesFin).toEqual(planTresorerieFixture().banques[0].soldesFin);

    c.effacer();
    expect(c.actif).toBeFalse();
  });

  it("relit l'état de l'adresse : vue tableau, sources, montant, recherche, étapes ouvertes", () => {
    const { fixture } = creer({ vue: 'tableau', sources: 'LEASING', op: '<=', montant: '7000', q: 'loyer', ouvert: 'ECHU' });
    repondre(fixture);
    const c = fixture.componentInstance;

    expect(c.vue).toBe('tableau');
    expect(c.filtres).toEqual({ sens: 'TOUS', sources: ['LEASING' as never], operateur: '<=', montant: 7000, texte: 'loyer' });
    expect([...c.ouvertes]).toEqual(['ECHU']);
    const tableau = fixture.debugElement.query(d => d.componentInstance instanceof TableauBanqueComponent).componentInstance as TableauBanqueComponent;
    expect(tableau.tableau.sorties.map(l => [l.libelle, l.source])).toEqual([['Leasing', 'LEASING']]);
    expect(tableau.tableau.entrees).toEqual([]);
  });

  it('une case du tableau détaille ses opérations ; une opération ouvre les encours filtrés sur sa source et sa période', () => {
    const { fixture } = creer({ vue: 'tableau' });
    repondre(fixture);
    const ouvrir = spyOn(TestBed.inject(EncoursNavigationService), 'ouvrirRecapitulatif');
    const tableau = fixture.debugElement.query(d => d.componentInstance instanceof TableauBanqueComponent).componentInstance as TableauBanqueComponent;
    const leasing = tableau.tableau.sorties.find(l => l.source === 'LEASING')!;

    tableau.choisir(leasing, 1);
    fixture.detectChanges();
    expect(tableau.operationsChoisies.map(o => o.libelle)).toEqual(['Loyer engins']);
    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('.detail .op')!.click();
    expect(ouvrir).toHaveBeenCalledWith(1, { horizon: null, source: 'LEASING', du: '2026-11-01', au: '2026-11-30' });
  });

  it("l'export porte sur le plan complet de la banque, sans les filtres", () => {
    const { fixture } = creer();
    repondre(fixture);
    const c = fixture.componentInstance;
    c.appliquer({ sens: 'ENCAISSEMENT' });

    c.exporterPdf();
    const req = http.expectOne(r => r.url.endsWith('/pdf'));
    expect(req.request.params.get('banqueId')).toBe('1');
    expect(req.request.params.get('periode')).toBe('FIN_DE_MOIS');
    expect(req.request.params.has('sens')).toBeFalse();
  });

  it("banque absente du plan : message d'erreur au lieu de la page", () => {
    const { fixture } = creer();
    const plan = planTresorerieFixture();
    plan.banques = [];
    http.expectOne(r => r.url.endsWith('tresorerie-previsionnelle')).flush(plan);
    http.expectOne(r => r.url.includes('/recapitulatif/banque/1')).flush(operations);
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Impossible de charger le plan');
  });
});
