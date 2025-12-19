import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AdminApplicationsService {
  private apiUrl = `${environment.apiUrl}/simple-admin`;

  constructor(private http: HttpClient) {}

  getApplications(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/applications`);
  }

  getStats(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/stats`);
  }
}