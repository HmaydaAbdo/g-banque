import {Component, inject, OnInit} from '@angular/core';
import {BanqueResponse} from "../../dtos/BanqueResponse";
import {BanqueService} from "../../services/BanqueService";
import {Router} from "@angular/router";
import {TableModule} from "primeng/table";
import {DialogModule} from "primeng/dialog";

@Component({
  selector: 'app-select-for-history',
  standalone: true,
  imports: [

    TableModule,
    DialogModule
  ],
  templateUrl: './select-for-history.component.html',
  styleUrl: './select-for-history.component.scss'
})
export class SelectForHistoryComponent implements OnInit {
  displayDialog: boolean = true;
  banks: BanqueResponse[] = [];
  selectedBank: BanqueResponse | null = null;

  private banqueService = inject(BanqueService);
  private router = inject(Router);

  ngOnInit(): void {
    this.loadBanks();
  }

  loadBanks(): void {

    this.banqueService.getAllBanques({}, 0, 1000).subscribe({
      next: (pageResponse) => {
        this.banks = pageResponse.content;
      },
      error: (err) => {
        console.error('Error loading banks:', err);
      }
    });
  }

  selectBank(bank: BanqueResponse): void {
    this.selectedBank = bank;
    this.navigateToHistory();
  }

  navigateToHistory(): void {
    if (this.selectedBank && this.selectedBank.id) {
      this.displayDialog = false; // Close the dialog
      this.router.navigate(['/portail/comptes-bancaires/history', this.selectedBank.id]);
    }
  }

  onDialogHide(): void {
    //this.router.navigate(['/portail/accueil']);
  }
}
