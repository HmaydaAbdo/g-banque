import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LcnEncoursComponent } from './lcn-encours.component';

describe('LcnEncoursComponent', () => {
  let component: LcnEncoursComponent;
  let fixture: ComponentFixture<LcnEncoursComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LcnEncoursComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(LcnEncoursComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
