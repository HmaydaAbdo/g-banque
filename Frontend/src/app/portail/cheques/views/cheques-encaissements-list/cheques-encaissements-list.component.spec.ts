import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ChequesEncaissementsListComponent } from './cheques-encaissements-list.component';
import { testProviders } from '../../../../../testing/test-providers';

describe('ChequesEncaissementsListComponent', () => {
  let component: ChequesEncaissementsListComponent;
  let fixture: ComponentFixture<ChequesEncaissementsListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: testProviders,
      imports: [ChequesEncaissementsListComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(ChequesEncaissementsListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
