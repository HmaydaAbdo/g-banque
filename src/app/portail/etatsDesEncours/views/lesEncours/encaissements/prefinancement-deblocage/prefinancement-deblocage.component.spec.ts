import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PrefinancementDeblocageComponent } from './prefinancement-deblocage.component';

describe('PrefinancementDeblocageComponent', () => {
  let component: PrefinancementDeblocageComponent;
  let fixture: ComponentFixture<PrefinancementDeblocageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PrefinancementDeblocageComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(PrefinancementDeblocageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
