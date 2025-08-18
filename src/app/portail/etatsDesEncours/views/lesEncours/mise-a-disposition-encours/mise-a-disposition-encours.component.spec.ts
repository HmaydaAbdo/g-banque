import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MiseADispositionEncoursComponent } from './mise-a-disposition-encours.component';

describe('MiseADispositionEncoursComponent', () => {
  let component: MiseADispositionEncoursComponent;
  let fixture: ComponentFixture<MiseADispositionEncoursComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MiseADispositionEncoursComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(MiseADispositionEncoursComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
