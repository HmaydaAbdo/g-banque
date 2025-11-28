import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EncaissementLcnEncoursComponent } from './encaissement-lcn-encours.component';

describe('EncaissementLcnEncoursComponent', () => {
  let component: EncaissementLcnEncoursComponent;
  let fixture: ComponentFixture<EncaissementLcnEncoursComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EncaissementLcnEncoursComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(EncaissementLcnEncoursComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
