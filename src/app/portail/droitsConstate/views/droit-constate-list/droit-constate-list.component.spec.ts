import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DroitConstateListComponent } from './droit-constate-list.component';

describe('DroitConstateListComponent', () => {
  let component: DroitConstateListComponent;
  let fixture: ComponentFixture<DroitConstateListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DroitConstateListComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(DroitConstateListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
