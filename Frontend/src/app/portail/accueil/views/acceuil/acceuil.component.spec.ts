import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpTestingController } from '@angular/common/http/testing';

import { AcceuilComponent } from './acceuil.component';
import { testProviders } from '../../../../../testing/test-providers';

describe('AcceuilComponent', () => {
  let component: AcceuilComponent;
  let fixture: ComponentFixture<AcceuilComponent>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: testProviders,
      imports: [AcceuilComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AcceuilComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('projette les cartes à la fin du mois par défaut, puis à la fin du trimestre sur demande', () => {
    const carte = (r: { url: string }) => r.url.endsWith('banques/accueil-info');
    expect(http.expectOne(carte).request.params.get('periode')).toBe('FIN_DE_MOIS');

    component.horizonControl.setValue('FIN_DE_TRIMESTRE');

    expect(http.expectOne(carte).request.params.get('periode')).toBe('FIN_DE_TRIMESTRE');
    expect(component.titrePrevision).toBe('Fin de trimestre');
  });

  it("le Récap global propose la fin de trimestre et présélectionne l'horizon des cartes", () => {
    component.horizonControl.setValue('FIN_DE_TRIMESTRE');
    component.openRecapGlobalPeriodeModal();

    expect(component.periodeChoix.map(o => o.value)).toEqual(['A_CE_JOUR', 'FIN_DE_MOIS', 'FIN_DE_TRIMESTRE']);
    expect(component.periodeControl.value).toBe('FIN_DE_TRIMESTRE');
  });
});
