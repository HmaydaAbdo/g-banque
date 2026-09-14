import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GeneralLignesByBankComponent } from './general-lignes-by-bank.component';

describe('GeneralLignesByBankComponent', () => {
  let component: GeneralLignesByBankComponent;
  let fixture: ComponentFixture<GeneralLignesByBankComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
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
