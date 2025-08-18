import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ChequesActifListComponent } from './cheques-actif-list.component';

describe('ChequesActifListComponent', () => {
  let component: ChequesActifListComponent;
  let fixture: ComponentFixture<ChequesActifListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ChequesActifListComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(ChequesActifListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
