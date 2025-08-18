import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MiseADispositionActifListComponent } from './mise-a-disposition-actif-list.component';

describe('MiseADispositionActifListComponent', () => {
  let component: MiseADispositionActifListComponent;
  let fixture: ComponentFixture<MiseADispositionActifListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MiseADispositionActifListComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(MiseADispositionActifListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
