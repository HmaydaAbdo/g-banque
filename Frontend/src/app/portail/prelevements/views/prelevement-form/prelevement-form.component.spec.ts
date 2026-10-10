import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PrelevementFormComponent } from './prelevement-form.component';
import { testProviders } from '../../../../../testing/test-providers';

describe('PrelevementFormComponent', () => {
  let component: PrelevementFormComponent;
  let fixture: ComponentFixture<PrelevementFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: testProviders,
      imports: [PrelevementFormComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(PrelevementFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
