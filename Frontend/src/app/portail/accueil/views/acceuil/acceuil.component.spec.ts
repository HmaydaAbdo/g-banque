import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AcceuilComponent } from './acceuil.component';
import { testProviders } from '../../../../../testing/test-providers';

describe('AcceuilComponent', () => {
  let component: AcceuilComponent;
  let fixture: ComponentFixture<AcceuilComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: testProviders,
      imports: [AcceuilComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(AcceuilComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
