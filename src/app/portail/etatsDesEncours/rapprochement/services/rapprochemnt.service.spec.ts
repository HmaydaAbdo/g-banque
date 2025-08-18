import { TestBed } from '@angular/core/testing';

import { RapprochemntService } from './rapprochemnt.service';

describe('RapprochemntService', () => {
  let service: RapprochemntService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(RapprochemntService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
