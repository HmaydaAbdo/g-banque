import {Component, OnInit} from '@angular/core';
import {LogEntryDetailResponse} from "../../dtos/LogEntryDetailResponse";
import {DynamicDialogConfig} from "primeng/dynamicdialog";
import {TableModule} from "primeng/table";
import {TooltipModule} from "primeng/tooltip";

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

  first: number = 0;
  rows: number = 5;

  constructor(public config: DynamicDialogConfig) { }

  ngOnInit(): void {
    if (this.config.data && this.config.data['logDetails']) {
      this.logDetails = this.config.data['logDetails'];
      this.totalDetailRecords = this.logDetails.length;
    }
  }

  onPageChange(event: any) {
    this.first = event.first;
    this.rows = event.rows;
  }
}
