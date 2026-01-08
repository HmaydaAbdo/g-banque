import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EncoursImpotsTaxesComponent } from './encours-impots-taxes.component';

describe('EncoursImpotsTaxesComponent', () => {
  let component: EncoursImpotsTaxesComponent;
  let fixture: ComponentFixture<EncoursImpotsTaxesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EncoursImpotsTaxesComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(EncoursImpotsTaxesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
