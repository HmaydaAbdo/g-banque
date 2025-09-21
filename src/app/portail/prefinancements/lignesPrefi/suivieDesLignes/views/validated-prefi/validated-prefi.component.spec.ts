import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ValidatedPrefiComponent } from './validated-prefi.component';

describe('ValidatedPrefiComponent', () => {
  let component: ValidatedPrefiComponent;
  let fixture: ComponentFixture<ValidatedPrefiComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ValidatedPrefiComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(ValidatedPrefiComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
