import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PrefinancementsListComponent } from './prefinancements-list.component';
import { testProviders } from '../../../../../testing/test-providers';

describe('PrefinancementsListComponent', () => {
  let component: PrefinancementsListComponent;
  let fixture: ComponentFixture<PrefinancementsListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: testProviders,
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
