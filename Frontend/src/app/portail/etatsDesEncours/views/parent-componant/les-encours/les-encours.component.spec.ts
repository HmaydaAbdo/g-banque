import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LesEncoursComponent } from './les-encours.component';
import { testProviders } from '../../../../../../testing/test-providers';

describe('LesEncoursComponent', () => {
  let component: LesEncoursComponent;
  let fixture: ComponentFixture<LesEncoursComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: testProviders,
      imports: [LesEncoursComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(LesEncoursComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
