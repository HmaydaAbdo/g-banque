import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DroitConstateFormComponent } from './droit-constate-form.component';

describe('DroitConstateFormComponent', () => {
  let component: DroitConstateFormComponent;
  let fixture: ComponentFixture<DroitConstateFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
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
