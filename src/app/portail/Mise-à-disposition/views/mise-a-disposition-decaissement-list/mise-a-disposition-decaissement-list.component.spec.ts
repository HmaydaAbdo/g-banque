import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MiseADispositionDecaissementListComponent } from './mise-a-disposition-decaissement-list.component';

describe('MiseADispositionDecaissementListComponent', () => {
  let component: MiseADispositionDecaissementListComponent;
  let fixture: ComponentFixture<MiseADispositionDecaissementListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
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
