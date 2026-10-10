import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, CanActivateFn, Router, RouterStateSnapshot } from '@angular/router';

import { authGuard } from './auth.guard';
import { AuthService } from '../services/auth.service';
import { testProviders } from '../../../testing/test-providers';

describe('authGuard', () => {
  const executeGuard: CanActivateFn = (...guardParameters) =>
      TestBed.runInInjectionContext(() => authGuard(...guardParameters));

  const route = {} as ActivatedRouteSnapshot;
  const state = { url: '/portail/banques' } as RouterStateSnapshot;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: testProviders,
    });
    spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
  });

  it('laisse passer quand une session est ouverte', () => {
    spyOn(TestBed.inject(AuthService), 'isAuthenticated').and.returnValue(true);
    expect(executeGuard(route, state)).toBeTrue();
  });

  it('renvoie vers la connexion (avec retour prévu) sans session', () => {
    spyOn(TestBed.inject(AuthService), 'isAuthenticated').and.returnValue(false);
    expect(executeGuard(route, state)).toBeFalse();
    expect(TestBed.inject(Router).navigate).toHaveBeenCalledWith(['/login'], { queryParams: { returnUrl: '/portail/banques' } });
  });
});
