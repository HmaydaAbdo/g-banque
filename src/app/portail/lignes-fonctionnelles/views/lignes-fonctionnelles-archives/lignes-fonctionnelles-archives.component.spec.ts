import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LignesFonctionnellesArchivesComponent } from './lignes-fonctionnelles-archives.component';

describe('LignesFonctionnellesArchivesComponent', () => {
  let component: LignesFonctionnellesArchivesComponent;
  let fixture: ComponentFixture<LignesFonctionnellesArchivesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LignesFonctionnellesArchivesComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(LignesFonctionnellesArchivesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
