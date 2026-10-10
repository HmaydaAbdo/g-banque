import { TestBed } from '@angular/core/testing';
import { CanActivateFn } from '@angular/router';

import { systemAdminGuard } from './system-admin.guard';
import { testProviders } from '../../../testing/test-providers';

describe('systemAdminGuard', () => {
  const executeGuard: CanActivateFn = (...guardParameters) =>
      TestBed.runInInjectionContext(() => systemAdminGuard(...guardParameters));

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: testProviders,
    });
  });

  it('should be created', () => {
    expect(executeGuard).toBeTruthy();
  });
});
