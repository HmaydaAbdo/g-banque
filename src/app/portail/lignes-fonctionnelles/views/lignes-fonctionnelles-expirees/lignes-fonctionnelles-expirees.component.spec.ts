import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LignesFonctionnellesExpireesComponent } from './lignes-fonctionnelles-expirees.component';

describe('LignesFonctionnellesExpireesComponent', () => {
  let component: LignesFonctionnellesExpireesComponent;
  let fixture: ComponentFixture<LignesFonctionnellesExpireesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LignesFonctionnellesExpireesComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(LignesFonctionnellesExpireesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
