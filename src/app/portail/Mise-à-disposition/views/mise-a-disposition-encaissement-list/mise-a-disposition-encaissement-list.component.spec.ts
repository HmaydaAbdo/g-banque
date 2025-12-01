import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MiseADispositionEncaissementListComponent } from './mise-a-disposition-encaissement-list.component';

describe('MiseADispositionEncaissementListComponent', () => {
  let component: MiseADispositionEncaissementListComponent;
  let fixture: ComponentFixture<MiseADispositionEncaissementListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MiseADispositionEncaissementListComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(MiseADispositionEncaissementListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
