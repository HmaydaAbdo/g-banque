import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { MessageService } from 'primeng/api';

import { appHttpInterceptorInterceptor } from './app-http-interceptor.interceptor';
import { AuthService } from '../services/auth.service';

describe('appHttpInterceptorInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let authService: AuthService;
  let messageService: MessageService;

  const unauthorized = { status: 401, statusText: 'Unauthorized' };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([appHttpInterceptorInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
        MessageService,
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
    authService = TestBed.inject(AuthService);
    messageService = TestBed.inject(MessageService);
    spyOn(messageService, 'add');
    spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
  });

  afterEach(() => backend.verify());

  function openSession(): void {
    authService.login({ username: 'alice', password: 'secret' }).subscribe();
    backend.expectOne(r => r.url.endsWith('auth/login')).flush({ username: 'alice', authorities: [] });
  }

  it('n\'ajoute aucun en-tête Authorization (jetons en cookies HttpOnly)', () => {
    http.get('/gbanque/api/banques').subscribe();
    const req = backend.expectOne('/gbanque/api/banques');
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush([]);
  });

  it('sur 401, renouvelle la session puis rejoue la requête une fois', () => {
    let body: unknown;
    http.get('/gbanque/api/banques').subscribe(b => body = b);

    backend.expectOne('/gbanque/api/banques').flush(null, unauthorized);
    backend.expectOne(r => r.url.endsWith('auth/refresh')).flush({ username: 'alice', authorities: [] });
    backend.expectOne('/gbanque/api/banques').flush(['BMCI']);

    expect(body).toEqual(['BMCI']);
    expect(messageService.add).not.toHaveBeenCalled();
  });

  it('si le renouvellement échoue, termine la session avec le message « Session expirée »', () => {
    openSession();
    spyOn(authService, 'endSession').and.callThrough();

    http.get('/gbanque/api/banques').subscribe({ error: () => undefined });
    backend.expectOne('/gbanque/api/banques').flush(null, unauthorized);
    backend.expectOne(r => r.url.endsWith('auth/refresh')).flush(null, unauthorized);

    expect(authService.endSession).toHaveBeenCalled();
    expect(messageService.add).toHaveBeenCalledWith(jasmine.objectContaining({ summary: 'Session expirée' }));
  });

  it('sans session ouverte (démarrage), un échec de renouvellement reste silencieux', () => {
    http.get('/gbanque/api/auth/profile').subscribe({ error: () => undefined });
    backend.expectOne('/gbanque/api/auth/profile').flush(null, unauthorized);
    backend.expectOne(r => r.url.endsWith('auth/refresh')).flush(null, unauthorized);

    expect(messageService.add).not.toHaveBeenCalled();
  });

  it('identifiants incorrects : message d\'erreur du backend, sans tentative de renouvellement', () => {
    authService.login({ username: 'alice', password: 'faux' }).subscribe({ error: () => undefined });
    backend.expectOne(r => r.url.endsWith('auth/login'))
      .flush({ error: 'Unauthorized', message: 'Identifiant ou mot de passe incorrect.' }, unauthorized);

    backend.expectNone(r => r.url.endsWith('auth/refresh'));
    expect(messageService.add).toHaveBeenCalledWith(jasmine.objectContaining({ detail: 'Identifiant ou mot de passe incorrect.' }));
  });

  it('autres erreurs (403, 500...) : message du backend, session conservée', () => {
    openSession();
    http.get('/gbanque/api/banques').subscribe({ error: () => undefined });
    backend.expectOne('/gbanque/api/banques')
      .flush({ error: 'Forbidden', message: 'Accès refusé.' }, { status: 403, statusText: 'Forbidden' });

    expect(messageService.add).toHaveBeenCalledWith(jasmine.objectContaining({ summary: 'Forbidden', detail: 'Accès refusé.' }));
    expect(authService.isAuthenticated()).toBeTrue();
  });
});
