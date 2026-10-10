import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ContratsLeasingEnAttenteDeMLComponent } from './contrats-leasing-en-attente-de-ml.component';
import { testProviders } from '../../../../../../testing/test-providers';

describe('ContratsLeasingEnAttenteDeMLComponent', () => {
  let component: ContratsLeasingEnAttenteDeMLComponent;
  let fixture: ComponentFixture<ContratsLeasingEnAttenteDeMLComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: testProviders,
      imports: [ContratsLeasingEnAttenteDeMLComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(ContratsLeasingEnAttenteDeMLComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
