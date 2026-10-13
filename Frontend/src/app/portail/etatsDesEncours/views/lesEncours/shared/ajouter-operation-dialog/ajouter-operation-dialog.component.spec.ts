import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { AjouterItem, AjouterOperationDialogComponent } from './ajouter-operation-dialog.component';
import { TransactionSource } from '../../../../../transactions/dtos/TransactionSource';

describe('AjouterOperationDialogComponent', () => {
  let component: AjouterOperationDialogComponent;
  let fixture: ComponentFixture<AjouterOperationDialogComponent>;

  const items: AjouterItem[] = [
    { source: TransactionSource.CHEQUE, label: 'Chèque', icon: 'pi pi-money-bill', route: ['/portail/Cheques/new'] },
    { source: TransactionSource.Virement, label: 'Virement', icon: 'pi pi-sync', route: ['/portail/Virements/new'] },
    { source: TransactionSource.AGIOS, label: 'Agios', icon: 'pi pi-percentage', route: null },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AjouterOperationDialogComponent],
      providers: [provideNoopAnimations()],
    }).compileComponents();

    fixture = TestBed.createComponent(AjouterOperationDialogComponent);
    component = fixture.componentInstance;
    component.items = items;
    fixture.detectChanges();
  });

  it('liste toutes les sources sans recherche', () => {
    expect(component.itemsFiltres.length).toBe(3);
  });

  it('cherche sans tenir compte des majuscules ni des accents', () => {
    component.recherche.setValue('CHEQUE');
    expect(component.itemsFiltres.map(i => i.source)).toEqual([TransactionSource.CHEQUE]);
  });

  it('remonte le choix d\'une source créable', () => {
    const choisies: AjouterItem[] = [];
    component.choisir.subscribe(i => choisies.push(i));
    component.choisirItem(items[0]);
    expect(choisies).toEqual([items[0]]);
  });

  it('ignore une source sans route de création', () => {
    const choisies: AjouterItem[] = [];
    component.choisir.subscribe(i => choisies.push(i));
    component.choisirItem(items[2]);
    expect(choisies).toEqual([]);
  });

  it('Entrée ouvre la première source créable de la liste filtrée', () => {
    const choisies: AjouterItem[] = [];
    component.choisir.subscribe(i => choisies.push(i));
    component.recherche.setValue('agios');
    component.onEntree();
    expect(choisies).toEqual([]);
    component.recherche.setValue('vir');
    component.onEntree();
    expect(choisies).toEqual([items[1]]);
  });
});
