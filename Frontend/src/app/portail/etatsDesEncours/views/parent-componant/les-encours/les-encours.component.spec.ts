import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LesEncoursComponent } from './les-encours.component';

describe('LesEncoursComponent', () => {
  let component: LesEncoursComponent;
  let fixture: ComponentFixture<LesEncoursComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LesEncoursComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(LesEncoursComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
