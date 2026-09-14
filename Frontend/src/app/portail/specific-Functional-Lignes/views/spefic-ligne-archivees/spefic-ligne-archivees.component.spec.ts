import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SpeficLigneArchiveesComponent } from './spefic-ligne-archivees.component';

describe('SpeficLigneArchiveesComponent', () => {
  let component: SpeficLigneArchiveesComponent;
  let fixture: ComponentFixture<SpeficLigneArchiveesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SpeficLigneArchiveesComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(SpeficLigneArchiveesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
