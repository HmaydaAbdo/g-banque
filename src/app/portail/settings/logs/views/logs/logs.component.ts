import {Component, inject, OnInit} from '@angular/core';
import { LogResponse } from '../../dtos/LogResponse';
import {LogSearchCriteria} from "../../dtos/LogSearchCriteria";
import {LogService} from "../../services/log-service.service";
import {DialogService} from "primeng/dynamicdialog";
import {TableLazyLoadEvent} from "primeng/table"; // Utilisez TableLazyLoadEvent pour une meilleure typisation
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown';
import {ToastModule} from "primeng/toast";
import {MenuItem, MessageService} from "primeng/api";
import {ToolbarModule} from "primeng/toolbar";
import {CalendarModule} from "primeng/calendar";
import {Ripple} from "primeng/ripple"; // Ripple est un directive, pas un module à importer
import {TooltipModule} from "primeng/tooltip";
import {LogDetailsPopupComponent} from "../log-details-popup/log-details-popup.component";
import {BreadcrumbModule} from "primeng/breadcrumb";


@Component({
  selector: 'app-logs',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    ButtonModule,
    InputTextModule,
    DropdownModule,
    ToastModule,
    ToolbarModule,
    CalendarModule,
    // Ripple, // Ripple est une directive, non un module. Il est utilisé directement dans le HTML.
    TooltipModule,
    BreadcrumbModule
  ],
  templateUrl: './logs.component.html',
  styleUrl: './logs.component.scss',
  providers: [DialogService, MessageService]
})
export class LogsComponent implements OnInit {

  logsService = inject(LogService);
  dialogService = inject(DialogService);
  messageService = inject(MessageService);

  logs: LogResponse[] = [];

  entityNames: string[] = [];
  usernames: string[] = [];
  operationTypes: string[] = ['UPDATE', 'DESACTIVATE', ]; // Assurez-vous d'avoir tous les types ici si non dynamique

  totalRecords: number = 0;
  loading: boolean = false;
  searchCriteria: LogSearchCriteria = {};

  // Stocke le dernier événement de lazy load pour réappliquer pagination/tri
  // Initialise avec des valeurs par défaut pour le premier chargement
  private lastLazyLoadEvent: TableLazyLoadEvent = { first: 0, rows: 10, sortField: 'timestamp', sortOrder: -1 };
  items: MenuItem[] | undefined;


  ngOnInit(): void {
    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label:"Accueil" },
      { icon: 'pi pi-list', route: '/portail/logs', label:"Les Journaux du portail"  },
    ]
    this.logsService.getEntityNames().subscribe({
      next: (data) => this.entityNames = data,
      error: (err) => {
        console.error('Erreur lors du chargement des entités:', err);
      }
    });
    this.logsService.getUsernames().subscribe({
      next: (data) => this.usernames = data,
      error: (err) => {
        console.error('Erreur lors du chargement des utilisateurs:', err);
      }
    });
  }

  /**
   * Gère l'événement LazyLoadEvent de la table PrimeNG pour la pagination, le tri et le filtrage.
   * @param event L'objet TableLazyLoadEvent contenant les paramètres de chargement.
   */
  loadLogs(event: TableLazyLoadEvent) {
    this.loading = true;

    // Assurez-vous de toujours mettre à jour l'événement le plus récent
    // Cela permet de maintenir l'état de la pagination et du tri après un changement de filtre
    this.lastLazyLoadEvent = {
      first: event.first ?? 0,
      rows: event.rows ?? 10,
      sortField: event.sortField ?? 'timestamp', // Default sort field
      sortOrder: event.sortOrder ?? -1 // Default sort order (descending)
    };


    const page = ((this.lastLazyLoadEvent.first ?? 0) / (this.lastLazyLoadEvent.rows ?? 10));
    const size = this.lastLazyLoadEvent.rows ?? 10;

    let sortOrder;
    let sortField;

    if (this.lastLazyLoadEvent.sortField && typeof this.lastLazyLoadEvent.sortField === 'string') {
      sortOrder = this.lastLazyLoadEvent.sortOrder === 1 ? 'asc' : 'desc';
      sortField = this.lastLazyLoadEvent.sortField;
    } else {
      sortOrder = 'desc';
      sortField = 'timestamp';
    }

    const sort = `${sortField},${sortOrder}`;

    // Crée un objet de critères propre pour l'appel au service
    // Ceci est crucial pour s'assurer qu'aucun critère "vide" n'est envoyé
    const criteriaToSend: LogSearchCriteria = {};
    if (this.searchCriteria.userName) {
      criteriaToSend.userName = this.searchCriteria.userName;
    }
    if (this.searchCriteria.operationType) {
      criteriaToSend.operationType = this.searchCriteria.operationType;
    }
    if (this.searchCriteria.entityName) {
      criteriaToSend.entityName = this.searchCriteria.entityName;
    }
    if (this.searchCriteria.timestampStart) {
      criteriaToSend.timestampStart = this.searchCriteria.timestampStart;
    }
    if (this.searchCriteria.timestampEnd) {
      criteriaToSend.timestampEnd = this.searchCriteria.timestampEnd;
    }


    this.logsService.getLogs(criteriaToSend, page, size, sort).subscribe({ // Utiliser criteriaToSend
      next: (data) => {
        this.logs = data.content;
        this.totalRecords = data.page.totalElements;
        this.loading = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des logs:', error);
        this.loading = false;
      }
    });
  }

  /**
   * Déclenchée lorsque les critères de recherche sont modifiés (depuis les dropdowns/inputs).
   * Recharge les logs avec les nouveaux filtres, en réinitialisant la pagination à la première page.
   */
  onFilterChange() {
    // Utilise les informations du dernier événement pour maintenir le nombre de lignes et le tri
    this.loadLogs({
      first: 0, // Toujours réinitialiser à la première page lors d'un changement de filtre
      rows: this.lastLazyLoadEvent.rows,
      sortField: this.lastLazyLoadEvent.sortField,
      sortOrder: this.lastLazyLoadEvent.sortOrder
    });
  }

  /**
   * Réinitialise tous les filtres de recherche et recharge la liste.
   */
  clearFilters() {
    // Réinitialise explicitement chaque propriété de searchCriteria à undefined
    // pour s'assurer que les champs de formulaire (dropdowns) sont aussi visuellement clairs.
    this.searchCriteria.userName = undefined;
    this.searchCriteria.operationType = undefined;
    this.searchCriteria.entityName = undefined;
    this.searchCriteria.timestampStart = undefined; // Pour p-calendar
    this.searchCriteria.timestampEnd = undefined;   // Pour p-calendar

    // PrimeNG's p-table peut parfois maintenir ses propres états de filtre interne.
    // Si la table avait des filtres directs via 'p-columnFilter', un appel à
    // table.filter() ou table.reset() serait nécessaire.
    // Mais ici, puisque nous gérons les filtres manuellement via searchCriteria,
    // un rechargement simple devrait suffire.

    // Déclenche le rechargement de la liste avec les critères maintenant vides
    this.onFilterChange();
    this.messageService.add({severity:'info', summary: 'Filtres réinitialisés', detail: 'La liste des logs a été rechargée.'});
  }

  /**
   * Ouvre un popup pour afficher les détails du log sélectionné.
   * @param log Le log dont les détails doivent être affichés.
   */
  showLogDetails(log: LogResponse) {
    this.dialogService.open(LogDetailsPopupComponent, {
      header: `Détails du Log - ID: ${log.id} (${log.userName})`,
      width: '70%',
      modal: true,
      data: {
        logDetails: log.logDetails // Passe les détails au composant du popup
      }
    });
  }

  /**
   * Gère la suppression d'un log.
   * @param log Le log à supprimer.
   */
  deleteLog(log: LogResponse) {
    if (confirm(`Êtes-vous sûr de vouloir supprimer le log ID: ${log.id} ?`)) {
      this.logsService.deleteLog(log.id).subscribe({
        next: () => {
          this.messageService.add({severity:'success', summary: 'Succès', detail: `Log ID: ${log.id} supprimé avec succès.`});
          this.onFilterChange(); // Recharge les logs après suppression
        },
        error: (error) => {
          console.error(`Erreur lors de la suppression du log ID: ${log.id}:`, error);
        }
      });
    }
  }
}
