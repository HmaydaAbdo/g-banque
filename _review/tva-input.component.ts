import {Component, forwardRef, Input, OnChanges, OnInit, SimpleChanges} from '@angular/core';
import {CommonModule} from '@angular/common';
import {
  AbstractControl,
  ControlValueAccessor,
  FormsModule,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
} from '@angular/forms';
import {DropdownModule} from 'primeng/dropdown';
import {InputNumberModule} from 'primeng/inputnumber';
import {MODE_SAISIE_MONTANT, SaisieTva, TAUX_TVA} from './saisie-tva.model';

/**
 * Champ TVA du formulaire de validation (rapprochement), partagé par tous les écrans « Encours ».
 *
 * L'utilisateur choisit un taux dans la liste, ou — quand {@link autoriserSaisieLibre} est actif —
 * l'option « Saisir le montant… », qui fait apparaître un champ montant en MAD. Le composant
 * produit une {@link SaisieTva} dont exactement l'un des deux champs est renseigné ; il ne convertit
 * jamais un taux en montant, cette conversion appartient au serveur ({@code TvaResolver}).
 *
 * La saisie libre est volontairement désactivée dans le modal de validation en lot : un montant
 * fixe appliqué à N opérations de montants différents n'a pas de sens comptable — le lot ne
 * transporte qu'un taux, converti par le serveur sur le TTC de chaque opération.
 *
 * Le message d'erreur reste à la charge du composant parent, qui suit son propre pattern
 * {@code isInvalid(...)} — utiliser {@code messageErreurTva(control.errors)} pour le libellé.
 */
@Component({
  selector: 'app-tva-input',
  standalone: true,
  imports: [CommonModule, FormsModule, DropdownModule, InputNumberModule],
  templateUrl: './tva-input.component.html',
  providers: [
    {provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => TvaInputComponent), multi: true},
    {provide: NG_VALIDATORS, useExisting: forwardRef(() => TvaInputComponent), multi: true},
  ],
})
export class TvaInputComponent implements ControlValueAccessor, Validator, OnInit, OnChanges {

  /** Libellé du champ — « TVA Déductible » en décaissement, « TVA Collectée » en encaissement. */
  @Input() label = 'TVA Déductible';

  /** Propose l'option « Saisir le montant… ». À laisser à false en validation en lot. */
  @Input() autoriserSaisieLibre = true;

  /** Montant TTC de l'opération : borne haute du montant saisissable. */
  @Input() montantMax: number | null = null;

  /** Suffixe d'identifiant HTML, pour éviter les collisions quand plusieurs champs coexistent. */
  @Input() inputId = 'tva';

  options: { label: string; value: number | string }[] = [];
  mode: number | string | null = null;
  montant: number | null = null;
  disabled = false;

  private onChange: (valeur: SaisieTva | null) => void = () => {
  };
  private onTouched: () => void = () => {
  };
  private onValidatorChange: () => void = () => {
  };

  ngOnInit(): void {
    this.construireOptions();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['autoriserSaisieLibre']) {
      this.construireOptions();
    }
    if (changes['montantMax']) {
      this.onValidatorChange();
    }
  }

  get estSaisieLibre(): boolean {
    return this.mode === MODE_SAISIE_MONTANT;
  }

  private construireOptions(): void {
    this.options = TAUX_TVA.map(taux => ({label: `${taux} %`, value: taux as number | string}));
    if (this.autoriserSaisieLibre) {
      this.options = [...this.options, {label: 'Saisir le montant…', value: MODE_SAISIE_MONTANT}];
    }
  }

  onModeChange(mode: number | string | null): void {
    this.mode = mode;
    if (mode !== MODE_SAISIE_MONTANT) {
      this.montant = null;
    }
    this.onTouched();
    this.emettre();
  }

  onMontantChange(montant: number | null): void {
    this.montant = montant;
    this.emettre();
  }

  onBlur(): void {
    this.onTouched();
  }

  // --- ControlValueAccessor ---------------------------------------------------------------

  writeValue(valeur: SaisieTva | null): void {
    if (valeur?.montantTva !== null && valeur?.montantTva !== undefined) {
      this.mode = MODE_SAISIE_MONTANT;
      this.montant = valeur.montantTva;
      return;
    }
    if (valeur?.tauxTva !== null && valeur?.tauxTva !== undefined) {
      this.mode = valeur.tauxTva;
      this.montant = null;
      return;
    }
    this.mode = null;
    this.montant = null;
  }

  registerOnChange(fn: (valeur: SaisieTva | null) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(disabled: boolean): void {
    this.disabled = disabled;
  }

  // --- Validator --------------------------------------------------------------------------

  validate(_control: AbstractControl): ValidationErrors | null {
    const valeur = this.valeurCourante();

    if (valeur === null) {
      return {tvaRequise: true};
    }
    if (valeur.tauxTva !== null) {
      return null;
    }
    if (valeur.montantTva === null) {
      return {montantTvaRequis: true};
    }
    if (valeur.montantTva < 0) {
      return {montantTvaNegatif: true};
    }
    if (this.montantMax !== null && valeur.montantTva > this.montantMax) {
      return {montantTvaSuperieurAuTotal: {max: this.montantMax}};
    }
    return null;
  }

  registerOnValidatorChange(fn: () => void): void {
    this.onValidatorChange = fn;
  }

  // --- Interne ----------------------------------------------------------------------------

  private valeurCourante(): SaisieTva | null {
    if (this.mode === MODE_SAISIE_MONTANT) {
      return {tauxTva: null, montantTva: this.montant};
    }
    if (typeof this.mode === 'number') {
      return {tauxTva: this.mode, montantTva: null};
    }
    return null;
  }

  private emettre(): void {
    this.onChange(this.valeurCourante());
    this.onValidatorChange();
  }
}
