import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ImpotEtTaxeListComponent } from './impot-et-taxe-list.component';
import { testProviders } from '../../../../../testing/test-providers';

describe('ImpotEtTaxeListComponent', () => {
  let component: ImpotEtTaxeListComponent;
  let fixture: ComponentFixture<ImpotEtTaxeListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: testProviders,
      imports: [ImpotEtTaxeListComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(ImpotEtTaxeListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
