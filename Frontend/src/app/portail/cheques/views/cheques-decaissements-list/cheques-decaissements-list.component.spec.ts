import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ChequesDecaissementsListComponent } from './cheques-decaissements-list.component';
import { testProviders } from '../../../../../testing/test-providers';

describe('ChequesDecaissementsListComponent', () => {
  let component: ChequesDecaissementsListComponent;
  let fixture: ComponentFixture<ChequesDecaissementsListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: testProviders,
      imports: [ChequesDecaissementsListComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ChequesDecaissementsListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
