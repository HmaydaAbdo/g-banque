import { ComponentFixture, TestBed } from '@angular/core/testing';

import { VirementErroneComponent } from './virement-errone.component';

describe('VirementErroneComponent', () => {
  let component: VirementErroneComponent;
  let fixture: ComponentFixture<VirementErroneComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VirementErroneComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(VirementErroneComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
