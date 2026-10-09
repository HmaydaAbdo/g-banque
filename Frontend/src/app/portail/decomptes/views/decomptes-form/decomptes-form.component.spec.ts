import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DecompteFormComponent } from './decomptes-form.component';

describe('DecompteFormComponent', () => {
  let component: DecompteFormComponent;
  let fixture: ComponentFixture<DecompteFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DecompteFormComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(DecompteFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
