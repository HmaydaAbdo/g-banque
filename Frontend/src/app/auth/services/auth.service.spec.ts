import { TestBed } from '@angular/core/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';

import { AuthService } from './auth.service';
import { testProviders } from '../../../testing/test-providers';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;
  let router: Router;

  const profile = { username: 'alice', authorities: ['DEPT_Finance:DIRECTOR'] };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: testProviders,
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
  });

  afterEach(() => http.verify());

  it('ne stocke rien dans le localStorage à la connexion (jetons en cookies HttpOnly)', () => {
    spyOn(localStorage, 'setItem');
    service.login({ username: 'alice', password: 'secret' }).subscribe();
    http.expectOne(r => r.url.endsWith('auth/login') && r.method === 'POST').flush(profile);

    expect(localStorage.setItem).not.toHaveBeenCalled();
    expect(service.isAuthenticated()).toBeTrue();
    expect(service.getUsername()).toBe('alice');
    expect(service.hasDepartmentRole('Finance', 'DIRECTOR')).toBeTrue();
    expect(service.isSystemAdmin()).toBeFalse();
  });

  it('restaure la session existante au démarrage', () => {
    service.restoreSession().subscribe();
    http.expectOne(r => r.url.endsWith('auth/profile')).flush(profile);
    expect(service.isAuthenticated()).toBeTrue();
  });

  it('reste déconnecté sans erreur si aucune session au démarrage', () => {
    let completed = false;
    service.restoreSession().subscribe({ complete: () => completed = true });
    http.expectOne(r => r.url.endsWith('auth/profile')).flush(null, { status: 401, statusText: 'Unauthorized' });
    expect(completed).toBeTrue();
    expect(service.isAuthenticated()).toBeFalse();
  });

  it('ne lance qu\'un seul renouvellement pour des appels simultanés', () => {
    const results: boolean[] = [];
    service.refreshSession().subscribe(r => results.push(r));
    service.refreshSession().subscribe(r => results.push(r));

    http.expectOne(r => r.url.endsWith('auth/refresh')).flush(profile);
    expect(results).toEqual([true, true]);
    expect(service.getUsername()).toBe('alice');
  });

  it('considère un 204 (déjà renouvelé par un autre onglet) comme un succès', () => {
    let result: boolean | undefined;
    service.refreshSession().subscribe(r => result = r);
    http.expectOne(r => r.url.endsWith('auth/refresh')).flush(null, { status: 204, statusText: 'No Content' });
    expect(result).toBeTrue();
  });

  it('signale l\'échec du renouvellement sans lever d\'erreur', () => {
    let result: boolean | undefined;
    service.refreshSession().subscribe(r => result = r);
    http.expectOne(r => r.url.endsWith('auth/refresh')).flush(null, { status: 401, statusText: 'Unauthorized' });
    expect(result).toBeFalse();
  });

  it('déconnecte localement même si la révocation serveur échoue', () => {
    service.login({ username: 'alice', password: 'secret' }).subscribe();
    http.expectOne(r => r.url.endsWith('auth/login')).flush(profile);

    let emitted: boolean | undefined;
    service.logout().subscribe(r => emitted = r);
    http.expectOne(r => r.url.endsWith('auth/logout')).flush(null, { status: 500, statusText: 'Error' });

    expect(emitted).toBeTrue();
    expect(service.isAuthenticated()).toBeFalse();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });
});
