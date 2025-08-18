import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EcheancesCmtComponent } from './echeances-cmt.component';

describe('EcheancesCmtComponent', () => {
  let component: EcheancesCmtComponent;
  let fixture: ComponentFixture<EcheancesCmtComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EcheancesCmtComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(EcheancesCmtComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
