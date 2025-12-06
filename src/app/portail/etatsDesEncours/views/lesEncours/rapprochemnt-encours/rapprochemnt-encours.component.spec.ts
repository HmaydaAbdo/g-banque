import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RapprochemntEncoursComponent } from './rapprochemnt-encours.component';

describe('RapprochemntEncoursComponent', () => {
  let component: RapprochemntEncoursComponent;
  let fixture: ComponentFixture<RapprochemntEncoursComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RapprochemntEncoursComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RapprochemntEncoursComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
