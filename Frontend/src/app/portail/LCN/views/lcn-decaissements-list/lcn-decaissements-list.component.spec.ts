import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LcnDecaissementsListComponent } from './lcn-decaissements-list.component';
import { testProviders } from '../../../../../testing/test-providers';

describe('LcnDecaissementsListComponent', () => {
  let component: LcnDecaissementsListComponent;
  let fixture: ComponentFixture<LcnDecaissementsListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: testProviders,
      imports: [LcnDecaissementsListComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(LcnDecaissementsListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
