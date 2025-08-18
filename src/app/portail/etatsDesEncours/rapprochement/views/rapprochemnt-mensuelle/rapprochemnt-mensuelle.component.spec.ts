import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RapprochemntMensuelleComponent } from './rapprochemnt-mensuelle.component';

describe('RapprochemntMensuelleComponent', () => {
  let component: RapprochemntMensuelleComponent;
  let fixture: ComponentFixture<RapprochemntMensuelleComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RapprochemntMensuelleComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(RapprochemntMensuelleComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
