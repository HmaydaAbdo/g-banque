import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { DropdownModule } from 'primeng/dropdown';
import { PasswordModule } from 'primeng/password';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { TooltipModule } from 'primeng/tooltip';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { ConfirmationService, MenuItem, MessageService } from 'primeng/api';
import { UserService } from '../../services/user.service';
import { DepartmentService } from '../../../departements/services/department.service';
import { UserResponse } from '../../dtos/UserResponse';
import { DepartmentResponse } from '../../../departements/dtos/DepartmentResponse';
import { escapeHtml } from '../../../../../shared/security/escape-html';

@Component({
  selector: 'app-utilisateur-detail',
  standalone: true,
  imports: [
    ReactiveFormsModule, RouterLink, ButtonModule, TableModule, DropdownModule,
    PasswordModule, TagModule, ToastModule, ConfirmDialogModule, TooltipModule, BreadcrumbModule
  ],
  providers: [MessageService, ConfirmationService],
  templateUrl: './utilisateur-detail.component.html'
})
export class UtilisateurDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private userService = inject(UserService);
  private departmentService = inject(DepartmentService);
  private messageService = inject(MessageService);
  private confirmationService = inject(ConfirmationService);
  private fb = inject(FormBuilder);

  items: MenuItem[] = [
    { icon: 'pi pi-home', route: '/portail/accueil', label: 'Accueil' },
    { icon: 'pi pi-shield', label: 'Administration' },
    { icon: 'pi pi-users', route: '/portail/admin/utilisateurs', label: 'Utilisateurs' },
    { icon: 'pi pi-user', label: 'Détail' }
  ];

  utilisateur: UserResponse | null = null;
  departements: DepartmentResponse[] = [];
  loading = true;

  roleOptions = [
    { label: 'Membre', value: 'MEMBER' as const },
    { label: 'Directeur', value: 'DIRECTOR' as const }
  ];

  assignForm: FormGroup = this.fb.group({
    departmentId: [null, Validators.required],
    roleInDept: ['MEMBER', Validators.required]
  });

  passwordForm: FormGroup = this.fb.group({
    password: ['', [Validators.required, Validators.minLength(8)]]
  });

  assignSubmitting = false;
  passwordSubmitting = false;

  ngOnInit(): void {
    const userId = Number(this.route.snapshot.paramMap.get('id'));
    this.load(userId);
    this.departmentService.getAll().subscribe({
      next: (data) => (this.departements = data)
    });
  }

  private load(userId: number): void {
    this.loading = true;
    this.userService.getById(userId).subscribe({
      next: (data) => {
        this.utilisateur = data;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  assignDepartmentRole(): void {
    if (!this.utilisateur || this.assignForm.invalid) {
      this.assignForm.markAllAsTouched();
      return;
    }

    this.assignSubmitting = true;
    this.userService.assignDepartmentRole(this.utilisateur.id, this.assignForm.value).subscribe({
      next: () => {
        this.assignSubmitting = false;
        this.assignForm.reset({ departmentId: null, roleInDept: 'MEMBER' });
        this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Rôle assigné.' });
        this.load(this.utilisateur!.id);
      },
      error: () => {
        this.assignSubmitting = false;
      }
    });
  }

  confirmRemoveDepartmentRole(departmentId: number, departmentName: string, event: Event): void {
    if (!this.utilisateur) return;
    this.confirmationService.confirm({
      target: event.target as EventTarget,
      message: `Retirer cet utilisateur du département "${escapeHtml(departmentName)}" ?`,
      acceptLabel: 'Oui, retirer',
      rejectLabel: 'Annuler',
      acceptButtonStyleClass: 'p-button-danger',
      rejectButtonStyleClass: 'p-button-text p-button-secondary',
      accept: () => {
        this.userService.removeDepartmentRole(this.utilisateur!.id, departmentId).subscribe({
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Rôle retiré.' });
            this.load(this.utilisateur!.id);
          }
        });
      }
    });
  }

  changePassword(): void {
    if (!this.utilisateur || this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }

    this.passwordSubmitting = true;
    this.userService.updatePassword(this.utilisateur.id, this.passwordForm.value.password).subscribe({
      next: () => {
        this.passwordSubmitting = false;
        this.passwordForm.reset();
        this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Mot de passe mis à jour.' });
      },
      error: () => {
        this.passwordSubmitting = false;
      }
    });
  }

  retour(): void {
    this.router.navigate(['/portail/admin/utilisateurs']);
  }
}
