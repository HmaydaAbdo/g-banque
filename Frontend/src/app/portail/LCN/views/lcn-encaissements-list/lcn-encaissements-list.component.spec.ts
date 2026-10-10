import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LcnEncaissementsListComponent } from './lcn-encaissements-list.component';
import { testProviders } from '../../../../../testing/test-providers';

describe('LcnEncaissementsListComponent', () => {
  let component: LcnEncaissementsListComponent;
  let fixture: ComponentFixture<LcnEncaissementsListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: testProviders,
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
