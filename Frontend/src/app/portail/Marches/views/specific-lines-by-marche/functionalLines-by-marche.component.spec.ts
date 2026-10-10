import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FunctionalLinesByMarcheComponent } from './functionalLines-by-marche.component';
import { testProviders } from '../../../../../testing/test-providers';

describe('FunctionalLinesByMarcheComponent', () => {
  let component: FunctionalLinesByMarcheComponent;
  let fixture: ComponentFixture<FunctionalLinesByMarcheComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: testProviders,
      imports: [FunctionalLinesByMarcheComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(FunctionalLinesByMarcheComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
