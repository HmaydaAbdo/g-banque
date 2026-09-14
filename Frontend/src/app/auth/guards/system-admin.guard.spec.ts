import { TestBed } from '@angular/core/testing';
import { CanActivateFn } from '@angular/router';

import { systemAdminGuard } from './system-admin.guard';

describe('systemAdminGuard', () => {
  const executeGuard: CanActivateFn = (...guardParameters) =>
      TestBed.runInInjectionContext(() => systemAdminGuard(...guardParameters));

  beforeEach(() => {
    TestBed.configureTestingModule({});
  });

  it('should be created', () => {
    expect(executeGuard).toBeTruthy();
  });
});
