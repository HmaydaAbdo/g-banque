import { Component, ViewChild } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from "../../shared-components/navbar/navbar.component";
import { SidebarComponent } from "../../shared-components/sidebar/sidebar.component";
import { ConfirmDialogModule } from "primeng/confirmdialog";
import { ConfirmationService } from "primeng/api";
import { ThemeService } from "../../services/ThemeService";


@Component({
  selector: 'app-portail',
  standalone: true,
  imports: [
    RouterOutlet,
    NavbarComponent,
    SidebarComponent,
    ConfirmDialogModule,
  ],
  templateUrl: './portail.component.html',
  styleUrl: './portail.component.scss',
  providers: [ConfirmationService]
})
export class PortailComponent {
  sidebarVisible: boolean = false;
  @ViewChild(SidebarComponent) sidebar!: SidebarComponent;

  // Injecter ThemeService ici garantit son initialisation au démarrage du portail
  // (applique data-theme sur <html> avant le premier rendu)
  constructor(private _themeService: ThemeService) {}
}
