import { TestBed } from '@angular/core/testing';

import { RestitutionService } from './restitution.service';
import { testProviders } from '../../../../testing/test-providers';

describe('RestitutionService', () => {
  let service: RestitutionService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: testProviders,
    });
    service = TestBed.inject(RestitutionService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
