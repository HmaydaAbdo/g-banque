import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PrefiLinesListComponent } from './prefi-lines-list.component';

describe('PrefiLinesListComponent', () => {
  let component: PrefiLinesListComponent;
  let fixture: ComponentFixture<PrefiLinesListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PrefiLinesListComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(PrefiLinesListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
