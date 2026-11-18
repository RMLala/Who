# WorshipFlow 5.8.7 — Intégration AppForge V3

Date : 18 novembre 2026  
Branche : `work/worshipflow-v3-cloud-20261117`  
PR WorshipFlow : https://github.com/RMLala/Who/pull/9  
SDK source : https://github.com/RMLala/appforg/blob/main/cloud/license-worker/client/appforge-cloud-sync.js

## Configuration WorshipFlow

- URL de base licence et Cloud : `https://appforge-license-api.lalainaadclick.workers.dev`.
- Parcours licence sans compte Cloud, e-mail, mot de passe ni organisation.
- Renouvellement du jeton par `POST /v1/cloud/token`.
- Synchronisation avec `GET/PUT /v1/cloud/snapshot); aucune méthode ni route SDK n’a été changée.
- Aucune modification des secrets ou de la configuration D1.
- Aucune clé serveur ni `CLOUD_TOKEN_SECRET` dans l’application.
- « Connecté » reste conditionné à une synchronisation réussie avec un jeton valide.

## Contrat HTTP officiel

La source `RMLala/appforg/cloud/license-worker/src/index.js` (SHA `76f2be77705073874570748ec3b2a326cf3db7ff`) route `GET` et `PUT` sur `/v1/cloud/snapshot`. Le SDK officiel (SHA `9a06175c9ca610f8c99474fa165abc311de414c5`) :

- lit `GET /v1/cloud/snapshot?resource=default` avec `Authorization: Bearer <cloudToken>` ;
- écrit par `PUT` au même chemin avec `{ "expectedRevision": n, "snapshot": {...} }` ;
- utilise compare-and-swap; la source renvoie HTTP 409 et l’instantané courant en cas de conflit.

## Déploiement central et contrôles HTTP

Le workflow [AppForge Worker #4](https://github.com/RMLala/appforg/actions/runs/44358161206) a déployé `appforge-license-api` sur `https://appforge-license-api.lalainaadclick.workers.dev`. Le journal signale 7 migrations appliquées à la base Wrangler `appforge-licenses`, ID `a77521ea-dc78-4b80-b495-405127e275e7`. Aucune modification de D1 n’a été faite par cette PR.

Contrôles HTTP du 18 novembre 2026 :

- Sur `https://appforge-license-api.lalainaadclick.workers.dev`, `GET /health` répond 200.
- GET snapshot sans jeton répond HTTP 401 (`Jeton Cloud invalide ou expiré.`).
- Le préflight OPTIONS pour PUT répond 204 et autorise `GET, POST, PUT, PATCH, DELETE, OPTIONS`.
- L’ancienne URL `https://appforge-license-api.appforge-licences-mg.workers.dev` répondait 404 pour le snapshot et son préflight ne listait pas PUT; elle n’est plus utilisée par WorshipFlow après cette correction.

Ces résultats confirment que la route est reconnue sur le Worker central cible. Ils ne prouvent pas l’accès authentifié ni l’écriture/lecture D1.

## Tests

Workflow WorshipFlow #99, avant ce changement d’URL : https://github.com/RMLala/Who/actions/runs/44369757026

- Tests Node et contrat SDK simulé : réussite.
- Smoke test navigateur 390 px et 1280 px : réussite.
- Builds Debug API 23/29 : réussite.
- Builds Release signés API 23/29 et vérification des signatures : réussite.

Le nouveau commit relance la CI avec la nouvelle URL et les assertions de configuration mises à jour.

## État et validations restantes

La PR #9 reste ouverte. La synchronisation n’est pas déclarée opérationnelle.

À vérifier avec une licence de test valide :

1. activation de la licence ;
2. obtention du jeton Cloud ;
3. lecture authentifiée d’un instantané ;
4. écriture authentifiée puis relecture et vérification de la révision dans D1 ;
5. fonctionnement hors ligne et reprise à la reconnexion.

Aucun test authentifié de production n’a encore été exécuté; aucun code de licence de test valide n’a été fourni.