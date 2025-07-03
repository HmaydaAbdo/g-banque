import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EvolTauxPlafondComponent } from './evol-taux-plafond.component';

describe('EvolTauxPlafondComponent', () => {
  let component: EvolTauxPlafondComponent;
  let fixture: ComponentFixture<EvolTauxPlafondComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EvolTauxPlafondComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(EvolTauxPlafondComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
