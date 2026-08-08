import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { debounceTime } from 'rxjs/operators';
import { TableModule, TableLazyLoadEvent } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { InputSwitchModule } from 'primeng/inputswitch';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { TooltipModule } from 'primeng/tooltip';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { ConfirmationService, MenuItem, MessageService } from 'primeng/api';
import { UserService } from '../../services/user.service';
import { UserResponse } from '../../dtos/UserResponse';
import { CreateUserRequest } from '../../dtos/CreateUserRequest';
import { escapeHtml } from '../../../../../shared/security/escape-html';

@Component({
  selector: 'app-utilisateurs-list',
  standalone: true,
  imports: [
    ReactiveFormsModule, TableModule, ButtonModule, RippleModule, DialogModule,
    InputTextModule, PasswordModule, InputSwitchModule, TagModule, ToastModule,
    ConfirmDialogModule, TooltipModule, BreadcrumbModule
  ],
  providers: [MessageService, ConfirmationService],
  templateUrl: './utilisateurs-list.component.html'
})
export class UtilisateursListComponent implements OnInit {
  private userService = inject(UserService);
  private messageService = inject(MessageService);
  private confirmationService = inject(ConfirmationService);
  private fb = inject(FormBuilder);
  private router = inject(Router);

  items: MenuItem[] = [
    { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
    { icon: 'pi pi-shield', label: 'Administration' },
    { icon: 'pi pi-users', route: '/portail/admin/utilisateurs', label: 'Utilisateurs' }
  ];

  utilisateurs: UserResponse[] = [];
  totalRecords = 0;
  loading = true;

  searchControl = new FormControl('');

  dialogVisible = false;
  submitting = false;

  form: FormGroup = this.fb.group({
    username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50)]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    enabled: [true]
  });

  private lastPage = 0;
  private lastSize = 10;
  private lastSort = 'id,desc';

  ngOnInit(): void {
    this.searchControl.valueChanges.pipe(debounceTime(300)).subscribe(() => {
      this.loadUtilisateurs();
    });
  }

  loadUtilisateurs(event?: TableLazyLoadEvent): void {
    this.loading = true;
    const page = event ? (event.first ?? 0) / (event.rows ?? 10) : this.lastPage;
    const size = event?.rows ?? this.lastSize;
    const sortField = event?.sortField ?? 'id';
    const sortOrder = event?.sortOrder === 1 ? 'asc' : 'desc';
    const sort = event?.sortField ? `${sortField},${sortOrder}` : this.lastSort;
    this.lastPage = page;
    this.lastSize = size;
    this.lastSort = sort;

    const username = this.searchControl.value || undefined;
    this.userService.search({ username }, page, size, sort).subscribe({
      next: (response) => {
        this.utilisateurs = response.content;
        this.totalRecords = response.page.totalElements;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  clearSearch(): void {
    this.searchControl.setValue('');
  }

  openCreateDialog(): void {
    this.form.reset({ username: '', password: '', enabled: true });
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
    const request: CreateUserRequest = this.form.value;

    this.userService.create(request).subscribe({
      next: () => {
        this.submitting = false;
        this.dialogVisible = false;
        this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Utilisateur créé.' });
        this.loadUtilisateurs();
      },
      error: () => {
        this.submitting = false;
      }
    });
  }

  toggleEnabled(utilisateur: UserResponse): void {
    this.userService.setEnabled(utilisateur.id, !utilisateur.enabled).subscribe({
      next: (updated) => {
        utilisateur.enabled = updated.enabled;
        this.messageService.add({
          severity: 'success',
          summary: 'Succès',
          detail: updated.enabled ? 'Utilisateur activé.' : 'Utilisateur désactivé.'
        });
      }
    });
  }

  toggleSystemAdmin(utilisateur: UserResponse): void {
    this.userService.setSystemAdmin(utilisateur.id, !utilisateur.systemAdmin).subscribe({
      next: (updated) => {
        utilisateur.systemAdmin = updated.systemAdmin;
        this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Statut administrateur système mis à jour.' });
      }
    });
  }

  gererUtilisateur(utilisateur: UserResponse): void {
    this.router.navigate(['/portail/admin/utilisateurs', utilisateur.id]);
  }

  confirmDelete(utilisateur: UserResponse, event: Event): void {
    this.confirmationService.confirm({
      target: event.target as EventTarget,
      message: `Supprimer l'utilisateur "${escapeHtml(utilisateur.username)}" ?`,
      acceptLabel: 'Oui, supprimer',
      rejectLabel: 'Annuler',
      acceptButtonStyleClass: 'p-button-danger',
      rejectButtonStyleClass: 'p-button-text p-button-secondary',
      accept: () => this.delete(utilisateur.id)
    });
  }

  private delete(userId: number): void {
    this.userService.delete(userId).subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Utilisateur supprimé.' });
        this.loadUtilisateurs();
      }
    });
  }
}
