import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SimpleChange } from '@angular/core';
import { HttpTestingController } from '@angular/common/http/testing';
import { testProviders } from '../../../../../testing/test-providers';
import { EncoursOperationsDialogComponent } from './encours-operations-dialog.component';
import { RecapitulatifCompte } from '../../dtos/RecapitulatifCompte';
import { SensOperation } from '../../../transactions/dtos/SensOperation';
import { TransactionSource } from '../../../transactions/dtos/TransactionSource';

describe('EncoursOperationsDialogComponent', () => {
  let fixture: ComponentFixture<EncoursOperationsDialogComponent>;
  let component: EncoursOperationsDialogComponent;
  let http: HttpTestingController;

  const compte = { banqueId: 3, nom: 'BMCI', logoBase64: '', devise: 'MAD', dateHorizon: '2026-10-31' } as unknown as RecapitulatifCompte;

  const ligne = (source: TransactionSource, sens: SensOperation, date: string, montant: number) => ({
    id: 1, dateEmission: date, dateEcheance: date, libelle: 'op', numero: null, sens,
    debit: sens === SensOperation.DECAISSEMENT ? montant : 0,
    credit: sens === SensOperation.ENCAISSEMENT ? montant : 0,
    observations: '', source, compositeKey: `${source}:1`,
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EncoursOperationsDialogComponent],
      providers: testProviders,
    }).compileComponents();
    fixture = TestBed.createComponent(EncoursOperationsDialogComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  function ouvrir(sens: SensOperation): void {
    component.compte = compte;
    component.sensInitial = sens;
    component.visible = true;
    component.ngOnChanges({ visible: new SimpleChange(false, true, false) });
  }

  it("présélectionne la vue de la ligne cliquée et totalise les deux sens à partir des opérations jusqu'à l'horizon", () => {
    ouvrir(SensOperation.DECAISSEMENT);

    const requete = http.expectOne(r => r.url.endsWith('/recapitulatif/banque/3'));
    expect(requete.request.params.get('periode')).toBe('FIN_DE_MOIS');
    expect(requete.request.params.get('size')).toBe('2000');
    requete.flush({
      totalDebit: 0, totalCredit: 0,
      page: {
        content: [
          ligne(TransactionSource.DECOMPTES, SensOperation.ENCAISSEMENT, '2099-12-30', 5000),
          ligne(TransactionSource.LEASING, SensOperation.DECAISSEMENT, '2099-12-30', 300),
        ],
        page: { size: 2000, number: 0, totalElements: 2, totalPages: 1 },
      },
    });

    expect(component.vue).toBe('SORTIES');
    expect(component.totalEntrees).toBe(5000);
    expect(component.totalSorties).toBe(300);
    expect(component.totalAffiche).toBe(-300);
    expect(component.nombreAffiche).toBe(1);
    expect(component.periodesAffichees.length).toBe(1);
    expect(component.blocs(component.periodes[0]).map(b => b.sens)).toEqual(['SORTIES']);

    component.choisirVue('TOUT');
    expect(component.totalAffiche).toBe(4700);
    expect(component.blocs(component.periodes[0]).map(b => b.sens)).toEqual(['ENTREES', 'SORTIES']);
  });

  it('replie et déplie une source', () => {
    ouvrir(SensOperation.ENCAISSEMENT);
    http.expectOne(r => r.url.endsWith('/recapitulatif/banque/3')).flush({
      totalDebit: 0, totalCredit: 0,
      page: { content: [], page: { size: 2000, number: 0, totalElements: 0, totalPages: 0 } },
    });

    expect(component.vue).toBe('ENTREES');
    expect(component.periodesAffichees).toEqual([]);
    component.basculer('k');
    expect(component.estReplie('k')).toBeTrue();
    component.basculer('k');
    expect(component.estReplie('k')).toBeFalse();
  });

  it('signale une erreur de chargement', () => {
    ouvrir(SensOperation.ENCAISSEMENT);
    http.expectOne(r => r.url.endsWith('/recapitulatif/banque/3')).flush('erreur', { status: 500, statusText: 'KO' });
    expect(component.erreur).toBeTrue();
    expect(component.chargement).toBeFalse();
  });

  it('se ferme en prévenant le parent', () => {
    let valeur: boolean | undefined;
    component.visibleChange.subscribe(v => (valeur = v));
    component.fermer();
    expect(valeur).toBeFalse();
  });
});
