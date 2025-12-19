import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Subject, takeUntil, interval } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { ChartsComponent } from '../../shared/components/charts.component';

interface AgentApplication {
  id: number;
  candidateName: string;
  email: string;
  status: string;
  submissionDate: string;
  assignedAgent?: string;
}

@Component({
  selector: 'app-agent-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, ChartsComponent],
  template: `
    <div class="min-h-screen bg-gray-50">
      <!-- Header -->
      <div class="bg-white shadow-sm border-b border-gray-200">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
          <div class="flex items-center justify-between">
            <div>
              <h1 class="text-3xl font-bold text-gray-900">Espace Agent</h1>
              <p class="mt-1 text-sm text-gray-600">Gestion et traitement des candidatures</p>
            </div>
            <div class="flex items-center space-x-4">
              <button routerLink="/agent/reports" 
                      class="inline-flex items-center px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50">
                <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
                </svg>
                Rapports
              </button>
            </div>
          </div>
        </div>
      </div>

      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        
        <!-- Quick Actions -->
        <div class="mb-8">
          <h2 class="text-xl font-semibold text-gray-900 mb-4">Actions rapides</h2>
          <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
            <button routerLink="/agent/dossiers" 
                    class="group bg-white hover:bg-blue-50 rounded-lg shadow-md hover:shadow-lg transition-all p-6 text-left border-2 border-transparent hover:border-blue-500">
              <div class="flex items-center">
                <div class="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center group-hover:bg-blue-500 transition-colors">
                  <svg class="w-5 h-5 text-blue-600 group-hover:text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
                  </svg>
                </div>
                <div class="ml-3">
                  <h3 class="text-sm font-semibold text-gray-900">Dossiers</h3>
                  <p class="text-xs text-gray-600">Gérer les candidatures</p>
                </div>
              </div>
            </button>

            <button routerLink="/agent/documents" 
                    class="group bg-white hover:bg-green-50 rounded-lg shadow-md hover:shadow-lg transition-all p-6 text-left border-2 border-transparent hover:border-green-500">
              <div class="flex items-center">
                <div class="w-10 h-10 bg-green-100 rounded-lg flex items-center justify-center group-hover:bg-green-500 transition-colors">
                  <svg class="w-5 h-5 text-green-600 group-hover:text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z"/>
                  </svg>
                </div>
                <div class="ml-3">
                  <h3 class="text-sm font-semibold text-gray-900">Documents</h3>
                  <p class="text-xs text-gray-600">Vérifier les pièces</p>
                </div>
              </div>
            </button>

            <button (click)="refreshData()" 
                    class="group bg-white hover:bg-purple-50 rounded-lg shadow-md hover:shadow-lg transition-all p-6 text-left border-2 border-transparent hover:border-purple-500">
              <div class="flex items-center">
                <div class="w-10 h-10 bg-purple-100 rounded-lg flex items-center justify-center group-hover:bg-purple-500 transition-colors">
                  <svg class="w-5 h-5 text-purple-600 group-hover:text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
                  </svg>
                </div>
                <div class="ml-3">
                  <h3 class="text-sm font-semibold text-gray-900">Actualiser</h3>
                  <p class="text-xs text-gray-600">Recharger les données</p>
                </div>
              </div>
            </button>

            <button routerLink="/agent/reports" 
                    class="group bg-white hover:bg-orange-50 rounded-lg shadow-md hover:shadow-lg transition-all p-6 text-left border-2 border-transparent hover:border-orange-500">
              <div class="flex items-center">
                <div class="w-10 h-10 bg-orange-100 rounded-lg flex items-center justify-center group-hover:bg-orange-500 transition-colors">
                  <svg class="w-5 h-5 text-orange-600 group-hover:text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
                  </svg>
                </div>
                <div class="ml-3">
                  <h3 class="text-sm font-semibold text-gray-900">Statistiques</h3>
                  <p class="text-xs text-gray-600">Voir les rapports</p>
                </div>
              </div>
            </button>
          </div>
        </div>

        <!-- Statistics Cards -->
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
          <div class="bg-gradient-to-br from-blue-500 to-blue-600 rounded-lg shadow-lg p-6 text-white">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-blue-100 text-sm font-medium">Total Candidatures</p>
                <p class="text-3xl font-bold mt-2">{{stats.totalApplications}}</p>
              </div>
              <div class="w-12 h-12 bg-blue-400 bg-opacity-30 rounded-lg flex items-center justify-center">
                <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
                </svg>
              </div>
            </div>
          </div>

          <div class="bg-gradient-to-br from-yellow-500 to-yellow-600 rounded-lg shadow-lg p-6 text-white">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-yellow-100 text-sm font-medium">En attente</p>
                <p class="text-3xl font-bold mt-2">{{stats.pendingApplications}}</p>
              </div>
              <div class="w-12 h-12 bg-yellow-400 bg-opacity-30 rounded-lg flex items-center justify-center">
                <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
                </svg>
              </div>
            </div>
          </div>

          <div class="bg-gradient-to-br from-green-500 to-green-600 rounded-lg shadow-lg p-6 text-white">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-green-100 text-sm font-medium">Approuvées</p>
                <p class="text-3xl font-bold mt-2">{{stats.approvedApplications}}</p>
              </div>
              <div class="w-12 h-12 bg-green-400 bg-opacity-30 rounded-lg flex items-center justify-center">
                <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
                </svg>
              </div>
            </div>
          </div>

          <div class="bg-gradient-to-br from-red-500 to-red-600 rounded-lg shadow-lg p-6 text-white">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-red-100 text-sm font-medium">Rejetées</p>
                <p class="text-3xl font-bold mt-2">{{stats.rejectedApplications}}</p>
              </div>
              <div class="w-12 h-12 bg-red-400 bg-opacity-30 rounded-lg flex items-center justify-center">
                <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z"/>
                </svg>
              </div>
            </div>
          </div>
        </div>

        <!-- Charts -->
        <div class="mb-8">
          <app-charts></app-charts>
        </div>

        <!-- Applications Table -->
        <div class="bg-white rounded-lg shadow-md overflow-hidden">
          <div class="px-6 py-4 border-b border-gray-200 flex items-center justify-between">
            <h2 class="text-xl font-semibold text-gray-900">Candidatures à traiter</h2>
            <div class="flex items-center space-x-2">
              <select class="border-gray-300 rounded-md text-sm focus:ring-blue-500 focus:border-blue-500"
                      (change)="filterByStatus($event)">
                <option value="">Tous les statuts</option>
                <option value="PENDING">En attente</option>
                <option value="UNDER_REVIEW">En révision</option>
                <option value="APPROVED">Approuvées</option>
                <option value="REJECTED">Rejetées</option>
              </select>
            </div>
          </div>

          <div class="overflow-x-auto">
            <table class="min-w-full divide-y divide-gray-200">
              <thead class="bg-gray-50">
                <tr>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Candidat
                  </th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Email
                  </th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Statut
                  </th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Date de soumission
                  </th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody class="bg-white divide-y divide-gray-200">
                <tr *ngFor="let app of filteredApplications" class="hover:bg-gray-50 transition-colors">
                  <td class="px-6 py-4 whitespace-nowrap">
                    <div class="text-sm font-medium text-gray-900">{{app.candidateName}}</div>
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap">
                    <div class="text-sm text-gray-500">{{app.email}}</div>
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap">
                    <span class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full"
                          [ngClass]="{
                            'bg-green-100 text-green-800': app.status === 'APPROVED',
                            'bg-yellow-100 text-yellow-800': app.status === 'PENDING',
                            'bg-blue-100 text-blue-800': app.status === 'UNDER_REVIEW',
                            'bg-red-100 text-red-800': app.status === 'REJECTED'
                          }">
                      {{getStatusLabel(app.status)}}
                    </span>
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {{formatDate(app.submissionDate)}}
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm font-medium">
                    <button (click)="processApplication(app.id)" 
                            class="text-blue-600 hover:text-blue-900 mr-3">
                      Traiter
                    </button>
                    <button (click)="viewDetails(app.id)" 
                            class="text-gray-600 hover:text-gray-900">
                      Détails
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div *ngIf="filteredApplications.length === 0" class="p-12 text-center">
            <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
            </svg>
            <h3 class="mt-2 text-sm font-medium text-gray-900">Aucune candidature</h3>
            <p class="mt-1 text-sm text-gray-500">Aucune candidature ne correspond aux critères sélectionnés.</p>
          </div>
        </div>

      </div>
    </div>
  `
})
export class AgentDashboardComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  
  stats = {
    totalApplications: 0,
    approvedApplications: 0,
    pendingApplications: 0,
    rejectedApplications: 0
  };
  
  applications: AgentApplication[] = [];
  filteredApplications: AgentApplication[] = [];
  currentFilter = '';

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.loadAgentData();
    
    // Auto-refresh every 30 seconds
    interval(30000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => this.loadAgentData());
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadAgentData(): void {
    // Load statistics
    this.http.get<any>(`${environment.apiUrl}/simple-admin/stats`)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          if (data.success) {
            this.stats = {
              totalApplications: data.stats.totalApplications || 0,
              approvedApplications: 0,
              pendingApplications: 0,
              rejectedApplications: 0
            };
          }
        },
        error: (error) => console.error('Erreur chargement stats:', error)
      });

    // Load applications
    this.http.get<any>(`${environment.apiUrl}/simple-admin/applications`)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          if (data.success && data.applications) {
            this.applications = data.applications.map((app: any[]) => ({
              id: app[0],
              candidateName: `${app[4] || ''} ${app[5] || ''}`.trim() || 'N/A',
              email: app[6] || 'N/A',
              status: app[1] || 'UNKNOWN',
              submissionDate: app[2] || new Date().toISOString(),
              assignedAgent: app[7] || null
            }));
            this.applyFilter();
          }
        },
        error: (error) => console.error('Erreur chargement candidatures:', error)
      });
  }

  refreshData(): void {
    this.loadAgentData();
  }

  filterByStatus(event: any): void {
    this.currentFilter = event.target.value;
    this.applyFilter();
  }

  private applyFilter(): void {
    if (!this.currentFilter) {
      this.filteredApplications = this.applications;
    } else {
      this.filteredApplications = this.applications.filter(
        app => app.status === this.currentFilter
      );
    }
  }

  getStatusLabel(status: string): string {
    const labels: { [key: string]: string } = {
      'APPROVED': 'Approuvée',
      'PENDING': 'En attente',
      'REJECTED': 'Rejetée',
      'UNDER_REVIEW': 'En révision'
    };
    return labels[status] || status;
  }

  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString('fr-FR');
  }

  processApplication(id: number): void {
    window.location.href = `/workflow/${id}`;
  }

  viewDetails(id: number): void {
    window.location.href = `/agent/dossiers`;
  }
}
