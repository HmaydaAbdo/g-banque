import { ComponentFixture, TestBed } from '@angular/core/testing';

import { VirementListComponent } from './virement-list.component';

describe('VirementListComponent', () => {
  let component: VirementListComponent;
  let fixture: ComponentFixture<VirementListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VirementListComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(VirementListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
