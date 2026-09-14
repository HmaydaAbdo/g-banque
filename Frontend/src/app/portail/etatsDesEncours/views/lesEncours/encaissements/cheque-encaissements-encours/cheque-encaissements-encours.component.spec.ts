import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ChequeEncaissementsEncoursComponent } from './cheque-encaissements-encours.component';

describe('ChequeEncaissementsEncoursComponent', () => {
  let component: ChequeEncaissementsEncoursComponent;
  let fixture: ComponentFixture<ChequeEncaissementsEncoursComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ChequeEncaissementsEncoursComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ChequeEncaissementsEncoursComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
