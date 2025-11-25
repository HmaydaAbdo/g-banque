import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PrelevementFormComponent } from './prelevement-form.component';

describe('PrelevementFormComponent', () => {
  let component: PrelevementFormComponent;
  let fixture: ComponentFixture<PrelevementFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
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
