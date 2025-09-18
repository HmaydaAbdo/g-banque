import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DcLinesListComponent } from './dc-lines-list.component';

describe('DcLinesListComponent', () => {
  let component: DcLinesListComponent;
  let fixture: ComponentFixture<DcLinesListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DcLinesListComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(DcLinesListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
