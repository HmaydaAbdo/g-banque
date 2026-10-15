import { Component, forwardRef, Input } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { finHorizon, PeriodeRecap } from '../../../etatsDesEncours/views/lesEncours/recap/dtos/PeriodeRecap';

/** Un choix de période : libellé, icône et sous-titre court. */
export interface PeriodeChoix {
  value: PeriodeRecap;
  label: string;
  icon: string;
  hint: string;
}

/**
 * Sélecteur de période en trois grandes cartes (un seul choix) : chaque carte affiche son libellé et la
 * date de fin qu'elle recouvre. Liste de boutons radio accessible (flèches, Espace), compatible
 * Reactive Forms.
 */
@Component({
  selector: 'app-periode-selector',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './periode-selector.component.html',
  styleUrl: './periode-selector.component.scss',
  providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => PeriodeSelectorComponent), multi: true }],
})
export class PeriodeSelectorComponent implements ControlValueAccessor {

  @Input() choix: PeriodeChoix[] = [];

  value: PeriodeRecap | null = null;
  disabled = false;
  readonly aujourdHui = new Date();

  private onChange: (v: PeriodeRecap) => void = () => undefined;
  private onTouched: () => void = () => undefined;

  fin(periode: PeriodeRecap): Date {
    return finHorizon(periode, this.aujourdHui);
  }

  choisir(periode: PeriodeRecap): void {
    if (this.disabled) {
      return;
    }
    this.value = periode;
    this.onChange(periode);
    this.onTouched();
  }

  /** Flèches : passe au choix suivant / précédent, comme un groupe de boutons radio natif. */
  naviguer(event: Event, decalage: number): void {
    event.preventDefault();
    const i = this.choix.findIndex(c => c.value === this.value);
    const suivant = this.choix[(i + decalage + this.choix.length) % this.choix.length];
    this.choisir(suivant.value);
    (event.currentTarget as HTMLElement).parentElement
      ?.querySelectorAll<HTMLElement>('[role="radio"]')[this.choix.indexOf(suivant)]?.focus();
  }

  writeValue(v: PeriodeRecap | null): void { this.value = v; }
  registerOnChange(fn: (v: PeriodeRecap) => void): void { this.onChange = fn; }
  registerOnTouched(fn: () => void): void { this.onTouched = fn; }
  setDisabledState(d: boolean): void { this.disabled = d; }
}
