import { Component, ViewChild} from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from "../../shared-components/navbar/navbar.component";
import { SidebarComponent } from "../../shared-components/sidebar/sidebar.component";
import { ConfirmDialogModule } from "primeng/confirmdialog";
import { ConfirmationService } from "primeng/api";


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

export class PortailComponent   {
  sidebarVisible: boolean = false;
  @ViewChild(SidebarComponent) sidebar!: SidebarComponent;

}
