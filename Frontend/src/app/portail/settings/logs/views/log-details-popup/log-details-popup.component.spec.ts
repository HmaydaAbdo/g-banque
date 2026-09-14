import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LogDetailsPopupComponent } from './log-details-popup.component';

describe('LogDetailsPopupComponent', () => {
  let component: LogDetailsPopupComponent;
  let fixture: ComponentFixture<LogDetailsPopupComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LogDetailsPopupComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(LogDetailsPopupComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
