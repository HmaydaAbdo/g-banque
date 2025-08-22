import { TestBed } from '@angular/core/testing';

import { RecapDecaissementService } from './recap-decaissement.service';

describe('RecapDecaissementService', () => {
  let service: RecapDecaissementService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(RecapDecaissementService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
