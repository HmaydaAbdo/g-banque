import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CmtListArchivedComponent } from './cmt-list-archived.component';
import { testProviders } from '../../../../../testing/test-providers';

describe('CmtListArchivedComponent', () => {
  let component: CmtListArchivedComponent;
  let fixture: ComponentFixture<CmtListArchivedComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: testProviders,
      imports: [CmtListArchivedComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(CmtListArchivedComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
