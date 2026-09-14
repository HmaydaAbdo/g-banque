import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DroitConstateDeblocageComponent } from './droit-constate-deblocage.component';

describe('DroitConstateDeblocageComponent', () => {
  let component: DroitConstateDeblocageComponent;
  let fixture: ComponentFixture<DroitConstateDeblocageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DroitConstateDeblocageComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(DroitConstateDeblocageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
