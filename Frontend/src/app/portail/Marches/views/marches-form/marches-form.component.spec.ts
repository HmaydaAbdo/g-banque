import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MarchesFormComponent } from './marches-form.component';
import { testProviders } from '../../../../../testing/test-providers';

describe('MarchesFormComponent', () => {
  let component: MarchesFormComponent;
  let fixture: ComponentFixture<MarchesFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: testProviders,
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
