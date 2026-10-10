import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DroitConstateFormComponent } from './droit-constate-form.component';
import { testProviders } from '../../../../../testing/test-providers';

describe('DroitConstateFormComponent', () => {
  let component: DroitConstateFormComponent;
  let fixture: ComponentFixture<DroitConstateFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: testProviders,
      imports: [DroitConstateFormComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(DroitConstateFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
