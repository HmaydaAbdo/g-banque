import { ComponentFixture, TestBed } from '@angular/core/testing';

import { OrganismeLeasingFormComponent } from './organisme-leasing-form.component';

describe('OrganismeLeasingFormComponent', () => {
  let component: OrganismeLeasingFormComponent;
  let fixture: ComponentFixture<OrganismeLeasingFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OrganismeLeasingFormComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(OrganismeLeasingFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
