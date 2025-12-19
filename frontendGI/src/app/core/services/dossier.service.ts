import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { of } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { ApiService } from './api.service';
import { AuthService } from './auth.service';
import { Dossier, DossierStatus } from '../models/models';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class DossierService {
  constructor(private api: ApiService, private authService: AuthService, private http: HttpClient) {}

  submitApplication(candidatId: number, dossier: Dossier): Observable<Dossier> {
    return this.api.post<Dossier>(`/applications/submit`, dossier as any);
  }

  getDossierById(id: number): Observable<Dossier> {
    if (this.authService.isAgent() || this.authService.isAdmin()) {
      return this.api.get<any>(`/applications/${id}`).pipe(
        map(response => this.mapCompleteApplicationsToDossiers([response])[0])
      );
    } else {
      // Pour les candidats, utiliser l'endpoint de détails admin mais filtré
      return this.api.get<any>(`/admin/applications/${id}/details`).pipe(
        map(response => {
          if (response.success && response.application) {
            return this.mapCompleteApplicationsToDossiers([response.application])[0];
          }
          throw new Error('Application not found');
        }),
        catchError(error => {
          console.error('Error fetching application details:', error);
          return of(this.createEmptyDossier(id));
        })
      );
    }
  }

  getAllDossiers(): Observable<Dossier[]> {
    const currentUser = this.authService.currentUserValue;
    console.log('Debug - Current user:', currentUser);
    console.log('Debug - User roles:', currentUser?.roles);
    console.log('Debug - Has SUPER_ADMIN role:', this.authService.hasRole('ROLE_SUPER_ADMIN'));
    console.log('Debug - Has CANDIDAT role:', this.authService.hasRole('ROLE_CANDIDAT'));
    console.log('Debug - Token exists:', !!this.authService.getToken());
    
    if (this.authService.isAgent() || this.authService.isAdmin()) {
      console.log('Debug - Using /applications/all-simple endpoint');
      return this.api.get<any>(`/applications/all-simple`).pipe(
        map(response => {
          console.log('Debug - API response:', response);
          return this.mapCompleteApplicationsToDossiers(response);
        }),
        catchError(error => {
          console.error('Error fetching all applications:', error);
          return of([]);
        })
      );
    } else {
      console.log('Debug - Using /applications/my-applications endpoint');
      return this.api.get<any>(`/applications/my-applications`).pipe(
        map(response => {
          console.log('Debug - API response:', response);
          return this.mapApplicationsToDossiers(response);
        }),
        catchError(error => {
          console.error('Error fetching my applications:', error);
          return of([]);
        })
      );
    }
  }

  getMyApplications(): Observable<Dossier[]> {
    console.log('Debug - Getting my applications for candidate');
    return this.api.get<any>(`/applications/my-applications-simple`).pipe(
      map(response => {
        console.log('Debug - My applications response:', response);
        return this.mapApplicationsToDossiers(response);
      }),
      catchError(error => {
        console.error('Error fetching my applications:', error);
        return of([]);
      })
    );
  }

  private mapApplicationsToDossiers(response: any): Dossier[] {
    console.log('=== MAPPING DEBUG ===');
    console.log('Response:', response);
    
    const applications = response?.applications || response;
    
    if (!Array.isArray(applications)) {
      console.warn('Applications is not an array:', applications);
      return [];
    }
    
    console.log('Applications array:', applications);

    return applications.map(app => {
      console.log('Mapping application:', app);
      
      const dossier = {
        id: app.applicationId,
        candidat: {
          nom: 'Candidat',
          prenom: 'Utilisateur',
          email: response.username || 'N/A'
        },
        status: this.mapBackendStatusToFrontend(app.status),
        dateCreation: app.submissionDate,
        documents: app.documents || [],
        targetInstitution: app.targetInstitution || 'N/A',
        specialization: app.specialization || 'N/A',
        completionRate: app.completionRate || 0
      };
      
      console.log('Mapped dossier:', dossier);
      return dossier;
    });
  }

  private mapCompleteApplicationsToDossiers(response: any): Dossier[] {
    console.log('=== COMPLETE MAPPING DEBUG ===');
    console.log('Response:', response);
    
    const applications = response?.applications || response || [];
    
    if (!Array.isArray(applications)) {
      console.warn('Applications is not an array:', applications);
      return [];
    }
    
    return applications.map((app: any) => {
      console.log('Mapping complete application:', app);
      
      const dossier = {
        id: app.applicationId,
        candidat: {
          nom: app.personalInfo?.lastName || 'N/A',
          prenom: app.personalInfo?.firstName || 'N/A',
          email: app.personalInfo?.email || 'N/A',
          telephone: app.personalInfo?.phoneNumber || 'N/A',
          adresse: app.personalInfo?.address || 'N/A',
          nationalite: app.personalInfo?.nationality || 'N/A',
          dateNaissance: app.personalInfo?.dateOfBirth || 'N/A',
          genre: app.personalInfo?.gender || 'N/A',
          numeroId: app.personalInfo?.userIdNum || 'N/A',
          contactUrgence: app.personalInfo?.emergencyContact || 'N/A',
          dernierEtablissement: app.academicHistory?.lastInstitution || 'N/A',
          specialisation: app.academicHistory?.specialization || 'N/A',
          sousSpecialisation: app.academicHistory?.subSpecialization || 'N/A',
          niveauEducation: app.academicHistory?.educationLevel || 'N/A',
          moyenne: app.academicHistory?.gpa || 0,
          mentions: app.academicHistory?.honors || 'N/A',
          dateDebut: app.academicHistory?.startDate || 'N/A',
          dateFin: app.academicHistory?.endDate || 'N/A'
        },
        status: this.mapBackendStatusToFrontend(app.status),
        dateCreation: app.submissionDate || new Date().toISOString(),
        institutionCible: app.targetInstitution || 'N/A',
        specialisationDemandee: app.specialization || 'N/A',
        tauxCompletion: app.completionRate || 0,
        derniereMiseAJour: app.lastUpdated || app.submissionDate,
        documents: app.documents || []
      };
      
      console.log('Mapped complete dossier:', dossier);
      return dossier;
    });
  }

  private mapBackendStatusToFrontend(backendStatus: string): DossierStatus {
    switch (backendStatus?.toUpperCase()) {
      case 'UNDER_REVIEW':
      case 'PENDING':
        return DossierStatus.EN_ATTENTE;
      case 'IN_PROGRESS':
        return DossierStatus.EN_COURS;
      case 'APPROVED':
        return DossierStatus.VALIDE;
      case 'REJECTED':
        return DossierStatus.REJETE;
      default:
        return DossierStatus.EN_ATTENTE;
    }
  }

  deleteDossier(id: number): Observable<void> {
    return this.api.delete<void>(`/applications/${id}`);
  }

  updateDossierStatus(id: number, status: DossierStatus): Observable<any> {
    const statusMap: { [key in DossierStatus]?: string } = {
      [DossierStatus.EN_ATTENTE]: 'PENDING',
      [DossierStatus.EN_COURS]: 'UNDER_REVIEW', 
      [DossierStatus.VALIDE]: 'APPROVED',
      [DossierStatus.REJETE]: 'REJECTED'
    };
    
    const backendStatus = statusMap[status] || 'PENDING';
    return this.http.put(`${environment.apiUrl}/registration-form/status/${id}?status=${backendStatus}`, {});
  }

  downloadRegistrationForm(applicationId: number): Observable<Blob> {
    return this.http.get(`${environment.apiUrl}/registration-form/${applicationId}/generate`, {
      responseType: 'blob'
    });
  }

  private mapCompleteApplicationToDossier(app: any): Dossier {
    return {
      id: app.applicationId || app.id,
      candidat: {
        nom: app.personalInfo?.lastName || 'N/A',
        prenom: app.personalInfo?.firstName || 'N/A',
        email: app.personalInfo?.email || 'N/A',
        telephone: app.personalInfo?.phoneNumber || 'N/A',
        adresse: app.personalInfo?.address || 'N/A',
        nationalite: app.personalInfo?.nationality || 'N/A',
        dateNaissance: app.personalInfo?.dateOfBirth || 'N/A',
        genre: app.personalInfo?.gender || 'N/A',
        numeroId: app.personalInfo?.userIdNum || 'N/A',
        contactUrgence: app.personalInfo?.emergencyContact || 'N/A',
        dernierEtablissement: app.academicHistory?.lastInstitution || 'N/A',
        specialisation: app.academicHistory?.specialization || 'N/A',
        sousSpecialisation: app.academicHistory?.subSpecialization || 'N/A',
        niveauEducation: app.academicHistory?.educationLevel || 'N/A',
        moyenne: app.academicHistory?.gpa || 0,
        mentions: app.academicHistory?.honors || 'N/A',
        dateDebut: app.academicHistory?.startDate || 'N/A',
        dateFin: app.academicHistory?.endDate || 'N/A'
      },
      status: this.mapBackendStatusToFrontend(app.status),
      dateCreation: app.submissionDate || new Date().toISOString(),
      institutionCible: app.targetInstitution || 'N/A',
      specialisationDemandee: app.specialization || 'N/A',
      tauxCompletion: app.completionRate || 0,
      derniereMiseAJour: app.lastUpdated || app.submissionDate,
      documents: app.documents || []
    };
  }

  private createEmptyDossier(id: number): Dossier {
    return {
      id: id,
      candidat: {
        nom: 'N/A', prenom: 'N/A', email: 'N/A', telephone: 'N/A',
        adresse: 'N/A', nationalite: 'N/A', dateNaissance: 'N/A',
        genre: 'N/A', numeroId: 'N/A', contactUrgence: 'N/A',
        dernierEtablissement: 'N/A', specialisation: 'N/A',
        sousSpecialisation: 'N/A', niveauEducation: 'N/A',
        moyenne: 0, mentions: 'N/A', dateDebut: 'N/A', dateFin: 'N/A'
      },
      status: DossierStatus.EN_ATTENTE,
      dateCreation: new Date().toISOString(),
      documents: []
    };
  }
}