import { ComponentFixture, TestBed } from '@angular/core/testing';

import { VirementFormComponent } from './virement-form.component';
import { testProviders } from '../../../../../testing/test-providers';

describe('VirementFormComponent', () => {
  let component: VirementFormComponent;
  let fixture: ComponentFixture<VirementFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: testProviders,
      imports: [VirementFormComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(VirementFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
