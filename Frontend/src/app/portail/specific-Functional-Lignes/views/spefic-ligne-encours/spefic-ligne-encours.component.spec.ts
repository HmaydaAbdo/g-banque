import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SpeficLigneEncoursComponent } from './spefic-ligne-encours.component';

describe('SpeficLigneEncoursComponent', () => {
  let component: SpeficLigneEncoursComponent;
  let fixture: ComponentFixture<SpeficLigneEncoursComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SpeficLigneEncoursComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(SpeficLigneEncoursComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
