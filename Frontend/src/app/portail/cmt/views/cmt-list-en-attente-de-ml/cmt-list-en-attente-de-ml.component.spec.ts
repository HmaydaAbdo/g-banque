import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CmtListEnAttenteDeMLComponent } from './cmt-list-en-attente-de-ml.component';
import { testProviders } from '../../../../../testing/test-providers';

describe('CmtListEnAttenteDeMLComponent', () => {
  let component: CmtListEnAttenteDeMLComponent;
  let fixture: ComponentFixture<CmtListEnAttenteDeMLComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: testProviders,
      imports: [CmtListEnAttenteDeMLComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(CmtListEnAttenteDeMLComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
