import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DetailsRapprochementComponent } from './details-rapprochement.component';

describe('DetailsRapprochementComponent', () => {
  let component: DetailsRapprochementComponent;
  let fixture: ComponentFixture<DetailsRapprochementComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DetailsRapprochementComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(DetailsRapprochementComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
