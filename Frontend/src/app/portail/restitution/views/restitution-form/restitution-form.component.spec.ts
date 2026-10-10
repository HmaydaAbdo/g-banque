import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RestitutionFormComponent } from './restitution-form.component';
import { testProviders } from '../../../../../testing/test-providers';

describe('RestitutionFormComponent', () => {
  let component: RestitutionFormComponent;
  let fixture: ComponentFixture<RestitutionFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: testProviders,
      imports: [RestitutionFormComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(RestitutionFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
