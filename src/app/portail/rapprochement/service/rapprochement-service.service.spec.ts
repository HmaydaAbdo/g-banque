import { TestBed } from '@angular/core/testing';

import { RapprochementServiceService } from './rapprochement-service.service';

describe('RapprochementServiceService', () => {
  let service: RapprochementServiceService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(RapprochementServiceService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
