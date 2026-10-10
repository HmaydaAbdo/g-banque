import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RestitutionListComponent } from './restitution-list.component';
import { testProviders } from '../../../../../testing/test-providers';

describe('RestitutionListComponent', () => {
  let component: RestitutionListComponent;
  let fixture: ComponentFixture<RestitutionListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: testProviders,
      imports: [RestitutionListComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(RestitutionListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
