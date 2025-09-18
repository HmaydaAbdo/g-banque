import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RecapEncaissementsComponent } from './recap-encaissements.component';

describe('RecapEncaissementsComponent', () => {
  let component: RecapEncaissementsComponent;
  let fixture: ComponentFixture<RecapEncaissementsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RecapEncaissementsComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(RecapEncaissementsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
