import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MiseADispositionListComponent } from './mise-a-disposition-list.component';

describe('MiseADispositionListComponent', () => {
  let component: MiseADispositionListComponent;
  let fixture: ComponentFixture<MiseADispositionListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MiseADispositionListComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MiseADispositionListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
