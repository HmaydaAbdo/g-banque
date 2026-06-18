import { Component, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from "primeng/confirmdialog";
import { PrimeNGConfig } from "primeng/api";

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, ToastModule, ConfirmDialogModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
  providers: []
})
export class AppComponent implements OnInit {

  title = 'Gbanque';

  constructor(private primengConfig: PrimeNGConfig) {}

  ngOnInit(): void {
    // Traduction complète des calendriers PrimeNG en français
    this.primengConfig.setTranslation({
      dayNames: ['Dimanche', 'Lundi', 'Mardi', 'Mercredi', 'Jeudi', 'Vendredi', 'Samedi'],
      dayNamesShort: ['Dim', 'Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam'],
      dayNamesMin: ['Di', 'Lu', 'Ma', 'Me', 'Je', 'Ve', 'Sa'],
      monthNames: ['Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin',
                   'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre'],
      monthNamesShort: ['Jan', 'Fév', 'Mar', 'Avr', 'Mai', 'Jun',
                        'Jul', 'Aoû', 'Sep', 'Oct', 'Nov', 'Déc'],
      today: 'Aujourd\'hui',
      clear: 'Effacer',
      weekHeader: 'Sem',
      firstDayOfWeek: 1,   // Lundi en premier
      dateFormat: 'dd/mm/yy',
      accept: 'Oui',
      reject: 'Non',
      choose: 'Choisir',
      upload: 'Télécharger',
      cancel: 'Annuler',
      fileSizeTypes: ['o', 'Ko', 'Mo', 'Go', 'To', 'Po', 'Eo', 'Zo', 'Yo'],
      aria: {
        trueLabel: 'Vrai',
        falseLabel: 'Faux',
        nullLabel: 'Non sélectionné',
        star: '1 étoile',
        stars: '{0} étoiles',
        selectAll: 'Tout sélectionner',
        unselectAll: 'Tout désélectionner',
        close: 'Fermer',
        previous: 'Précédent',
        next: 'Suivant',
        navigation: 'Navigation',
        scrollTop: 'Haut de page',
        moveTop: 'Déplacer en haut',
        moveUp: 'Déplacer vers le haut',
        moveDown: 'Déplacer vers le bas',
        moveBottom: 'Déplacer en bas',
        moveToTarget: 'Déplacer vers la cible',
        moveToSource: 'Déplacer vers la source',
        moveAllToTarget: 'Tout déplacer vers la cible',
        moveAllToSource: 'Tout déplacer vers la source',
        pageLabel: 'Page {page}',
        firstPageLabel: 'Première page',
        lastPageLabel: 'Dernière page',
        nextPageLabel: 'Page suivante',
        previousPageLabel: 'Page précédente',
        rowsPerPageLabel: 'Lignes par page',
        jumpToPageDropdownLabel: 'Aller à la page',
        jumpToPageInputLabel: 'Aller à la page',
        selectRow: 'Ligne sélectionnée',
        unselectRow: 'Ligne désélectionnée',
        expandRow: 'Ligne développée',
        collapseRow: 'Ligne réduite',
        showFilterMenu: 'Afficher le menu de filtre',
        hideFilterMenu: 'Masquer le menu de filtre',
        filterOperator: 'Opérateur de filtre',
        filterConstraint: 'Contrainte de filtre',
        editRow: 'Modifier la ligne',
        saveEdit: 'Sauvegarder',
        cancelEdit: 'Annuler',
        listView: 'Vue liste',
        gridView: 'Vue grille',
        slide: 'Diapositive',
        slideNumber: '{slideNumber}',
        zoomImage: 'Agrandir l\'image',
        zoomIn: 'Zoomer',
        zoomOut: 'Dézoomer',
        rotateRight: 'Rotation droite',
        rotateLeft: 'Rotation gauche'
      }
    });
  }
}
