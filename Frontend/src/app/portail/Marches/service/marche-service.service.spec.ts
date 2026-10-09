import { TestBed } from '@angular/core/testing';

import { MarcheService } from './marche-service.service';

describe('MarcheService', () => {
  let service: MarcheService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(MarcheService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
