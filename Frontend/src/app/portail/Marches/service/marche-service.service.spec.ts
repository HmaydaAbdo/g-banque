import { TestBed } from '@angular/core/testing';

import { MarcheServiceService } from './marche-service.service';

describe('MarcheServiceService', () => {
  let service: MarcheServiceService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(MarcheServiceService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
