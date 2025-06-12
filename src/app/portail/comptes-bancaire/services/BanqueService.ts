
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {environment} from "../../../../environments/environment";
import {BanqueRequest} from "../dtos/BanqueRequest";
import {BanqueResponse} from "../dtos/BanqueResponse";
import {BanqueSearchCriteria} from "../types/BanqueSearchCriteria";
import {PageResponse} from "../../global/types/PageResponse";
import {RecapitulatifCompte} from "../../accueil/dtos/RecapitulatifCompte";



@Injectable({
  providedIn: 'root'
})
export class BanqueService {

  private apiUrl = `${environment.apiUrl}banques`;

  constructor(private http: HttpClient) { }

  /**
   * Crée une nouvelle banque.
   * @param banqueRequest Les données de la banque.
   * @param logoFile Le fichier logo.
   * @returns Un Observable de BanqueResponse.
   */
  createBanque(banqueRequest: BanqueRequest, logoFile: File): Observable<BanqueResponse> {
    const formData = new FormData();
    formData.append('banque', new Blob([JSON.stringify(banqueRequest)], { type: 'application/json' }));
    formData.append('logoFile', logoFile, logoFile.name);

    return this.http.post<BanqueResponse>(this.apiUrl, formData);
  }

  /**
   * Récupère une banque par son ID.
   * @param id L'ID de la banque.
   * @returns Un Observable de BanqueResponse.
   */
  getBanqueById(id: number): Observable<BanqueResponse> {
    return this.http.get<BanqueResponse>(`${this.apiUrl}/${id}`);
  }

  /**
   * Récupère toutes les banques avec recherche, pagination et tri.
   * @param criteria Les critères de recherche.
   * @param page Le numéro de page (0-indexed).
   * @param size Le nombre d'éléments par page.
   * @param sort La chaîne de tri (ex: "nom,asc").
   * @returns Un Observable de PageResponse<BanqueResponse>.
   */
  getAllBanques(
    criteria: BanqueSearchCriteria,
    page: number = 0,
    size: number = 10,
    sort: string = 'id,asc'
  ): Observable<PageResponse<BanqueResponse>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    // Ajouter les critères de recherche si présents
    if (criteria.nom) {
      params = params.set('nom', criteria.nom);
    }
    if (criteria.numeroDeCompte) {
      params = params.set('numeroDeCompte', criteria.numeroDeCompte);
    }
    if (criteria.minSolde !== undefined && criteria.minSolde !== null) {
      params = params.set('minSolde', criteria.minSolde.toString());
    }
    if (criteria.maxSolde !== undefined && criteria.maxSolde !== null) {
      params = params.set('maxSolde', criteria.maxSolde.toString());
    }

    return this.http.get<PageResponse<BanqueResponse>>(this.apiUrl, { params });
  }

  /**
   * Met à jour une banque existante.
   * @param id L'ID de la banque à mettre à jour.
   * @param banqueRequest Les données de la banque mises à jour.
   * @param logoFile Le nouveau fichier logo (optionnel).
   * @returns Un Observable de BanqueResponse.
   */
  updateBanque(id: number, banqueRequest: BanqueRequest, logoFile: File | null): Observable<BanqueResponse> {
    const formData = new FormData();
    formData.append('banque', new Blob([JSON.stringify(banqueRequest)], { type: 'application/json' }));

    if (logoFile) { // Envoyer le fichier uniquement s'il est fourni
      formData.append('logoFile', logoFile, logoFile.name);
    }

    return this.http.put<BanqueResponse>(`${this.apiUrl}/${id}`, formData);
  }

  /**
   * Supprime une banque par son ID.
   * @param id L'ID de la banque à supprimer.
   * @returns Un Observable<void>.
   */
  deleteBanque(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  /**
   * Récupère les informations récapitulatives des banques pour le composant d'accueil.
   * @returns Un Observable d'un tableau de RecapitulatifCompte.
   */
  getAccueilBankInfo(): Observable<RecapitulatifCompte[]> {
    return this.http.get<RecapitulatifCompte[]>(`${this.apiUrl}/accueil-info`);
  }
}
