import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GeneralLignesByBankComponent } from './general-lignes-by-bank.component';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of } from 'rxjs';
import { testProviders } from '../../../../../../testing/test-providers';

describe('GeneralLignesByBankComponent', () => {
  let component: GeneralLignesByBankComponent;
  let fixture: ComponentFixture<GeneralLignesByBankComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [
        ...testProviders,
        // Composant de l'écran d'historique d'une banque : l'id de la banque est lu dans la route (paramètre 'id')
        { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ id: '1' })) } },
      ],
      imports: [GeneralLignesByBankComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(GeneralLignesByBankComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
