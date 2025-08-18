import { ComponentFixture, TestBed } from '@angular/core/testing';

import { VirementActifListComponent } from './virement-actif-list.component';

describe('VirementActifListComponent', () => {
  let component: VirementActifListComponent;
  let fixture: ComponentFixture<VirementActifListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VirementActifListComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(VirementActifListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
