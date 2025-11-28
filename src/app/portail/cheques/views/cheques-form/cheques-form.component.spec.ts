import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ChequesFormComponent } from './cheques-form.component';

describe('ChequesFormComponent', () => {
  let component: ChequesFormComponent;
  let fixture: ComponentFixture<ChequesFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ChequesFormComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(ChequesFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
