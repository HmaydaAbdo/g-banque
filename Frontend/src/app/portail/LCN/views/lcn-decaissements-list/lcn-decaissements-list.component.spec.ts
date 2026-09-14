import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LcnDecaissementsListComponent } from './lcn-decaissements-list.component';

describe('LcnDecaissementsListComponent', () => {
  let component: LcnDecaissementsListComponent;
  let fixture: ComponentFixture<LcnDecaissementsListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LcnDecaissementsListComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(LcnDecaissementsListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
