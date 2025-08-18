import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RecapRapprochementComponent } from './recap-rapprochement.component';

describe('RecapRapprochementComponent', () => {
  let component: RecapRapprochementComponent;
  let fixture: ComponentFixture<RecapRapprochementComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RecapRapprochementComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(RecapRapprochementComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
