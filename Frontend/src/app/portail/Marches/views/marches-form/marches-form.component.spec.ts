import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MarchesFormComponent } from './marches-form.component';

describe('MarchesFormComponent', () => {
  let component: MarchesFormComponent;
  let fixture: ComponentFixture<MarchesFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MarchesFormComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(MarchesFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
