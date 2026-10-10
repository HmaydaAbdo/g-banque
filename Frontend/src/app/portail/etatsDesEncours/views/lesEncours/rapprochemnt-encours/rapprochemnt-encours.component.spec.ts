import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RapprochemntEncoursComponent } from './rapprochemnt-encours.component';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of } from 'rxjs';
import { testProviders } from '../../../../../../testing/test-providers';

describe('RapprochemntEncoursComponent', () => {
  let component: RapprochemntEncoursComponent;
  let fixture: ComponentFixture<RapprochemntEncoursComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [
        ...testProviders,
        // Écran enfant de /etats-des-encours/:banqueId : la banque est lue sur la route parente
        { provide: ActivatedRoute, useValue: { parent: { paramMap: of(convertToParamMap({ banqueId: '1' })) } } },
      ],
      imports: [RapprochemntEncoursComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RapprochemntEncoursComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
