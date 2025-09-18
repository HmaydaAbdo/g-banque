import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DecomptesComponent } from './decomptes.component';

describe('DecomptesComponent', () => {
  let component: DecomptesComponent;
  let fixture: ComponentFixture<DecomptesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DecomptesComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(DecomptesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
