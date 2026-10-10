import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RecapitulatifComponent } from './recapitulatif.component';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of } from 'rxjs';
import { testProviders } from '../../../../../../../../testing/test-providers';

describe('RecapitulatifComponent', () => {
  let component: RecapitulatifComponent;
  let fixture: ComponentFixture<RecapitulatifComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [
        ...testProviders,
        // Écran enfant de /etats-des-encours/:banqueId : la banque est lue sur la route parente
        { provide: ActivatedRoute, useValue: { parent: { paramMap: of(convertToParamMap({ banqueId: '1' })) } } },
      ],
      imports: [RecapitulatifComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(RecapitulatifComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
