import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DecomptesFormComponent } from './decomptes-form.component';

describe('DecomptesFormComponent', () => {
  let component: DecomptesFormComponent;
  let fixture: ComponentFixture<DecomptesFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DecomptesFormComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(DecomptesFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
