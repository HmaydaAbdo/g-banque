import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LcnActifListComponent } from './lcn-actif-list.component';

describe('LcnActifListComponent', () => {
  let component: LcnActifListComponent;
  let fixture: ComponentFixture<LcnActifListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LcnActifListComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(LcnActifListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
