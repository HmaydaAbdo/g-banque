import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterOutlet, NavigationEnd } from "@angular/router";
import { PanelMenuModule } from "primeng/panelmenu";
import {ConfirmationService, MenuItem, MessageService} from 'primeng/api';
import {CommonModule, DatePipe} from '@angular/common';
import { BankDetailsComponent } from "../../shared-componanat/bank-details/bank-details.component";
import { Ripple } from "primeng/ripple";
import { BreadcrumbModule } from "primeng/breadcrumb";
import { filter} from 'rxjs/operators';
import {Button} from "primeng/button";
import {RapprochementRequest} from "../../../../rapprochement/dtos/RapprochementRequest";
import {FormBuilder, FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators} from "@angular/forms";
import {RapprochementService} from "../../../../rapprochement/service/rapprochement-service.service";
import {CalendarModule} from "primeng/calendar";
import {ConfirmDialogModule} from "primeng/confirmdialog";
import {DialogModule} from "primeng/dialog";
import {ToastModule} from "primeng/toast";
import {RecapService} from "../../lesEncours/recap/services/recap.service";

@Component({
  selector: 'app-les-encours',
  standalone: true,
  imports: [
    RouterOutlet,
    PanelMenuModule,
    CommonModule,
    BankDetailsComponent,
    Ripple,
    BreadcrumbModule,
    Button,
    CalendarModule,
    ConfirmDialogModule,
    DialogModule,
    FormsModule,
    ToastModule,
    ReactiveFormsModule
  ],
  providers: [ConfirmationService,MessageService,DatePipe],
  templateUrl: './les-encours.component.html',
  styleUrls: ['./les-encours.component.scss']
})
export class LesEncoursComponent implements OnInit {

  items: MenuItem[] = [];
  breadcrumbItems: MenuItem[] = [];
  banqueId!: number ;


  dateClotureControl!: FormControl;
  displayClotureDialog: boolean = false;
  clotureLoading: boolean = false;
  clotureForm!: FormGroup;


  constructor(
    private route: ActivatedRoute, private rapprochementService: RapprochementService,private recapService: RecapService,
    private datePipe:DatePipe,
    private messageService:MessageService,private fb:FormBuilder,private confirmationService:ConfirmationService, private router: Router) {}

  /** Mapping entre les routes et les labels/icônes pour le fil d’Ariane **/
  private breadcrumbMap: { [key: string]: { label: string, icon: string } } = {
    // Par défaut
    'rapprochement': { label: 'Rapprochement Encours', icon: 'pi pi-sync' },

    //recap
    'recap-decaissement': { label: 'Récap Décaissements', icon: 'pi pi-briefcase' },
    'recap-encaissement': { label: 'Récap Encaissements', icon: 'pi pi-briefcase' },


    // Décaissements
    'echeances-leasing': { label: 'Décaissements ⇒ Échéances Leasing', icon: 'pi pi-list' },
    'echeances-cmt': { label: 'Décaissements ⇒ Échéances CMT', icon: 'pi pi-list' },
    'cheques-decaissements': { label: 'Décaissements ⇒ Chèques', icon: 'pi pi-list' },
    'lcn-decaissements': { label: 'Décaissements ⇒ LCN', icon: 'pi pi-list' },
    'virements-decaissements': { label: 'Décaissements ⇒ Virements', icon: 'pi pi-list' },
    'mise-a-disposition-decaissements': { label: 'Décaissements ⇒ Mise à disposition', icon: 'pi pi-list' },
    'mise-a-disposition-encaissements': { label: 'Décaissements ⇒ Mise à disposition', icon: 'pi pi-list' },
    'prefinancement-remboursement': { label: 'Décaissements ⇒ Préfinancement (Remboursement)', icon: 'pi pi-list' },
    'dc-remboursement': { label: 'Décaissements ⇒ Droits Constatés (Remboursement)', icon: 'pi pi-list' },
    'factoring-remboursement': { label:'Décaissements ⇒ Factoring (Remboursement) ',icon: 'pi pi-list'},
    'commissions': { label: 'Décaissements ⇒ Commissions', icon: 'pi pi-list' },
    'agios': { label: 'Décaissements ⇒ Agios', icon: 'pi pi-list' },
    'prelevements': { label: 'Décaissements ⇒ Prélèvements', icon: 'pi pi-list' },
    'interests': { label: 'Décaissements ⇒ Intérêts', icon: 'pi pi-list' },
    'fraisBancaire': { label: 'Décaissements ⇒ Frais Bancaires', icon: 'pi pi-list' },
    'impots-taxes': { label: 'Décaissements ⇒ Impôts et Taxes', icon: 'pi pi-list' },

    // Encaissements
    'lcn-encaissements': { label: 'Encaissements ⇒ LCN', icon: 'pi pi-list' },
    'cheques-encaissements': { label: 'Encaissements ⇒ Chèques', icon: 'pi pi-list' },
    'virements-encaissements': { label: 'Encaissements ⇒ Virements', icon: 'pi pi-list' },
    'factoring-deblocage': { label: 'Encaissements ⇒ Factoring (Déblocage)', icon: 'pi pi-list' },
    'prefinancement-deblocage': { label: 'Encaissements ⇒ Préfinancement (Déblocage)', icon: 'pi pi-list' },
    'dc-deblocage': { label: 'Encaissements ⇒ Droits Constatés (Déblocage)', icon: 'pi pi-list' },
    'decomptes': { label: 'Encaissements ⇒ Décomptes', icon: 'pi pi-list' }
  };

  ngOnInit() {
    // Initialisation du fil d’Ariane
    this.breadcrumbItems = [
      { icon: 'pi pi-home', route: '/portail/accueil', label: "Accueil" },
      { icon: 'pi pi-list', route: '/portail/etats-des-encours', label: "Suivi des encours" },
      { icon: 'pi pi-sync', route: '', label: "Rapprochement des encours" } // par défaut
    ];

    // Récupération du banqueId et mise à jour du menu + breadcrumb
    this.route.paramMap.subscribe(params => {
      this.banqueId = params.get('banqueId') as unknown as number;
      this.updateMenuItems();
      this.updateBreadcrumb();
    });

    // Mise à jour du fil d’Ariane à chaque changement de route enfant
    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe(() => this.updateBreadcrumb());

    this.initClotureForm();
  }
  /** Met à jour le fil d’Ariane dynamiquement selon la route enfant **/
  updateBreadcrumb() {
    const child = this.route.firstChild?.snapshot;
    if (child?.routeConfig?.path && this.banqueId) {
      const segment = child.routeConfig.path;
      const bc = this.breadcrumbMap[segment];
      if (bc) {
        this.breadcrumbItems[2] = {
          icon: bc.icon,
          route: `/portail/etats-des-encours/${this.banqueId}/${segment}`,
          label: bc.label
        };
      }
    }
  }

  /** Initialise les éléments du menu latéral **/
  updateMenuItems() {
    this.items = [
      {
        label: 'Rapprochement',
        icon: 'pi pi-sync',
        routerLink: ['/portail/etats-des-encours', this.banqueId, 'rapprochement']
      },

      {
        label: 'Récapitulatifs ',
        icon: 'pi pi-briefcase',
        items:[
          {
            label: 'Décaissements',
            icon: 'pi pi-minus-circle',
            routerLink: ['/portail/etats-des-encours', this.banqueId, 'recap-decaissement']
          },
          {
            label: 'Encaissements',
            icon: 'pi pi-plus-circle',
            routerLink: ['/portail/etats-des-encours', this.banqueId, 'recap-encaissement']
          },
        ]
      },


      {
        label: 'Décaissements',
        icon: 'pi pi-arrow-circle-left',
        items: [
          { label: 'Échéances leasing', icon: 'pi pi-list', routerLink: ['/portail/etats-des-encours', this.banqueId, 'echeances-leasing'] },
          { label: 'Échéances CMT', icon: 'pi pi-list', routerLink: ['/portail/etats-des-encours', this.banqueId, 'echeances-cmt'] },
          { label: 'Chèques', icon: 'pi pi-list', routerLink: ['/portail/etats-des-encours', this.banqueId, 'cheques-decaissements'] },
          { label: 'LCN', icon: 'pi pi-list', routerLink: ['/portail/etats-des-encours', this.banqueId, 'lcn-decaissements'] },
          { label: 'Virements', icon: 'pi pi-list', routerLink: ['/portail/etats-des-encours', this.banqueId, 'virements-decaissements'] },
          { label: 'Mise à disposition', icon: 'pi pi-list', routerLink: ['/portail/etats-des-encours', this.banqueId, 'mise-a-disposition-decaissements'] },
          { label: 'Préfinancement', icon: 'pi pi-list', routerLink: ['/portail/etats-des-encours', this.banqueId, 'prefinancement-remboursement'] },
          { label: 'Droits Constatés', icon: 'pi pi-list', routerLink: ['/portail/etats-des-encours', this.banqueId, 'dc-remboursement'] },
          { label: 'Factoring', icon: 'pi pi-list', routerLink: ['/portail/etats-des-encours', this.banqueId, 'factoring-remboursement'] },
          { label: 'Commissions', icon: 'pi pi-list', routerLink: ['/portail/etats-des-encours', this.banqueId, 'commissions'] },
          { label: 'Agios', icon: 'pi pi-list', routerLink: ['/portail/etats-des-encours', this.banqueId, 'agios'] },
          { label: 'Prélèvements', icon: 'pi pi-list', routerLink: ['/portail/etats-des-encours', this.banqueId, 'prelevements'] },
          { label: 'Intérêts', icon: 'pi pi-list', routerLink: ['/portail/etats-des-encours', this.banqueId, 'interests'] },
          { label: 'Frais Bancaire', icon: 'pi pi-list', routerLink: ['/portail/etats-des-encours', this.banqueId, 'fraisBancaire'] },
          { label: 'Impôts et Taxes', icon: 'pi pi-list', routerLink: ['/portail/etats-des-encours', this.banqueId, 'impots-taxes'] },

        ]
      },
      {
        label: 'Encaissements',
        icon: 'pi pi-arrow-circle-right',
        items: [
          {label: "Virements", icon: "pi pi-list", routerLink: ["/portail/etats-des-encours", this.banqueId, "virements-encaissements"]},
          { label: 'Chèques', icon: 'pi pi-list', routerLink: ['/portail/etats-des-encours', this.banqueId, 'cheques-encaissements'] },
          { label: 'LCN', icon: 'pi pi-list', routerLink: ['/portail/etats-des-encours', this.banqueId, 'lcn-encaissements'] },
          { label: 'Préfinancement', icon: 'pi pi-list', routerLink: ['/portail/etats-des-encours', this.banqueId, 'prefinancement-deblocage'] },
          { label: 'Droits Constatés', icon: 'pi pi-list', routerLink: ['/portail/etats-des-encours', this.banqueId, 'dc-deblocage'] },
          { label: 'Factoring', icon: 'pi pi-list', routerLink: ['/portail/etats-des-encours', this.banqueId, 'factoring-deblocage'] },
          { label: 'Décomptes', icon: 'pi pi-list', routerLink: ['/portail/etats-des-encours', this.banqueId, 'decomptes'] }
        ]
      }
    ];
  }

  /** Ouvre ou ferme les sous-menus **/
  toggleMenuItem(item: MenuItem): void {
    if (item.items) {
      item.expanded = !item.expanded;
    }
  }


  showClotureDialog(): void {
    this.confirmationService.confirm({
      message: "Attention : La clôture du rapprochement est une **action irréversible**. Elle doit être effectuée uniquement lorsque toutes les opérations du relevé bancaire ont été rapprochées. Confirmez-vous vouloir procéder à la clôture ?",
      header: 'Avertissement',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Oui, Clôturer',
      rejectLabel: 'Annuler',
      accept: () => {
        // Si l'utilisateur confirme (clique sur "Oui, Clôturer")
        this.clotureForm.reset({ dateCloture: new Date() });
        this.clotureForm.markAsUntouched();
        this.displayClotureDialog = true;
      },
      reject: () => {
        // Si l'utilisateur annule (clique sur "Annuler")
        this.messageService.add({ severity: 'info', summary: 'Annulé', detail: 'Opération de clôture annulée.' });
      }
    });
  }

  initClotureForm(): void {
    this.clotureForm = this.fb.group({
      dateCloture:[null, [Validators.required]]
    });
    this.dateClotureControl = this.clotureForm.get('dateCloture') as FormControl;

  }
  confirmCloture(): void {

    if (this.clotureForm.invalid || !this.banqueId) {
      this.clotureForm.markAllAsTouched();
      return;
    }

    this.clotureLoading = true;

    // Récupération de la date du formulaire
    const selectedDate: Date | null | undefined = this.clotureForm.value.dateCloture;

    if (!selectedDate) {
      this.clotureLoading = false;
      return;
    }

    // Conversion en chaîne 'YYYY-MM' (supposé par RapprochementRequest)
    const dateRapprochementString = this.datePipe.transform(selectedDate, 'yyyy-MM');

    if (!dateRapprochementString) {
      this.messageService.add({ severity: 'error', summary: 'Erreur Date', detail: 'Format de date invalide.' });
      this.clotureLoading = false;
      return;
    }


    const requestDto: RapprochementRequest = {
      banqueId: this.banqueId,
      dateRapprochement: dateRapprochementString // Utilisation de la chaîne formatée
    };

    // 2. Appel au service
    this.rapprochementService.cloturerRapprochement(requestDto)
      .subscribe({
        next: (response) => {
          this.messageService.add({
            severity: 'success',
            summary: 'Succès',
            detail: `Rapprochement clôturé pour le mois : ${response.dateRapprochement}`
          });
          this.displayClotureDialog = false;
          this.clotureLoading = false;
          window.location.reload();
        },
        error: (error) => {
          console.error('Erreur lors de la clôture:', error);
          const errorMessage = error.error?.message || "Une erreur est survenue lors de la clôture du rapprochement.";
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur de Clôture',
            detail: errorMessage
          });
          this.clotureLoading = false;
        }
      });
  }



}
