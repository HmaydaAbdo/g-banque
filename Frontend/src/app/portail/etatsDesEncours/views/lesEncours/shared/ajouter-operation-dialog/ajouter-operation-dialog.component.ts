import { Component, ElementRef, EventEmitter, Input, Output, ViewChild } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { RippleModule } from 'primeng/ripple';
import { TransactionSource } from '../../../../../transactions/dtos/TransactionSource';

/** Une source proposée par la fenêtre ; `route` est `null` quand elle ne se crée pas depuis le Récapitulatif. */
export interface AjouterItem {
  source: TransactionSource;
  label: string;
  icon: string;
  route: any[] | null;
}

/**
 * Fenêtre « Ajouter une opération » du Récapitulatif : toutes les sources, avec un champ de recherche
 * qui ignore majuscules et accents. Une source sans route de création reste visible mais grisée
 * (« Création indisponible »). Composant de présentation : le choix est remonté au parent
 * (`choisir`), qui connaît les routes de création et navigue.
 */
@Component({
  selector: 'app-ajouter-operation-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, DialogModule, ButtonModule, RippleModule, InputTextModule],
  templateUrl: './ajouter-operation-dialog.component.html',
  styleUrl: './ajouter-operation-dialog.component.scss',
})
export class AjouterOperationDialogComponent {
  @Input() items: AjouterItem[] = [];
  @Input() visible = false;
  @Output() visibleChange = new EventEmitter<boolean>();
  @Output() choisir = new EventEmitter<AjouterItem>();

  readonly recherche = new FormControl('', { nonNullable: true });

  @ViewChild('champ') private champ?: ElementRef<HTMLInputElement>;

  /** Minuscules + sans accents : « Chèque » se trouve en tapant « cheque ». */
  private static normaliser(texte: string): string {
    return texte.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
  }

  get itemsFiltres(): AjouterItem[] {
    const terme = AjouterOperationDialogComponent.normaliser(this.recherche.value.trim());
    if (!terme) {
      return this.items;
    }
    return this.items.filter(item =>
      AjouterOperationDialogComponent.normaliser(`${item.label} ${item.source}`).includes(terme));
  }

  onShow(): void {
    this.recherche.setValue('');
    this.champ?.nativeElement.focus();
  }

  fermer(): void {
    this.visibleChange.emit(false);
  }

  choisirItem(item: AjouterItem): void {
    if (item.route) {
      this.choisir.emit(item);
    }
  }

  /** Entrée dans le champ de recherche : ouvre la première source créable de la liste filtrée. */
  onEntree(): void {
    const premiere = this.itemsFiltres.find(item => item.route);
    if (premiere) {
      this.choisirItem(premiere);
    }
  }
}
