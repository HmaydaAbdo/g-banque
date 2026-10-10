import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ValidatedDcComponent } from './validated-dc.component';
import { testProviders } from '../../../../../../../testing/test-providers';

describe('ValidatedDcComponent', () => {
  let component: ValidatedDcComponent;
  let fixture: ComponentFixture<ValidatedDcComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: testProviders,
      imports: [ValidatedDcComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(ValidatedDcComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
