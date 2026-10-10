import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LcnEscompteByLineComponent } from './lcn-escompte-by-line.component';
import { testProviders } from '../../../../../testing/test-providers';

describe('LcnEscompteByLineComponent', () => {
  let component: LcnEscompteByLineComponent;
  let fixture: ComponentFixture<LcnEscompteByLineComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: testProviders,
      imports: [LcnEscompteByLineComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(LcnEscompteByLineComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
