import { ComponentFixture, TestBed } from '@angular/core/testing';

import { VirementEncoursComponent } from './virement-encours.component';

describe('VirementEncoursComponent', () => {
  let component: VirementEncoursComponent;
  let fixture: ComponentFixture<VirementEncoursComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VirementEncoursComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(VirementEncoursComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
