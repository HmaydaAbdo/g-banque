import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RecapDecaissementComponent } from './recap-decaissement.component';

describe('RecapDecaissementComponent', () => {
  let component: RecapDecaissementComponent;
  let fixture: ComponentFixture<RecapDecaissementComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RecapDecaissementComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(RecapDecaissementComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
