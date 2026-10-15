import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { PeriodeChoix, PeriodeSelectorComponent } from './periode-selector.component';
import { PeriodeRecap } from '../../../etatsDesEncours/views/lesEncours/recap/dtos/PeriodeRecap';

const CHOIX: PeriodeChoix[] = [
  { value: 'A_CE_JOUR', label: 'À ce jour', icon: 'pi pi-calendar', hint: 'Déjà échu' },
  { value: 'FIN_DE_MOIS', label: 'Fin de mois', icon: 'pi pi-calendar-plus', hint: "Jusqu'au" },
  { value: 'FIN_DE_TRIMESTRE', label: 'Fin de trimestre', icon: 'pi pi-chart-line', hint: "Jusqu'au" },
];

@Component({
  standalone: true,
  imports: [PeriodeSelectorComponent, ReactiveFormsModule],
  template: `<app-periode-selector [formControl]="control" [choix]="choix"/>`,
})
class HoteComponent {
  control = new FormControl<PeriodeRecap | null>('FIN_DE_MOIS');
  choix = CHOIX;
}

describe('PeriodeSelectorComponent', () => {
  function creer() {
    const fixture = TestBed.createComponent(HoteComponent);
    fixture.detectChanges();
    const boutons = () => Array.from<HTMLElement>(fixture.nativeElement.querySelectorAll('[role="radio"]'));
    return { fixture, hote: fixture.componentInstance, boutons };
  }

  it('affiche trois choix et coche celui de la valeur du formulaire', () => {
    const { boutons } = creer();
    expect(boutons().length).toBe(3);
    expect(boutons().map(b => b.getAttribute('aria-checked'))).toEqual(['false', 'true', 'false']);
  });

  it('un clic choisit la période et met à jour le formulaire', () => {
    const { fixture, hote, boutons } = creer();
    boutons()[2].click();
    fixture.detectChanges();
    expect(hote.control.value).toBe('FIN_DE_TRIMESTRE');
    expect(boutons()[2].getAttribute('aria-checked')).toBe('true');
  });

  it('les flèches passent au choix suivant, en bouclant', () => {
    const { fixture, hote, boutons } = creer();
    boutons()[1].dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight' }));
    expect(hote.control.value).toBe('FIN_DE_TRIMESTRE');
    boutons()[2].dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight' }));
    fixture.detectChanges();
    expect(hote.control.value).toBe('A_CE_JOUR');
  });

  it('affiche la date de fin de chaque période et ne contient aucun texte explicatif', () => {
    const { fixture } = creer();
    expect(fixture.nativeElement.querySelectorAll('.periode__date').length).toBe(3);
    expect(fixture.nativeElement.querySelector('p')).toBeNull();
  });

  it('désactivé : un clic ne change rien', () => {
    const { fixture, hote, boutons } = creer();
    hote.control.disable();
    fixture.detectChanges();
    boutons()[0].click();
    expect(hote.control.value).toBe('FIN_DE_MOIS');
  });
});
