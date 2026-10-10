import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LogsComponent } from './logs.component';
import { testProviders } from '../../../../../../testing/test-providers';

describe('LogsComponent', () => {
  let component: LogsComponent;
  let fixture: ComponentFixture<LogsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: testProviders,
      imports: [LogsComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(LogsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
