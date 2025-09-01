import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LcnListComponent } from './lcn-list.component';

describe('LcnListComponent', () => {
  let component: LcnListComponent;
  let fixture: ComponentFixture<LcnListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LcnListComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(LcnListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
