import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ImpotEtTaxeFormComponent } from './impot-et-taxe-form.component';

describe('ImpotEtTaxeFormComponent', () => {
  let component: ImpotEtTaxeFormComponent;
  let fixture: ComponentFixture<ImpotEtTaxeFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ImpotEtTaxeFormComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(ImpotEtTaxeFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
