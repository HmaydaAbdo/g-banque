import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PrelevementListComponent } from './prelevement-list.component';

describe('PrelevementListComponent', () => {
  let component: PrelevementListComponent;
  let fixture: ComponentFixture<PrelevementListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PrelevementListComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(PrelevementListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
