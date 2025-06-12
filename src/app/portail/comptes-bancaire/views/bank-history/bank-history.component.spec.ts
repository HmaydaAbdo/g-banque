import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BankHistoryComponent } from './bank-history.component';

describe('BankHistoryComponent', () => {
  let component: BankHistoryComponent;
  let fixture: ComponentFixture<BankHistoryComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BankHistoryComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(BankHistoryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
