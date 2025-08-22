import { Component, OnInit, ViewChild, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import {ConfirmationService, MenuItem, MessageService} from "primeng/api";
import { TableLazyLoadEvent, Table } from "primeng/table";
import { debounceTime, distinctUntilChanged, Subscription } from 'rxjs';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { InputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { RippleModule } from "primeng/ripple";
import { DialogModule } from 'primeng/dialog';
import {CreditMoyenTermeResponseDto} from "../../dtos/CreditMoyenTermeResponseDto";
import {CmtService} from "../../services/CmtService";
import {BanqueService} from "../../../comptes-bancaire/services/BanqueService";
import {PageResponse} from "../../../global/types/PageResponse";
import {BanqueResponse} from "../../../comptes-bancaire/dtos/BanqueResponse";
import {CreditMoyenTermeSearchDto} from "../../dtos/CreditMoyenTermeSearchDto";
import {BreadcrumbModule} from "primeng/breadcrumb";

@Component({
  selector: 'app-cmt-list-actifs',
  templateUrl: './cmt-list.component.html',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    CardModule,
    ButtonModule,
    ToastModule,
    InputTextModule,
    DropdownModule,
    TableModule,
    TagModule,
    TooltipModule,
    ConfirmDialogModule,
    RippleModule,
    DialogModule,
    BreadcrumbModule
  ],
  providers: [MessageService, ConfirmationService]
})
export class CmtListComponent implements OnInit, OnDestroy {

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


  constructor(
    private cmtService: CmtService,
    private banqueService: BanqueService,
    private router: Router,
    private messageService: MessageService,
    private confirmationService: ConfirmationService,
    private fb: FormBuilder
  ) { }

  ngOnInit(): void {

    this.items = [
      { icon: 'pi pi-home', route: '/portail/accueil', label:"Accueil" },
      { icon: 'pi pi-list', route: '/portail/cmt', label:"Les Crédits Moyens Termes Actifs" },
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
      error: (error) => {
        console.error('Error loading bank options:', error);
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to load banks.' });
      }
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

    this.cmtService.searchActiveCredits(searchCriteria).subscribe({
      next: (page: PageResponse<CreditMoyenTermeResponseDto>) => {
        this.credits = page.content;
        this.totalRecords = page.page.totalElements;
        this.loading = false;
      },
      error: (error) => {
        this.loading = false;
        console.error('Erreur lors du chargement des crédits:', error);
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Échec du chargement des crédits.' });
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
    // Navigate to the creation form. The route should be defined in cmt.routes.ts.
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
      error: (error) => {
        console.error('Erreur lors de la suppression:', error);
        let errorMessage = 'Échec de la suppression du crédit.';
        if (error.error && error.error.message) {
          errorMessage = error.error.message;
        }
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: errorMessage });
      }
    });
  }
}
