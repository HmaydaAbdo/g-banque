import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DroitConstateListComponent } from './droit-constate-list.component';
import { testProviders } from '../../../../../testing/test-providers';

describe('DroitConstateListComponent', () => {
  let component: DroitConstateListComponent;
  let fixture: ComponentFixture<DroitConstateListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: testProviders,
      imports: [DroitConstateListComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(DroitConstateListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
