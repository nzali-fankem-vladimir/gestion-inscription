import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

interface CandidateApplication {
  id: number;
  targetInstitution: string;
  specialization: string;
  status: string;
  submissionDate: string;
  completionRate: number;
}

@Component({
  selector: 'app-candidate-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100">
      <!-- Header Section -->
      <div class="bg-white shadow-sm border-b border-gray-200">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
          <div class="flex items-center justify-between">
            <div>
              <h1 class="text-3xl font-bold text-gray-900">Mon Espace Candidat</h1>
              <p class="mt-1 text-sm text-gray-600">Bienvenue dans votre espace personnel</p>
            </div>
            <button routerLink="/candidate/notifications" 
                    class="relative p-2 text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-full transition-colors">
              <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"/>
              </svg>
              <span *ngIf="unreadNotifications > 0" 
                    class="absolute top-0 right-0 inline-flex items-center justify-center px-2 py-1 text-xs font-bold leading-none text-white transform translate-x-1/2 -translate-y-1/2 bg-red-600 rounded-full">
                {{unreadNotifications}}
              </span>
            </button>
          </div>
        </div>
      </div>

      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        
        <!-- Quick Actions -->
        <div class="mb-8">
          <h2 class="text-xl font-semibold text-gray-900 mb-4">Actions rapides</h2>
          <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
            <button routerLink="/inscription" 
                    class="group bg-white hover:bg-blue-50 rounded-xl shadow-md hover:shadow-xl transition-all duration-300 p-6 text-left border-2 border-transparent hover:border-blue-500">
              <div class="flex items-center">
                <div class="flex-shrink-0 w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center group-hover:bg-blue-500 transition-colors">
                  <svg class="w-6 h-6 text-blue-600 group-hover:text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6"/>
                  </svg>
                </div>
                <div class="ml-4">
                  <h3 class="text-lg font-semibold text-gray-900">Nouvelle Candidature</h3>
                  <p class="text-sm text-gray-600">Démarrer une inscription</p>
                </div>
              </div>
            </button>

            <button routerLink="/candidate/dossiers" 
                    class="group bg-white hover:bg-green-50 rounded-xl shadow-md hover:shadow-xl transition-all duration-300 p-6 text-left border-2 border-transparent hover:border-green-500">
              <div class="flex items-center">
                <div class="flex-shrink-0 w-12 h-12 bg-green-100 rounded-lg flex items-center justify-center group-hover:bg-green-500 transition-colors">
                  <svg class="w-6 h-6 text-green-600 group-hover:text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
                  </svg>
                </div>
                <div class="ml-4">
                  <h3 class="text-lg font-semibold text-gray-900">Mes Dossiers</h3>
                  <p class="text-sm text-gray-600">Consulter mes candidatures</p>
                </div>
              </div>
            </button>

            <button routerLink="/settings" 
                    class="group bg-white hover:bg-purple-50 rounded-xl shadow-md hover:shadow-xl transition-all duration-300 p-6 text-left border-2 border-transparent hover:border-purple-500">
              <div class="flex items-center">
                <div class="flex-shrink-0 w-12 h-12 bg-purple-100 rounded-lg flex items-center justify-center group-hover:bg-purple-500 transition-colors">
                  <svg class="w-6 h-6 text-purple-600 group-hover:text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"/>
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
                  </svg>
                </div>
                <div class="ml-4">
                  <h3 class="text-lg font-semibold text-gray-900">Paramètres</h3>
                  <p class="text-sm text-gray-600">Gérer mon profil</p>
                </div>
              </div>
            </button>
          </div>
        </div>

        <!-- Statistics Cards -->
        <div class="mb-8">
          <h2 class="text-xl font-semibold text-gray-900 mb-4">Mes statistiques</h2>
          <div class="grid grid-cols-1 md:grid-cols-4 gap-6">
            <div class="bg-white rounded-xl shadow-md p-6 border-l-4 border-blue-500">
              <div class="flex items-center justify-between">
                <div>
                  <p class="text-sm font-medium text-gray-600">Total Candidatures</p>
                  <p class="text-3xl font-bold text-gray-900 mt-2">{{stats.total}}</p>
                </div>
                <div class="w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center">
                  <svg class="w-6 h-6 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
                  </svg>
                </div>
              </div>
            </div>

            <div class="bg-white rounded-xl shadow-md p-6 border-l-4 border-green-500">
              <div class="flex items-center justify-between">
                <div>
                  <p class="text-sm font-medium text-gray-600">Approuvées</p>
                  <p class="text-3xl font-bold text-green-600 mt-2">{{stats.approved}}</p>
                </div>
                <div class="w-12 h-12 bg-green-100 rounded-lg flex items-center justify-center">
                  <svg class="w-6 h-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
                  </svg>
                </div>
              </div>
            </div>

            <div class="bg-white rounded-xl shadow-md p-6 border-l-4 border-yellow-500">
              <div class="flex items-center justify-between">
                <div>
                  <p class="text-sm font-medium text-gray-600">En cours</p>
                  <p class="text-3xl font-bold text-yellow-600 mt-2">{{stats.pending}}</p>
                </div>
                <div class="w-12 h-12 bg-yellow-100 rounded-lg flex items-center justify-center">
                  <svg class="w-6 h-6 text-yellow-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
                  </svg>
                </div>
              </div>
            </div>

            <div class="bg-white rounded-xl shadow-md p-6 border-l-4 border-red-500">
              <div class="flex items-center justify-between">
                <div>
                  <p class="text-sm font-medium text-gray-600">Rejetées</p>
                  <p class="text-3xl font-bold text-red-600 mt-2">{{stats.rejected}}</p>
                </div>
                <div class="w-12 h-12 bg-red-100 rounded-lg flex items-center justify-center">
                  <svg class="w-6 h-6 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z"/>
                  </svg>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Recent Applications -->
        <div class="bg-white rounded-xl shadow-md overflow-hidden">
          <div class="px-6 py-4 border-b border-gray-200">
            <h2 class="text-xl font-semibold text-gray-900">Mes candidatures récentes</h2>
          </div>
          
          <div *ngIf="applications.length === 0" class="p-12 text-center">
            <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
            </svg>
            <h3 class="mt-2 text-sm font-medium text-gray-900">Aucune candidature</h3>
            <p class="mt-1 text-sm text-gray-500">Commencez par créer votre première candidature.</p>
            <div class="mt-6">
              <button routerLink="/inscription" 
                      class="inline-flex items-center px-4 py-2 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700">
                <svg class="-ml-1 mr-2 h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6"/>
                </svg>
                Nouvelle candidature
              </button>
            </div>
          </div>

          <div *ngIf="applications.length > 0" class="divide-y divide-gray-200">
            <div *ngFor="let app of applications" 
                 class="p-6 hover:bg-gray-50 transition-colors cursor-pointer"
                 (click)="viewApplication(app.id)">
              <div class="flex items-center justify-between">
                <div class="flex-1">
                  <div class="flex items-center justify-between mb-2">
                    <h3 class="text-lg font-semibold text-gray-900">{{app.targetInstitution}}</h3>
                    <span class="px-3 py-1 text-xs font-semibold rounded-full"
                          [ngClass]="{
                            'bg-green-100 text-green-800': app.status === 'APPROVED',
                            'bg-yellow-100 text-yellow-800': app.status === 'PENDING' || app.status === 'UNDER_REVIEW',
                            'bg-red-100 text-red-800': app.status === 'REJECTED',
                            'bg-blue-100 text-blue-800': app.status === 'DRAFT'
                          }">
                      {{getStatusLabel(app.status)}}
                    </span>
                  </div>
                  <p class="text-sm text-gray-600 mb-2">{{app.specialization}}</p>
                  <div class="flex items-center text-sm text-gray-500">
                    <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/>
                    </svg>
                    Soumise le {{formatDate(app.submissionDate)}}
                  </div>
                  <div class="mt-3">
                    <div class="flex items-center">
                      <div class="flex-1 bg-gray-200 rounded-full h-2">
                        <div class="bg-blue-600 h-2 rounded-full transition-all duration-300" 
                             [style.width.%]="app.completionRate"></div>
                      </div>
                      <span class="ml-3 text-sm font-medium text-gray-700">{{app.completionRate}}%</span>
                    </div>
                  </div>
                </div>
                <div class="ml-6">
                  <svg class="w-6 h-6 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
                  </svg>
                </div>
              </div>
            </div>
          </div>
        </div>

      </div>
    </div>
  `
})
export class CandidateDashboardComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  
  stats = {
    total: 0,
    approved: 0,
    pending: 0,
    rejected: 0
  };
  
  applications: CandidateApplication[] = [];
  unreadNotifications = 0;

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.loadCandidateData();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadCandidateData(): void {
    this.http.get<any>(`${environment.apiUrl}/applications/my-applications`)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          if (data.success && data.applications) {
            this.applications = data.applications;
            this.calculateStats();
          }
        },
        error: (error) => {
          console.error('Erreur chargement candidatures:', error);
        }
      });
  }

  private calculateStats(): void {
    this.stats = {
      total: this.applications.length,
      approved: this.applications.filter(app => app.status === 'APPROVED').length,
      pending: this.applications.filter(app => ['PENDING', 'UNDER_REVIEW'].includes(app.status)).length,
      rejected: this.applications.filter(app => app.status === 'REJECTED').length
    };
  }

  getStatusLabel(status: string): string {
    const labels: { [key: string]: string } = {
      'APPROVED': 'Approuvée',
      'PENDING': 'En attente',
      'REJECTED': 'Rejetée',
      'UNDER_REVIEW': 'En révision',
      'DRAFT': 'Brouillon'
    };
    return labels[status] || status;
  }

  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString('fr-FR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  }

  viewApplication(id: number): void {
    window.location.href = `/candidate/dossiers`;
  }
}
