import { ComponentFixture, TestBed } from '@angular/core/testing';

import { VirementEncaissementListComponent } from './virement-encaissement-list.component';

describe('VirementEncaissementListComponent', () => {
  let component: VirementEncaissementListComponent;
  let fixture: ComponentFixture<VirementEncaissementListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VirementEncaissementListComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(VirementEncaissementListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
