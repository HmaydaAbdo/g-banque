import { TestBed } from '@angular/core/testing';

import { RapprochementService } from './rapprochement-service.service';
import { testProviders } from '../../../../testing/test-providers';

describe('RapprochementService', () => {
  let service: RapprochementService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: testProviders,
    });
    service = TestBed.inject(RapprochementService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
