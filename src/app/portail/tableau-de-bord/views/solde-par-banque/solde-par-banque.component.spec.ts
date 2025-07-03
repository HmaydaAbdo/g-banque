import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SoldeParBanqueComponent } from './solde-par-banque.component';

describe('SoldeParBanqueComponent', () => {
  let component: SoldeParBanqueComponent;
  let fixture: ComponentFixture<SoldeParBanqueComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SoldeParBanqueComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(SoldeParBanqueComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
