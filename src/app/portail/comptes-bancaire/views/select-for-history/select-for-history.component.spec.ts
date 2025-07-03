import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SelectForHistoryComponent } from './select-for-history.component';

describe('SelectForHistoryComponent', () => {
  let component: SelectForHistoryComponent;
  let fixture: ComponentFixture<SelectForHistoryComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SelectForHistoryComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(SelectForHistoryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
