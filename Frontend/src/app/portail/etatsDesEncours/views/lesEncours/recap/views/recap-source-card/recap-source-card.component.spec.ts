import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { RecapCardView, RecapSourceCardComponent } from './recap-source-card.component';
import { TransactionSource } from '../../../../../../transactions/dtos/TransactionSource';

describe('RecapSourceCardComponent', () => {
  let fixture: ComponentFixture<RecapSourceCardComponent>;
  let component: RecapSourceCardComponent;
  let ouvertures: boolean[];

  const carte = (echues: number): RecapCardView => ({
    key: TransactionSource.LEASING,
    label: 'Leasing',
    icon: 'pi pi-car',
    count: 28,
    debit: 1500,
    credit: 0,
    echues,
    debitPct: 100,
  });

  function creer(echues: number): void {
    fixture = TestBed.createComponent(RecapSourceCardComponent);
    component = fixture.componentInstance;
    component.card = carte(echues);
    ouvertures = [];
    component.ouvrir.subscribe(v => ouvertures.push(v));
    fixture.detectChanges();
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RecapSourceCardComponent],
      providers: [provideNoopAnimations()],
    }).compileComponents();
  });

  it('affiche le nom, le nombre et les montants de la source', () => {
    creer(0);
    const texte: string = fixture.nativeElement.textContent;
    expect(texte).toContain('Leasing');
    expect(texte).toContain('28');
    expect(texte).toContain('1,500.00');
  });

  it('un clic sur la carte demande l\'ouverture simple de la source', () => {
    creer(0);
    fixture.nativeElement.querySelector('.card').click();
    expect(ouvertures).toEqual([false]);
  });

  it('le lien « échues · voir » ouvre la source filtrée sur ses échues, sans double ouverture', () => {
    creer(3);
    const lien: HTMLButtonElement = fixture.nativeElement.querySelector('.card__echues');
    expect(lien.textContent).toContain('3 échues');
    lien.click();
    expect(ouvertures).toEqual([true]);
  });

  it('pas de lien d\'échues quand aucune opération n\'est échue', () => {
    creer(0);
    expect(fixture.nativeElement.querySelector('.card__echues')).toBeNull();
  });

  it('Entrée sur la carte l\'ouvre ; Entrée sur le lien ne l\'ouvre pas deux fois', () => {
    creer(1);
    const carteEl: HTMLElement = fixture.nativeElement.querySelector('.card');
    carteEl.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }));
    expect(ouvertures).toEqual([false]);
    const lien: HTMLElement = fixture.nativeElement.querySelector('.card__echues');
    lien.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }));
    expect(ouvertures).toEqual([false]);
  });
});
