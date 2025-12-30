import {Component, OnInit, ViewChild} from "@angular/core";
import {CommonModule, DecimalPipe} from "@angular/common";
import {TableLazyLoadEvent, TableModule} from "primeng/table";
import {ToastModule} from "primeng/toast";
import {ToolbarModule} from "primeng/toolbar";
import {ButtonModule} from "primeng/button";
import {RippleModule} from "primeng/ripple";
import {InputTextModule} from "primeng/inputtext";
import {FormsModule} from "@angular/forms";
import {TagModule} from "primeng/tag";
import {TooltipModule} from "primeng/tooltip";
import {BreadcrumbModule} from "primeng/breadcrumb";
import {MenuItem, MessageService} from "primeng/api";
import {SpecificFunctionalLigneService} from "../../../../specific-Functional-Lignes/service/SpecificFunctionalLigneService";
import {ActivatedRoute} from "@angular/router";
import {AgregatedSuivieFactoringLigneResponseDto} from "../dtos/AgregatedSuivieFactoringLigneResponseDto";
import {SuivieFactoringLineResponseDto} from "../dtos/SuivieFactoringLineResponseDto";

@Component({
  selector: 'app-validated-factoring',
  standalone: true,
  imports: [
    CommonModule,
    TableModule,
    ToastModule,
    ToolbarModule,
    ButtonModule,
    RippleModule,
    InputTextModule,
    FormsModule,
    TagModule,
    TooltipModule,
    BreadcrumbModule,
    DecimalPipe
  ],
  templateUrl: './validated-factoring.component.html',
  providers: [MessageService]
})
export class ValidatedFactoringComponent implements OnInit {

  @ViewChild('dt') dt: any;

  aggregatedData!: AgregatedSuivieFactoringLigneResponseDto;
  factoringLines: SuivieFactoringLineResponseDto[] = [];
  totalRecords: number = 0;
  loading: boolean = false;
  lineId!: number;
  items: MenuItem[] | undefined;

  constructor(
    private specificLigneService: SpecificFunctionalLigneService,
    private messageService: MessageService,
    private route: ActivatedRoute
  ) {
  }

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const idParam = params.get('lineId');
      if (idParam === null) {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'ID de ligne manquant dans l\'URL.',
          life: 3000
        });
        return;
      }
      this.lineId = +idParam;
      if (isNaN(this.lineId)) {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'ID de ligne invalide.',
          life: 3000
        });
        return;
      }
      this.items = [
        {icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil'},
        {icon: 'pi pi-list',route: '/portail/factoring-line', label: 'Les Encours du factoring '}
      ];
    });
  }

  loadFactoringLines(event: TableLazyLoadEvent): void {
    if (!this.lineId) {
      return;
    }
    this.loading = true;
    // Use optional chaining and nullish coalescing to handle potential undefined values
    const page = (event.first ?? 0) / (event.rows ?? 10);
    const size = event.rows ?? 10;
    const sortField = event.sortField || 'id';
    const sortOrder = event.sortOrder === 1 ? 'asc' : 'desc';
    const sort = `${sortField},${sortOrder}`;

    this.specificLigneService.getFactoringByLineId(this.lineId, page, size, sort).subscribe({
      next: (response) => {
        this.aggregatedData = response;
        this.factoringLines = response.page.content;
        // Correct the path to totalElements
        this.totalRecords = response.page.page.totalElements;
        this.loading = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des factoring:', error);
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Échec du chargement des factoring. ' + (error.error?.message || ''),
          life: 3000
        });
        this.loading = false;
      }
    });
  }





}
