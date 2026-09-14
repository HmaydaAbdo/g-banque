import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LcnEncaissementsListComponent } from './lcn-encaissements-list.component';

describe('LcnEncaissementsListComponent', () => {
  let component: LcnEncaissementsListComponent;
  let fixture: ComponentFixture<LcnEncaissementsListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LcnEncaissementsListComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(LcnEncaissementsListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
