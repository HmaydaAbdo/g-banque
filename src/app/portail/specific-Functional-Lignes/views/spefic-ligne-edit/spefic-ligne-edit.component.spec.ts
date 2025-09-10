import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SpeficLigneEditComponent } from './spefic-ligne-edit.component';

describe('SpeficLigneEditComponent', () => {
  let component: SpeficLigneEditComponent;
  let fixture: ComponentFixture<SpeficLigneEditComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SpeficLigneEditComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(SpeficLigneEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
