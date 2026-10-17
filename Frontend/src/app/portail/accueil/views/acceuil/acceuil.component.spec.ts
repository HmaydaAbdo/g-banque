import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpTestingController } from '@angular/common/http/testing';

import { AcceuilComponent } from './acceuil.component';
import { testProviders } from '../../../../../testing/test-providers';

registerLocaleData(localeFr, 'fr-FR');

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

  it("affiche, par carte, une barre de FC par tuile et le statut Disponibilité / Besoin couvert par FC / Besoin dépassement", () => {
    const base = { logoBase64: '', devise: 'MAD', lcnsEchusCount: 0, virementsEchusCount: 0, periode: 'FIN_DE_MOIS', dateHorizon: '2026-10-31', soldeDebut: 0, encoursPlus: 100, encoursMoins: 50 };
    http.expectOne(r => r.url.endsWith('banques/accueil-info')).flush([
      { ...base, banqueId: 1, nom: 'A', plafondFC: 10_000_000, soldeActuel: 2_500_000, soldePrevu: -4_000_000, disponibiliteFC: 12_500_000, disponibilitePrevue: 6_000_000 },
      { ...base, banqueId: 2, nom: 'B', plafondFC: 5_000_000, soldeActuel: -3_900_000, soldePrevu: -6_350_000, disponibiliteFC: 1_100_000, disponibilitePrevue: -1_350_000 },
      { ...base, banqueId: 3, nom: 'C', plafondFC: 0, soldeActuel: 1_000, soldePrevu: 1_000, disponibiliteFC: 1_000, disponibilitePrevue: 1_000 },
    ]);
    fixture.detectChanges();

    const cartes = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('.bank-card'));
    expect(cartes.length).toBe(3);
    expect(cartes[0].querySelectorAll('.fc').length).toBe(2);
    expect(cartes[0].textContent).toContain('Disponibilité');
    expect(cartes[0].textContent).toContain('couvert par FC');
    expect(cartes[1].textContent).toContain('dépassement');
    expect(cartes[1].textContent).toContain('FC disponible');
    expect(cartes[1].querySelector('.fc__fill--depasse')).not.toBeNull();
    expect(cartes[2].querySelectorAll('.fc--aucune').length).toBe(2);
    expect(cartes[2].textContent).toContain('Aucune');
    expect(cartes[2].textContent).toContain('Sans facilité de caisse');
    expect(cartes[0].textContent).not.toContain('Plafond');
  });

  it('un clic sur « Encours sorties » ouvre le popup sur les sorties sans suivre le lien de la carte', () => {
    const compte = { banqueId: 1, nom: 'A' } as never;
    const evenement = new Event('click');
    spyOn(evenement, 'stopPropagation');
    component.ouvrirEncours(evenement, compte, 'DECAISSEMENT');

    expect(evenement.stopPropagation).toHaveBeenCalled();
    expect(component.showEncoursDialog).toBeTrue();
    expect(component.encoursSens).toBe('DECAISSEMENT' as never);
  });
});
