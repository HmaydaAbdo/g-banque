import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DroitConstateRemboursementsComponent } from './droit-constate-remboursements.component';

describe('DroitConstateRemboursementsComponent', () => {
  let component: DroitConstateRemboursementsComponent;
  let fixture: ComponentFixture<DroitConstateRemboursementsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DroitConstateRemboursementsComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(DroitConstateRemboursementsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
