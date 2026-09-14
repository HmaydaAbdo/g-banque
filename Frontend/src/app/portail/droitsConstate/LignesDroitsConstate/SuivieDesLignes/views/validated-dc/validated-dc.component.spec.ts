import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ValidatedDcComponent } from './validated-dc.component';

describe('ValidatedDcComponent', () => {
  let component: ValidatedDcComponent;
  let fixture: ComponentFixture<ValidatedDcComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ValidatedDcComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(ValidatedDcComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
