import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DossierService } from '../../core/services/dossier.service';
import { AuthService } from '../../core/services/auth.service';
import { Dossier } from '../../core/models/models';

@Component({
  selector: 'app-candidat-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="max-w-6xl mx-auto p-6 animate-fadeIn">
      <div class="mb-8">
        <h1 class="text-3xl font-bold text-gray-900">Mon Tableau de Bord</h1>
        <p class="mt-2 text-gray-600">Suivez l'état de vos candidatures</p>
      </div>

      <!-- Statistiques rapides -->
      <div class="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
        <div class="bg-white rounded-lg shadow p-6 card-hover">
          <div class="flex items-center">
            <div class="p-3 rounded-full bg-blue-100 text-blue-600">
              <i class="fas fa-file-alt text-xl"></i>
            </div>
            <div class="ml-4">
              <p class="text-sm font-medium text-gray-600">Total</p>
              <p class="text-2xl font-bold text-gray-900">{{mesCandidatures.length}}</p>
            </div>
          </div>
        </div>
        
        <div class="bg-white rounded-lg shadow p-6 card-hover">
          <div class="flex items-center">
            <div class="p-3 rounded-full bg-yellow-100 text-yellow-600">
              <i class="fas fa-clock text-xl"></i>
            </div>
            <div class="ml-4">
              <p class="text-sm font-medium text-gray-600">En attente</p>
              <p class="text-2xl font-bold text-gray-900">{{getCountByStatus('UNDER_REVIEW')}}</p>
            </div>
          </div>
        </div>
        
        <div class="bg-white rounded-lg shadow p-6 card-hover">
          <div class="flex items-center">
            <div class="p-3 rounded-full bg-green-100 text-green-600">
              <i class="fas fa-check-circle text-xl"></i>
            </div>
            <div class="ml-4">
              <p class="text-sm font-medium text-gray-600">Approuvées</p>
              <p class="text-2xl font-bold text-gray-900">{{getCountByStatus('APPROVED')}}</p>
            </div>
          </div>
        </div>
        
        <div class="bg-white rounded-lg shadow p-6 card-hover">
          <div class="flex items-center">
            <div class="p-3 rounded-full bg-red-100 text-red-600">
              <i class="fas fa-times-circle text-xl"></i>
            </div>
            <div class="ml-4">
              <p class="text-sm font-medium text-gray-600">Rejetées</p>
              <p class="text-2xl font-bold text-gray-900">{{getCountByStatus('REJECTED')}}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Liste des candidatures -->
      <div class="bg-white rounded-lg shadow overflow-hidden card-hover">
        <div class="px-6 py-4 border-b border-gray-200">
          <h3 class="text-lg font-medium text-gray-900">Mes Candidatures</h3>
        </div>
        
        <div class="overflow-x-auto">
          <table class="table-enhanced min-w-full divide-y divide-gray-200">
            <thead class="bg-gray-50">
              <tr>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">ID</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Institution</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Spécialisation</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Statut</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Date soumission</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
              </tr>
            </thead>
            <tbody class="bg-white divide-y divide-gray-200">
              <tr *ngFor="let candidature of mesCandidatures" class="hover:bg-gray-50">
                <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                  #{{candidature.id}}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  {{candidature.institutionCible || 'N/A'}}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  {{candidature.specialisationDemandee || 'N/A'}}
                </td>
                <td class="px-6 py-4 whitespace-nowrap">
                  <span [class]="getStatusClass(candidature.status)" class="px-2 py-1 text-xs font-medium rounded-full">
                    {{getStatusLabel(candidature.status)}}
                  </span>
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                  {{candidature.dateCreation | date:'short'}}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm font-medium space-x-2">
                  <button (click)="voirDetails(candidature)" class="btn-secondary">
                    Voir détails
                  </button>
                  <button *ngIf="candidature.status === 'APPROVED'" 
                          (click)="telechargerFiche(candidature.id!)" 
                          class="px-3 py-2 rounded-lg text-white bg-green-600 hover:bg-green-700 transition-colors">
                    Fiche d'inscription
                  </button>
                </td>
              </tr>
              <tr *ngIf="mesCandidatures.length === 0">
                <td colspan="6" class="px-6 py-4 text-center text-gray-500">
                  Aucune candidature trouvée
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Modal de détails -->
      <div *ngIf="candidatureSelectionnee" class="fixed inset-0 z-50 overflow-y-auto">
        <div class="flex items-center justify-center min-h-screen px-4">
          <div class="fixed inset-0 bg-black opacity-50" (click)="fermerModal()"></div>
          <div class="bg-white rounded-lg max-w-2xl w-full max-h-screen overflow-y-auto relative z-10 shadow-xl">
            <div class="p-6">
              <div class="flex justify-between items-center mb-6">
                <h3 class="text-xl font-bold">Candidature #{{candidatureSelectionnee.id}}</h3>
                <button (click)="fermerModal()" class="text-gray-400 hover:text-gray-600">
                  <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                  </svg>
                </button>
              </div>
              
              <div class="space-y-4">
                <div>
                  <label class="block text-sm font-medium text-gray-700">Institution cible</label>
                  <p class="mt-1 text-sm text-gray-900">{{candidatureSelectionnee.institutionCible || 'N/A'}}</p>
                </div>
                
                <div>
                  <label class="block text-sm font-medium text-gray-700">Spécialisation demandée</label>
                  <p class="mt-1 text-sm text-gray-900">{{candidatureSelectionnee.specialisationDemandee || 'N/A'}}</p>
                </div>
                
                <div>
                  <label class="block text-sm font-medium text-gray-700">Statut</label>
                  <span [class]="getStatusClass(candidatureSelectionnee.status)" class="inline-block mt-1 px-2 py-1 text-xs font-medium rounded-full">
                    {{getStatusLabel(candidatureSelectionnee.status)}}
                  </span>
                </div>
                
                <div>
                  <label class="block text-sm font-medium text-gray-700">Date de soumission</label>
                  <p class="mt-1 text-sm text-gray-900">{{candidatureSelectionnee.dateCreation | date:'full'}}</p>
                </div>
                
                <div>
                  <label class="block text-sm font-medium text-gray-700">Taux de complétion</label>
                  <p class="mt-1 text-sm text-gray-900">{{candidatureSelectionnee.tauxCompletion || 0}}%</p>
                </div>
                
                <div>
                  <label class="block text-sm font-medium text-gray-700">Documents ({{candidatureSelectionnee.documents?.length || 0}})</label>
                  <div class="mt-2 space-y-2">
                    <div *ngFor="let doc of candidatureSelectionnee.documents" class="flex items-center justify-between p-2 border rounded">
                      <div>
                        <p class="text-sm font-medium">{{doc.nom}}</p>
                        <p class="text-xs text-gray-500">{{doc.type}} - {{doc.taille}} MB</p>
                      </div>
                      <span [class]="getDocumentStatusClass(doc.statut || 'PENDING')" class="px-2 py-1 text-xs rounded">
                        {{doc.statut || 'PENDING'}}
                      </span>
                    </div>
                    <div *ngIf="!candidatureSelectionnee.documents || candidatureSelectionnee.documents.length === 0" class="text-center py-4 text-gray-500">
                      Aucun document trouvé
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `
})
export class CandidatDashboardComponent implements OnInit {
  mesCandidatures: Dossier[] = [];
  candidatureSelectionnee: Dossier | null = null;

  constructor(
    private dossierService: DossierService,
    private authService: AuthService
  ) {}

  ngOnInit() {
    this.chargerMesCandidatures();
  }

  chargerMesCandidatures() {
    // Utiliser l'endpoint my-applications pour récupérer les candidatures du candidat connecté
    this.dossierService.getMyApplications().subscribe({
      next: (dossiers) => {
        console.log('Candidatures récupérées:', dossiers);
        this.mesCandidatures = dossiers;
      },
      error: (error) => {
        console.error('Erreur chargement candidatures:', error);
        this.mesCandidatures = [];
      }
    });
  }

  getCountByStatus(status: string): number {
    return this.mesCandidatures.filter(c => c.status === status).length;
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'UNDER_REVIEW':
      case 'PENDING':
        return 'bg-yellow-100 text-yellow-800';
      case 'MANUAL_REVIEW':
        return 'bg-blue-100 text-blue-800';
      case 'APPROVED':
        return 'bg-green-100 text-green-800';
      case 'REJECTED':
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'UNDER_REVIEW':
        return 'En révision';
      case 'PENDING':
        return 'En attente';
      case 'MANUAL_REVIEW':
        return 'Révision manuelle';
      case 'APPROVED':
        return 'Approuvée';
      case 'REJECTED':
        return 'Rejetée';
      default:
        return status;
    }
  }

  getDocumentStatusClass(status: string): string {
    switch (status) {
      case 'PENDING':
        return 'bg-yellow-100 text-yellow-800';
      case 'APPROVED':
        return 'bg-green-100 text-green-800';
      case 'REJECTED':
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  }

  voirDetails(candidature: Dossier) {
    this.candidatureSelectionnee = candidature;
  }

  fermerModal() {
    this.candidatureSelectionnee = null;
  }

  telechargerFiche(candidatureId: number) {
    this.dossierService.downloadRegistrationForm(candidatureId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `fiche_inscription_${candidatureId}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: (error) => {
        console.error('Erreur téléchargement fiche:', error);
        alert('Erreur lors du téléchargement de la fiche d\'inscription');
      }
    });
  }
}