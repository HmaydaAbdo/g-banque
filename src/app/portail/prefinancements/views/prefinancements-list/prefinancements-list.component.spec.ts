import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PrefinancementsListComponent } from './prefinancements-list.component';

describe('PrefinancementsListComponent', () => {
  let component: PrefinancementsListComponent;
  let fixture: ComponentFixture<PrefinancementsListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PrefinancementsListComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(PrefinancementsListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
