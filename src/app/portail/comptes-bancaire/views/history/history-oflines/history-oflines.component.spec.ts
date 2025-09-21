import { ComponentFixture, TestBed } from '@angular/core/testing';

import { HistoryOflinesComponent } from './history-oflines.component';

describe('HistoryOflinesComponent', () => {
  let component: HistoryOflinesComponent;
  let fixture: ComponentFixture<HistoryOflinesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HistoryOflinesComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(HistoryOflinesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
