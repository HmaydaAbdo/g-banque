import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SelectBanquePourLEncoursComponent } from './select-banque-pour-l-encours.component';

describe('SelectBanquePourLEncoursComponent', () => {
  let component: SelectBanquePourLEncoursComponent;
  let fixture: ComponentFixture<SelectBanquePourLEncoursComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SelectBanquePourLEncoursComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(SelectBanquePourLEncoursComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
