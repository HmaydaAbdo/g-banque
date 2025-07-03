import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TresorerieGlobaleComponent } from './tresorerie-globale.component';

describe('TresorerieGlobaleComponent', () => {
  let component: TresorerieGlobaleComponent;
  let fixture: ComponentFixture<TresorerieGlobaleComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TresorerieGlobaleComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(TresorerieGlobaleComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
