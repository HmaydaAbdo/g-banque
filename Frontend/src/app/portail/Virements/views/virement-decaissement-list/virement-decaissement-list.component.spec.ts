import { ComponentFixture, TestBed } from '@angular/core/testing';

import { VirementDecaissementListComponent } from './virement-decaissement-list.component';
import { testProviders } from '../../../../../testing/test-providers';

describe('VirementDecaissementListComponent', () => {
  let component: VirementDecaissementListComponent;
  let fixture: ComponentFixture<VirementDecaissementListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: testProviders,
      imports: [VirementDecaissementListComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(VirementDecaissementListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
