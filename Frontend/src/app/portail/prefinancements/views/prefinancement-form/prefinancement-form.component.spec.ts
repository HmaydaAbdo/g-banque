import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PrefinancementFormComponent } from './prefinancement-form.component';
import { testProviders } from '../../../../../testing/test-providers';

describe('PrefinancementFormComponent', () => {
  let component: PrefinancementFormComponent;
  let fixture: ComponentFixture<PrefinancementFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: testProviders,
      imports: [PrefinancementFormComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(PrefinancementFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
