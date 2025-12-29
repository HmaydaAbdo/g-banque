import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SpecificLinesByMarcheComponent } from './specific-lines-by-marche.component';

describe('SpecificLinesByMarcheComponent', () => {
  let component: SpecificLinesByMarcheComponent;
  let fixture: ComponentFixture<SpecificLinesByMarcheComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SpecificLinesByMarcheComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(SpecificLinesByMarcheComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
