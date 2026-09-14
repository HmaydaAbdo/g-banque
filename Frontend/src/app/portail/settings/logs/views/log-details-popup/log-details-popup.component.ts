import {Component, inject, OnInit} from '@angular/core';
import {LogEntryDetailResponse} from "../../dtos/LogEntryDetailResponse";
import {DynamicDialogConfig} from "primeng/dynamicdialog";
import {TableModule} from "primeng/table";
import {TooltipModule} from "primeng/tooltip";
import {LogService} from "../../services/log-service.service";

/**
 * Affiche le détail champ-par-champ (avant/après) d'une opération journalisée.
 *
 * <p>Le détail n'est pas transmis par la liste : il est reconstruit à la demande
 * côté serveur à partir de la révision Envers du log. Le popup ne reçoit donc que
 * l'identifiant du log et charge lui-même le détail à l'ouverture.
 */
@Component({
  selector: 'app-log-details-popup',
  standalone: true,
  imports: [
    TableModule,
    TooltipModule
  ],
  templateUrl: './log-details-popup.component.html',
  styleUrl: './log-details-popup.component.scss'
})
export class LogDetailsPopupComponent implements OnInit {

  logDetails: LogEntryDetailResponse[] = [];
  totalDetailRecords: number = 0;
  loadingDetails: boolean = false;
  loadFailed: boolean = false;

  first: number = 0;
  rows: number = 5;

  private config = inject(DynamicDialogConfig);
  private logService = inject(LogService);

  ngOnInit(): void {
    const logId = this.config.data?.['logId'];
    if (logId == null) {
      return;
    }
    this.loadDetails(logId);
  }

  private loadDetails(logId: number): void {
    this.loadingDetails = true;
    this.loadFailed = false;
    this.logService.getLogDetails(logId).subscribe({
      next: (details) => {
        this.logDetails = details;
        this.totalDetailRecords = details.length;
        this.first = 0;
        this.loadingDetails = false;
      },
      error: () => {
        this.logDetails = [];
        this.totalDetailRecords = 0;
        this.loadFailed = true;
        this.loadingDetails = false;
      }
    });
  }

  onPageChange(event: any) {
    this.first = event.first;
    this.rows = event.rows;
  }
}
