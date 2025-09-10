import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SpeficLigneExpireesComponent } from './spefic-ligne-expirees.component';

describe('SpeficLigneExpireesComponent', () => {
  let component: SpeficLigneExpireesComponent;
  let fixture: ComponentFixture<SpeficLigneExpireesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SpeficLigneExpireesComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(SpeficLigneExpireesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
