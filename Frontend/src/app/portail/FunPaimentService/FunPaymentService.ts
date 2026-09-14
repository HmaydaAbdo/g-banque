import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from "../../../environments/environment";


@Injectable({
  providedIn: 'root'
})
export class FunPaymentService {

  private apiUrl = `${environment.apiUrl}funPayment`; // Base URL for the FunPayment endpoints

  constructor(private http: HttpClient) {
  }

  /**
   * Triggers the payment import batch job on the backend.
   * @returns An Observable of a string response from the server.
   */
  synchronizePayments(): Observable<string> {
    const url = `${this.apiUrl}/synchronisation`;
    return this.http.post(url, {}, {responseType: 'text'});
  }
}
