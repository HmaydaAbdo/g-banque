import { TestBed } from '@angular/core/testing';

import { MarcheService } from './marche-service.service';
import { testProviders } from '../../../../testing/test-providers';

describe('MarcheService', () => {
  let service: MarcheService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: testProviders,
    });
    service = TestBed.inject(MarcheService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
