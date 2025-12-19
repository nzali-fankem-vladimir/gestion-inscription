import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Subject, takeUntil, interval } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { ChartsComponent } from '../../shared/components/charts.component';
import { UniversityHeatmapComponent } from '../../shared/components/university-heatmap.component';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, ChartsComponent, UniversityHeatmapComponent],
  template: `
    <div class="min-h-screen bg-gray-100">
      <!-- Header -->
      <div class="bg-gradient-to-r from-indigo-600 to-purple-600 shadow-lg">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <div class="flex items-center justify-between">
            <div class="text-white">
              <h1 class="text-4xl font-bold">Administration SIGEC</h1>
              <p class="mt-2 text-indigo-100">Tableau de bord administrateur - Contrôle total du système</p>
            </div>
            <div class="flex items-center space-x-3">
              <button (click)="exportData()" 
                      class="inline-flex items-center px-4 py-2 border border-white text-white rounded-md hover:bg-white hover:text-indigo-600 transition-colors">
                <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
                </svg>
                Exporter
              </button>
              <button (click)="refreshData()" 
                      class="inline-flex items-center px-4 py-2 bg-white text-indigo-600 rounded-md hover:bg-indigo-50 transition-colors">
                <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
                </svg>
                Actualiser
              </button>
            </div>
          </div>
        </div>
      </div>

      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        
        <!-- Quick Admin Actions -->
        <div class="mb-8">
          <h2 class="text-2xl font-bold text-gray-900 mb-4">Gestion Système</h2>
          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            <button routerLink="/admin/agents" 
                    class="group bg-white hover:shadow-xl transition-all rounded-lg p-6 border-l-4 border-indigo-500">
              <div class="flex items-center">
                <div class="w-12 h-12 bg-indigo-100 rounded-lg flex items-center justify-center group-hover:scale-110 transition-transform">
                  <svg class="w-6 h-6 text-indigo-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"/>
                  </svg>
                </div>
                <div class="ml-4">
                  <h3 class="text-lg font-semibold text-gray-900">Agents</h3>
                  <p class="text-sm text-gray-600">Gérer les agents</p>
                </div>
              </div>
            </button>

            <button routerLink="/admin/candidats" 
                    class="group bg-white hover:shadow-xl transition-all rounded-lg p-6 border-l-4 border-blue-500">
              <div class="flex items-center">
                <div class="w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center group-hover:scale-110 transition-transform">
                  <svg class="w-6 h-6 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"/>
                  </svg>
                </div>
                <div class="ml-4">
                  <h3 class="text-lg font-semibold text-gray-900">Candidats</h3>
                  <p class="text-sm text-gray-600">Gérer les candidats</p>
                </div>
              </div>
            </button>

            <button routerLink="/admin/dossiers" 
                    class="group bg-white hover:shadow-xl transition-all rounded-lg p-6 border-l-4 border-green-500">
              <div class="flex items-center">
                <div class="w-12 h-12 bg-green-100 rounded-lg flex items-center justify-center group-hover:scale-110 transition-transform">
                  <svg class="w-6 h-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
                  </svg>
                </div>
                <div class="ml-4">
                  <h3 class="text-lg font-semibold text-gray-900">Dossiers</h3>
                  <p class="text-sm text-gray-600">Tous les dossiers</p>
                </div>
              </div>
            </button>

            <button routerLink="/admin/reports" 
                    class="group bg-white hover:shadow-xl transition-all rounded-lg p-6 border-l-4 border-purple-500">
              <div class="flex items-center">
                <div class="w-12 h-12 bg-purple-100 rounded-lg flex items-center justify-center group-hover:scale-110 transition-transform">
                  <svg class="w-6 h-6 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
                  </svg>
                </div>
                <div class="ml-4">
                  <h3 class="text-lg font-semibold text-gray-900">Rapports</h3>
                  <p class="text-sm text-gray-600">Analyses avancées</p>
                </div>
              </div>
            </button>
          </div>
        </div>

        <!-- System Statistics -->
        <div class="mb-8">
          <h2 class="text-2xl font-bold text-gray-900 mb-4">Statistiques Système</h2>
          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-6">
            <div class="bg-white rounded-lg shadow-md p-6 border-t-4 border-purple-500">
              <div class="flex items-center justify-between">
                <div>
                  <p class="text-sm font-medium text-gray-600">Utilisateurs</p>
                  <p class="text-3xl font-bold text-gray-900 mt-2">{{stats.totalUsers}}</p>
                  <p class="text-xs text-gray-500 mt-1">Total système</p>
                </div>
                <div class="w-14 h-14 bg-purple-100 rounded-full flex items-center justify-center">
                  <svg class="w-7 h-7 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"/>
                  </svg>
                </div>
              </div>
            </div>

            <div class="bg-white rounded-lg shadow-md p-6 border-t-4 border-indigo-500">
              <div class="flex items-center justify-between">
                <div>
                  <p class="text-sm font-medium text-gray-600">Agents</p>
                  <p class="text-3xl font-bold text-gray-900 mt-2">{{stats.totalAgents}}</p>
                  <p class="text-xs text-gray-500 mt-1">Actifs</p>
                </div>
                <div class="w-14 h-14 bg-indigo-100 rounded-full flex items-center justify-center">
                  <svg class="w-7 h-7 text-indigo-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/>
                  </svg>
                </div>
              </div>
            </div>

            <div class="bg-white rounded-lg shadow-md p-6 border-t-4 border-blue-500">
              <div class="flex items-center justify-between">
                <div>
                  <p class="text-sm font-medium text-gray-600">Candidatures</p>
                  <p class="text-3xl font-bold text-gray-900 mt-2">{{stats.totalApplications}}</p>
                  <p class="text-xs text-gray-500 mt-1">Total</p>
                </div>
                <div class="w-14 h-14 bg-blue-100 rounded-full flex items-center justify-center">
                  <svg class="w-7 h-7 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
                  </svg>
                </div>
              </div>
            </div>

            <div class="bg-white rounded-lg shadow-md p-6 border-t-4 border-green-500">
              <div class="flex items-center justify-between">
                <div>
                  <p class="text-sm font-medium text-gray-600">Approuvées</p>
                  <p class="text-3xl font-bold text-green-600 mt-2">{{stats.approvedApplications}}</p>
                  <p class="text-xs text-gray-500 mt-1">{{getPercentage(stats.approvedApplications)}}%</p>
                </div>
                <div class="w-14 h-14 bg-green-100 rounded-full flex items-center justify-center">
                  <svg class="w-7 h-7 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
                  </svg>
                </div>
              </div>
            </div>

            <div class="bg-white rounded-lg shadow-md p-6 border-t-4 border-yellow-500">
              <div class="flex items-center justify-between">
                <div>
                  <p class="text-sm font-medium text-gray-600">En attente</p>
                  <p class="text-3xl font-bold text-yellow-600 mt-2">{{stats.pendingApplications}}</p>
                  <p class="text-xs text-gray-500 mt-1">{{getPercentage(stats.pendingApplications)}}%</p>
                </div>
                <div class="w-14 h-14 bg-yellow-100 rounded-full flex items-center justify-center">
                  <svg class="w-7 h-7 text-yellow-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
                  </svg>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Charts and Analytics -->
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-8 mb-8">
          <div class="bg-white rounded-lg shadow-md p-6">
            <h3 class="text-lg font-semibold text-gray-900 mb-4">Graphiques de performance</h3>
            <app-charts></app-charts>
          </div>

          <div class="bg-white rounded-lg shadow-md p-6">
            <h3 class="text-lg font-semibold text-gray-900 mb-4">Carte thermique des universités</h3>
            <app-university-heatmap></app-university-heatmap>
          </div>
        </div>

        <!-- Recent Activity -->
        <div class="bg-white rounded-lg shadow-md overflow-hidden">
          <div class="px-6 py-4 border-b border-gray-200 bg-gray-50">
            <h2 class="text-xl font-semibold text-gray-900">Activité récente du système</h2>
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
                    Agent assigné
                  </th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Date
                  </th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody class="bg-white divide-y divide-gray-200">
                <tr *ngFor="let app of recentApplications" class="hover:bg-gray-50 transition-colors">
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
                    {{app.assignedAgent || 'Non assigné'}}
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {{formatDate(app.submissionDate)}}
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm font-medium">
                    <button (click)="viewApplication(app.id)" 
                            class="text-indigo-600 hover:text-indigo-900 mr-3">
                      Voir
                    </button>
                    <button (click)="manageApplication(app.id)" 
                            class="text-gray-600 hover:text-gray-900">
                      Gérer
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

      </div>
    </div>
  `
})
export class AdminDashboardComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  
  stats = {
    totalUsers: 0,
    totalAgents: 0,
    totalApplications: 0,
    approvedApplications: 0,
    pendingApplications: 0,
    rejectedApplications: 0
  };
  
  recentApplications: any[] = [];

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.loadAdminData();
    
    // Auto-refresh every 30 seconds
    interval(30000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => this.loadAdminData());
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadAdminData(): void {
    // Load statistics
    this.http.get<any>(`${environment.apiUrl}/simple-admin/stats`)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          if (data.success) {
            this.stats = {
              totalUsers: data.stats.totalUsers || 0,
              totalAgents: data.stats.totalAgents || 0,
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
            this.recentApplications = data.applications.slice(0, 15).map((app: any[]) => ({
              id: app[0],
              candidateName: `${app[4] || ''} ${app[5] || ''}`.trim() || 'N/A',
              email: app[6] || 'N/A',
              status: app[1] || 'UNKNOWN',
              submissionDate: app[2] || new Date().toISOString(),
              assignedAgent: app[7] || null
            }));
          }
        },
        error: (error) => console.error('Erreur chargement candidatures:', error)
      });
  }

  refreshData(): void {
    this.loadAdminData();
  }

  exportData(): void {
    // TODO: Implement export functionality
    console.log('Export data');
  }

  getPercentage(value: number): number {
    if (this.stats.totalApplications === 0) return 0;
    return Math.round((value / this.stats.totalApplications) * 100);
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

  viewApplication(id: number): void {
    window.location.href = `/admin/dossiers`;
  }

  manageApplication(id: number): void {
    window.location.href = `/workflow/${id}`;
  }
}
