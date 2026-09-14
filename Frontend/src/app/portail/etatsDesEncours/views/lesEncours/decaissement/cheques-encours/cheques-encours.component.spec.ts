import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ChequesEncoursComponent } from './cheques-encours.component';

describe('ChequesEncoursComponent', () => {
  let component: ChequesEncoursComponent;
  let fixture: ComponentFixture<ChequesEncoursComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ChequesEncoursComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(ChequesEncoursComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
