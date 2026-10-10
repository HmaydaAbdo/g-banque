import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LcnFormComponent } from './lcn-form.component';
import { testProviders } from '../../../../../testing/test-providers';

describe('LcnFormComponent', () => {
  let component: LcnFormComponent;
  let fixture: ComponentFixture<LcnFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: testProviders,
      imports: [LcnFormComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(LcnFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
