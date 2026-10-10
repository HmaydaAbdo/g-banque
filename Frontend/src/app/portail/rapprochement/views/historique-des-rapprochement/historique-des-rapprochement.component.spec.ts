import { ComponentFixture, TestBed } from '@angular/core/testing';

import { HistoriqueDesRapprochementComponent } from './historique-des-rapprochement.component';
import { testProviders } from '../../../../../testing/test-providers';

describe('HistoriqueDesRapprochementComponent', () => {
  let component: HistoriqueDesRapprochementComponent;
  let fixture: ComponentFixture<HistoriqueDesRapprochementComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: testProviders,
      imports: [HistoriqueDesRapprochementComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(HistoriqueDesRapprochementComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
