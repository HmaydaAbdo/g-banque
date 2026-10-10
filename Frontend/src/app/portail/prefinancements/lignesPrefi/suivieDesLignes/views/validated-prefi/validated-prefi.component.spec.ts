import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ValidatedPrefiComponent } from './validated-prefi.component';
import { testProviders } from '../../../../../../../testing/test-providers';

describe('ValidatedPrefiComponent', () => {
  let component: ValidatedPrefiComponent;
  let fixture: ComponentFixture<ValidatedPrefiComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: testProviders,
      imports: [ValidatedPrefiComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(ValidatedPrefiComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
