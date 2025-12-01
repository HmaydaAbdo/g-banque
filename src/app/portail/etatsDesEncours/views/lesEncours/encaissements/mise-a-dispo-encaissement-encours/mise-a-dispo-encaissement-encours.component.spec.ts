import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MiseADispoEncaissementEncoursComponent } from './mise-a-dispo-encaissement-encours.component';

describe('MiseADispoEncaissementEncoursComponent', () => {
  let component: MiseADispoEncaissementEncoursComponent;
  let fixture: ComponentFixture<MiseADispoEncaissementEncoursComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MiseADispoEncaissementEncoursComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(MiseADispoEncaissementEncoursComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
