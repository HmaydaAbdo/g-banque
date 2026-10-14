import { TestBed } from '@angular/core/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { testProviders } from '../../../../testing/test-providers';
import { TresorerieService } from './tresorerie.service';

describe('TresorerieService', () => {
  let service: TresorerieService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: testProviders });
    service = TestBed.inject(TresorerieService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it("demande le plan à l'horizon voulu", () => {
    service.getPlan('FIN_DE_TRIMESTRE').subscribe();
    const req = http.expectOne(r => r.url.endsWith('tresorerie-previsionnelle'));
    expect(req.request.params.get('periode')).toBe('FIN_DE_TRIMESTRE');
  });

  it("n'envoie la banque à l'export que pour une banque précise", () => {
    service.exportPdf('FIN_DE_MOIS', null).subscribe();
    expect(http.expectOne(r => r.url.endsWith('tresorerie-previsionnelle/pdf')).request.params.has('banqueId')).toBeFalse();

    service.exportPdf('FIN_DE_MOIS', 7).subscribe();
    const req = http.expectOne(r => r.url.endsWith('tresorerie-previsionnelle/pdf'));
    expect(req.request.params.get('banqueId')).toBe('7');
    expect(req.request.responseType).toBe('blob');
  });
});
