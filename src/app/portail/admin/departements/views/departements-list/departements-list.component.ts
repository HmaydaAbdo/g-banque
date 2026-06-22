import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { TooltipModule } from 'primeng/tooltip';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { ConfirmationService, MenuItem, MessageService } from 'primeng/api';
import { DepartmentService } from '../../services/department.service';
import { DepartmentResponse } from '../../dtos/DepartmentResponse';
import { DepartmentRequest } from '../../dtos/DepartmentRequest';

@Component({
  selector: 'app-departements-list',
  standalone: true,
  imports: [
    ReactiveFormsModule, TableModule, ButtonModule, RippleModule,
    DialogModule, InputTextModule, InputTextareaModule, ToastModule,
    ConfirmDialogModule, TooltipModule, BreadcrumbModule
  ],
  providers: [MessageService, ConfirmationService],
  templateUrl: './departements-list.component.html'
})
export class DepartementsListComponent implements OnInit {
  private departmentService = inject(DepartmentService);
  private messageService = inject(MessageService);
  private confirmationService = inject(ConfirmationService);
  private fb = inject(FormBuilder);

  items: MenuItem[] = [
    { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
    { icon: 'pi pi-shield', label: 'Administration' },
    { icon: 'pi pi-sitemap', route: '/portail/admin/departements', label: 'Départements' }
  ];

  departements: DepartmentResponse[] = [];
  loading = true;

  searchControl = new FormControl('');

  get filteredDepartements(): DepartmentResponse[] {
    const term = (this.searchControl.value || '').trim().toLowerCase();
    if (!term) return this.departements;
    return this.departements.filter(d =>
      d.name.toLowerCase().includes(term) || (d.description ?? '').toLowerCase().includes(term)
    );
  }

  clearSearch(): void {
    this.searchControl.setValue('');
  }

  dialogVisible = false;
  editingId: number | null = null;
  submitting = false;

  form: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
    description: ['', [Validators.maxLength(255)]]
  });

  ngOnInit(): void {
    this.loadDepartements();
  }

  loadDepartements(): void {
    this.loading = true;
    this.departmentService.getAll().subscribe({
      next: (data) => {
        this.departements = data;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  openCreateDialog(): void {
    this.editingId = null;
    this.form.reset();
    this.dialogVisible = true;
  }

  openEditDialog(departement: DepartmentResponse): void {
    this.editingId = departement.id;
    this.form.reset({ name: departement.name, description: departement.description ?? '' });
    this.dialogVisible = true;
  }

  closeDialog(): void {
    this.dialogVisible = false;
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting = true;
    const request: DepartmentRequest = this.form.value;

    const call = this.editingId
      ? this.departmentService.update(this.editingId, request)
      : this.departmentService.create(request);

    call.subscribe({
      next: () => {
        this.submitting = false;
        this.dialogVisible = false;
        this.messageService.add({
          severity: 'success',
          summary: 'Succès',
          detail: this.editingId ? 'Département mis à jour.' : 'Département créé.'
        });
        this.loadDepartements();
      },
      error: () => {
        this.submitting = false;
      }
    });
  }

  confirmDelete(departement: DepartmentResponse, event: Event): void {
    this.confirmationService.confirm({
      target: event.target as EventTarget,
      message: `Supprimer le département "${departement.name}" ?`,
      acceptLabel: 'Oui, supprimer',
      rejectLabel: 'Annuler',
      acceptButtonStyleClass: 'p-button-danger',
      rejectButtonStyleClass: 'p-button-text p-button-secondary',
      accept: () => this.delete(departement.id)
    });
  }

  private delete(id: number): void {
    this.departmentService.delete(id).subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Département supprimé.' });
        this.loadDepartements();
      }
    });
  }
}
