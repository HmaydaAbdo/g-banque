import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MiseADispositionDecaissementListComponent } from './mise-a-disposition-decaissement-list.component';
import { testProviders } from '../../../../../testing/test-providers';

describe('MiseADispositionDecaissementListComponent', () => {
  let component: MiseADispositionDecaissementListComponent;
  let fixture: ComponentFixture<MiseADispositionDecaissementListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: testProviders,
      imports: [MiseADispositionDecaissementListComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MiseADispositionDecaissementListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
