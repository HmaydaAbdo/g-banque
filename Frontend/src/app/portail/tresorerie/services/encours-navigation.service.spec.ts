import { TestBed } from '@angular/core/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { testProviders } from '../../../../testing/test-providers';
import { EncoursNavigationService } from './encours-navigation.service';

describe('EncoursNavigationService', () => {
  it("ouvre le récapitulatif du rapprochement « En cours » avec les filtres, sans écran de choix", () => {
    TestBed.configureTestingModule({ providers: testProviders });
    const navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    const http = TestBed.inject(HttpTestingController);

    TestBed.inject(EncoursNavigationService).ouvrirRecapitulatif(7, { horizon: 'FIN_DE_TRIMESTRE', source: 'LEASING' });
    http.expectOne(r => r.url.endsWith('/banque/7/actifs')).flush({ enCoursId: 42, provisoireId: 41 });

    expect(navigate).toHaveBeenCalledWith(
      ['/portail/etats-des-encours', 7, 42, 'recapitulatif'],
      { queryParams: { horizon: 'FIN_DE_TRIMESTRE', source: 'LEASING' } });
    http.verify();
  });
});
