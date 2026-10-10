import { TestBed } from '@angular/core/testing';

import { LogService } from './log-service.service';
import { testProviders } from '../../../../../testing/test-providers';

describe('LogService', () => {
  let service: LogService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: testProviders,
    });
    service = TestBed.inject(LogService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
