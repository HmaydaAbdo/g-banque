import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MiseADispositionFormComponent } from './mise-a-disposition-form.component';

describe('MiseADispositionFormComponent', () => {
  let component: MiseADispositionFormComponent;
  let fixture: ComponentFixture<MiseADispositionFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MiseADispositionFormComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(MiseADispositionFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
