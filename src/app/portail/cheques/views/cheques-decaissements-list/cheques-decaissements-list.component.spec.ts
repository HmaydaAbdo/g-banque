import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ChequesDecaissementsListComponent } from './cheques-decaissements-list.component';

describe('ChequesDecaissementsListComponent', () => {
  let component: ChequesDecaissementsListComponent;
  let fixture: ComponentFixture<ChequesDecaissementsListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ChequesDecaissementsListComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ChequesDecaissementsListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
