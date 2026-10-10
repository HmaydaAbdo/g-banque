import { ComponentFixture, TestBed } from '@angular/core/testing';

import { VirementEncaissementListComponent } from './virement-encaissement-list.component';
import { testProviders } from '../../../../../testing/test-providers';

describe('VirementEncaissementListComponent', () => {
  let component: VirementEncaissementListComponent;
  let fixture: ComponentFixture<VirementEncaissementListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: testProviders,
      imports: [VirementEncaissementListComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(VirementEncaissementListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
