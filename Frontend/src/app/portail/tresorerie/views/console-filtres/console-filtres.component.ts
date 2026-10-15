import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { SelectButtonModule } from 'primeng/selectbutton';
import { TooltipModule } from 'primeng/tooltip';
import { HORIZON_OPTIONS, HorizonPrevision } from '../../../etatsDesEncours/views/lesEncours/recap/dtos/PeriodeRecap';
import { FiltreActif } from '../../filtre-actif';

/**
 * Console de l'écran : **tous les filtres et l'export au même endroit**. L'horizon et la devise se règlent
 * ici, les filtres posés en descendant dans le détail (banque, période) y sont rappelés en pastilles, et
 * le bouton « Exporter PDF » exporte exactement ce que l'on regarde (la banque ouverte, sinon le total de
 * la devise). Présentation uniquement : l'écran porte l'état.
 */
@Component({
  selector: 'app-console-filtres',
  standalone: true,
  imports: [ReactiveFormsModule, ButtonModule, SelectButtonModule, TooltipModule],
  templateUrl: './console-filtres.component.html',
  styleUrl: './console-filtres.component.scss'
})
export class ConsoleFiltresComponent {

  readonly horizonOptions = HORIZON_OPTIONS;

  @Input({ required: true }) horizon!: FormControl<HorizonPrevision>;
  @Input({ required: true }) devise!: FormControl<string>;
  /** Devises présentes ; le sélecteur n'apparaît que s'il y en a plusieurs. */
  @Input() devises: string[] = [];
  @Input() filtres: FiltreActif[] = [];
  /** Ce que l'export va produire (« Banque A », « Total groupe »…), rappelé dans l'infobulle. */
  @Input() perimetreExport = '';
  @Input() exportEnCours = false;
  @Input() exportPossible = false;

  @Output() exporter = new EventEmitter<void>();
  @Output() effacer = new EventEmitter<void>();
}
