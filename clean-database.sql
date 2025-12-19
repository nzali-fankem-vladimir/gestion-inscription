-- Script pour nettoyer la base de données
-- Supprimer les documents orphelins
DELETE FROM document WHERE application_id NOT IN (SELECT id FROM candidat);

-- Renommer la table candidat en application si nécessaire
-- ALTER TABLE candidat RENAME TO application;

-- Ou supprimer toutes les données pour un redémarrage propre
-- TRUNCATE TABLE document CASCADE;
-- TRUNCATE TABLE candidat CASCADE;