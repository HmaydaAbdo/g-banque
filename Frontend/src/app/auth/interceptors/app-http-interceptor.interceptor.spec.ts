import { TestBed } from '@angular/core/testing';
import { HttpInterceptorFn } from '@angular/common/http';

import { appHttpInterceptorInterceptor } from './app-http-interceptor.interceptor';
import { testProviders } from '../../../testing/test-providers';

describe('appHttpInterceptorInterceptor', () => {
  const interceptor: HttpInterceptorFn = (req, next) => 
    TestBed.runInInjectionContext(() => appHttpInterceptorInterceptor(req, next));

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: testProviders,
    });
  });

  it('should be created', () => {
    expect(interceptor).toBeTruthy();
  });
});
