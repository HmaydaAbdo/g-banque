import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PrefinancementRemboursementsComponent } from './prefinancement-remboursements.component';

describe('PrefinancementRemboursementsComponent', () => {
  let component: PrefinancementRemboursementsComponent;
  let fixture: ComponentFixture<PrefinancementRemboursementsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PrefinancementRemboursementsComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(PrefinancementRemboursementsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
