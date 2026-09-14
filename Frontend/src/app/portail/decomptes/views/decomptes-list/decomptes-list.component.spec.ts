import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DecomptesListComponent } from './decomptes-list.component';

describe('DecomptesListComponent', () => {
  let component: DecomptesListComponent;
  let fixture: ComponentFixture<DecomptesListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DecomptesListComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(DecomptesListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
