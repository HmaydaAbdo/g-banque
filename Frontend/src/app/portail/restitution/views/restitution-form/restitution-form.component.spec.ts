import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RestitutionFormComponent } from './restitution-form.component';

describe('RestitutionFormComponent', () => {
  let component: RestitutionFormComponent;
  let fixture: ComponentFixture<RestitutionFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
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
