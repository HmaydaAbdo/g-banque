import { TestBed } from '@angular/core/testing';

import { RestitutionService } from './restitution.service';

describe('RestitutionService', () => {
  let service: RestitutionService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(RestitutionService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
