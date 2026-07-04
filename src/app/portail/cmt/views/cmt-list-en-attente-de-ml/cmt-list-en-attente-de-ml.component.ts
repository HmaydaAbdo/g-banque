import {Component, OnDestroy, OnInit, ViewChild, inject} from '@angular/core';
import {CreditMoyenTermeResponseDto} from "../../dtos/CreditMoyenTermeResponseDto";
import {FormBuilder, FormGroup, ReactiveFormsModule} from "@angular/forms";
import {Table, TableLazyLoadEvent, TableModule} from "primeng/table";
import {debounceTime, distinctUntilChanged, Subscription} from "rxjs";
import {ConfirmationService, MenuItem, MessageService, PrimeTemplate} from "primeng/api";
import {CmtService} from "../../services/CmtService";
import {BanqueService} from "../../../comptes-bancaire/services/BanqueService";
import {Router, RouterLinkActive} from "@angular/router";
import {PageResponse} from "../../../global/types/PageResponse";
import {BanqueResponse} from "../../../comptes-bancaire/dtos/BanqueResponse";
import {CreditMoyenTermeSearchDto} from "../../dtos/CreditMoyenTermeSearchDto";
import {BreadcrumbModule} from "primeng/breadcrumb";
import {Button, ButtonDirective} from "primeng/button";
import {ConfirmDialogModule} from "primeng/confirmdialog";
import {DatePipe, DecimalPipe, NgClass} from "@angular/common";
import {DropdownModule} from "primeng/dropdown";
import {InputTextModule} from "primeng/inputtext";
import {Ripple} from "primeng/ripple";
import {TagModule} from "primeng/tag";
import {ToastModule} from "primeng/toast";
import {TooltipModule} from "primeng/tooltip";

@Component({
  selector: 'app-cmt-list-en-attente-de-ml',
  standalone: true,
  imports: [
    BreadcrumbModule,
    Button,
    ButtonDirective,
    ConfirmDialogModule,
    DatePipe,
    DropdownModule,
    InputTextModule,
    PrimeTemplate,
    ReactiveFormsModule,
    Ripple,
    RouterLinkActive,
    TableModule,
    TagModule,
    ToastModule,
    TooltipModule,
    NgClass,
    DecimalPipe
  ],
  templateUrl: './cmt-list-en-attente-de-ml.component.html',
  styleUrl: './cmt-list-en-attente-de-ml.component.scss'
})
export class CmtListEnAttenteDeMLComponent  implements OnInit, OnDestroy {

  credits: CreditMoyenTermeResponseDto[] = [];
  totalRecords: number = 0;
  loading: boolean = false;

  searchForm!: FormGroup;

  banqueOptions: { label: string, value: number }[] = [];

  @ViewChild('dt') dt: Table | undefined;

  private subscriptions: Subscription[] = [];
  private lastLazyLoadEvent: TableLazyLoadEvent = {
    first: 0,
    rows: 10,
    sortField: 'id',
    sortOrder: 1,
    filters: {}
  };
  items: MenuItem[] | undefined;


  private readonly cmtService = inject(CmtService);
  private readonly banqueService = inject(BanqueService);
  private readonly router = inject(Router);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly fb = inject(FormBuilder);

  ngOnInit(): void {

    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label:"Accueil" },
      { icon: 'pi pi-list', route: '/portail/cmt/en-attente-de-ML', label:"Les Crédits Moyens Termes en attente de main levée" },
    ]
    this.initSearchForm();
    this.loadDropdownOptions();

    const designationSub = this.searchForm.get('designation')?.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe(() => this.onSearch());

    const banqueSub = this.searchForm.get('banqueId')?.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe(() => this.onSearch());



    this.subscriptions.push(designationSub!, banqueSub!);
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  initSearchForm(): void {
    this.searchForm = this.fb.group({
      designation: [''],
      banqueId: [null],
    });
  }

  loadDropdownOptions(): void {
    this.banqueService.getAllBanques({}, 0, 1000).subscribe({
      next: (response: PageResponse<BanqueResponse>) => {
        this.banqueOptions = response.content.map(banque => ({
          label: banque.nom,
          value: banque.id
        }));
      },
      error: () => {}
    });
  }

  loadCredits(event: TableLazyLoadEvent): void {
    this.loading = true;
    this.lastLazyLoadEvent = event;

    const currentPage = event.first! / event.rows!;
    const pageSize = event.rows!;

    let sort: string = 'id,asc';
    if (event.sortField && event.sortOrder) {
      const actualSortField = Array.isArray(event.sortField) ? event.sortField[0] : event.sortField;
      sort = `${actualSortField},${event.sortOrder === 1 ? 'asc' : 'desc'}`;
    }

    const searchCriteria: CreditMoyenTermeSearchDto = {
      designation: this.searchForm.get('designation')?.value,
      banqueId:this.searchForm.get('banqueId')?.value,
      page: currentPage,
      size: pageSize,
      sort: sort
    };

    this.cmtService.searchPendingClosureCredits(searchCriteria).subscribe({
      next: (page: PageResponse<CreditMoyenTermeResponseDto>) => {
        this.credits = page.content;
        this.totalRecords = page.page.totalElements;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  onSearch(): void {
    if (this.dt) {
      this.dt.first = 0;
      this.loadCredits(this.dt.createLazyLoadMetadata());
    } else {
      this.loadCredits({
        ...this.lastLazyLoadEvent,
        first: 0
      });
    }
  }

  clearSearch(): void {
    this.searchForm.reset({
      designation: '',
      banqueId: null,
    });
    if (this.dt) {
      this.dt.clear();
      this.dt.first = 0;
      this.loadCredits(this.dt.createLazyLoadMetadata());
    } else {
      this.loadCredits(this.lastLazyLoadEvent);
    }
  }

  createNewCredit(): void {
    this.router.navigate(['portail/cmt/new']);
  }

  viewAmortizationTable(creditId: number): void {
    this.router.navigate(['portail/cmt', creditId, 'tableau-amortissement']);
  }

  editCredit(id: number): void {
    this.router.navigate(['portail/cmt/edit', id]);
  }

  confirmDeleteCredit(credit: CreditMoyenTermeResponseDto): void {
    this.confirmationService.confirm({
      message: `Êtes-vous sûr de vouloir supprimer le crédit : ${credit.designation} ?`,
      header: 'Confirmation de suppression',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Oui',
      rejectLabel: 'Non',
      acceptButtonStyleClass: 'p-button-danger',
      rejectButtonStyleClass: 'p-button-text',
      accept: () => {
        this.deleteCredit(credit.id);
      }
    });
  }

  deleteCredit(id: number): void {
    this.cmtService.deleteCredit(id).subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Crédit supprimé avec succès.' });
        if (this.dt) {
          this.loadCredits(this.dt.createLazyLoadMetadata());
        } else {
          this.loadCredits(this.lastLazyLoadEvent);
        }
      },
      error: () => {}
    });
  }

  /**
   * Confirms the archiving of a credit with a dialog.
   * @param credit The credit to be archived.
   */
  confirmArchiveCredit(credit: CreditMoyenTermeResponseDto): void {
    this.confirmationService.confirm({
      message: `Êtes-vous sûr de vouloir archiver le crédit : ${credit.designation} ?`,
      header: 'Confirmation d\'archivage',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Oui',
      rejectLabel: 'Non',
      acceptButtonStyleClass: 'p-button-success',
      rejectButtonStyleClass: 'p-button-text',
      accept: () => {
        this.archiveCredit(credit.id);
      }
    });
  }

  /**
   * Archives a credit by calling the service.
   * @param id The ID of the credit to archive.
   */
  archiveCredit(id: number): void {
    this.cmtService.archiveCredit(id).subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Crédit archivé avec succès.' });
        if (this.dt) {
          this.loadCredits(this.dt.createLazyLoadMetadata());
        } else {
          this.loadCredits(this.lastLazyLoadEvent);
        }
      },
      error: () => {}
    });
  }
}
