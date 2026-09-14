import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EcheancesLeasingComponent } from './echeances-leasing.component';

describe('EcheancesLeasingComponent', () => {
  let component: EcheancesLeasingComponent;
  let fixture: ComponentFixture<EcheancesLeasingComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EcheancesLeasingComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(EcheancesLeasingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
