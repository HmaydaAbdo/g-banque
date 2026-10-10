import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ContratsLeasingArchivesComponent } from './contrats-leasing-archives.component';
import { testProviders } from '../../../../../../testing/test-providers';

describe('ContratsLeasingArchivesComponent', () => {
  let component: ContratsLeasingArchivesComponent;
  let fixture: ComponentFixture<ContratsLeasingArchivesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: testProviders,
      imports: [ContratsLeasingArchivesComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(ContratsLeasingArchivesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
