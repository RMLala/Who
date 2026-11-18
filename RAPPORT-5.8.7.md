# WorshipFlow 5.8.7 — Intégration AppForge V3

Date : 18 novembre 2026  
Branche : `work/worshipflow-v3-cloud-20261117`  
PR WorshipFlow : https://github.com/RMLala/Who/pull/9  
SDK source : https://github.com/RMLala/appforg/blob/main/cloud/license-worker/client/appforge-cloud-sync.js

## État précédent

- WorshipFlow vérifiait une preuve de licence ECDSA et gardait ses données métier dans IndexedDB.
- Le client de synchronisation avait un ancien parcours AppForge Cloud et des routes séparées de celles du Worker central.
- La synchronisation locale/Cloud gérait les snapshots, révisions, conflits et copies de récupération.
- Le Worker central de licences est déployé par AppForge; le dépôt fournit son SDK officiel.

## Changements poussés dans la PR

- Ajout de la copie exacte du SDK officiel AppForge dans `www/appforge-cloud-sync.js`.
- Remplacement du Worker Cloud distinct par le Worker central `https://appforge-license-api.appforge-licences-mg.workers.dev`.
- L’activation passe par le SDK, sans compte Cloud, e-mail, mot de passe ou organisation. Le SDK renouvelle le jeton par `POST /v1/cloud/token` et synchronise les instantanés avec `GET/PUT /v1/cloud/snapshot`.
- Le SDK est relié aux données de WorshipFlow par des fonctions de lecture/restauration de snapshot. Les médias audio locaux sont exclus.
- Les données et changements locaux restent disponibles hors ligne; la reprise réseau, le versionnage et la gestion de conflit sont conservés.
- « Connecté » exige une synchronisation réussie et un jeton non expiré. Aucun jeton n’est affiché.
- Sans coffre Android activé, les jetons restent en mémoire; seules les métadonnées de révision sont conservées en clair. Avec le verrouillage Android activé, l’état SDK est chiffré par le coffre natif existant.
- Aucune clé serveur ni `CLOUD_TOKEN_SECRET` n’est inclus dans WorshipFlow.
- L’ancienne PR Worker séparée #10 a été fermée comme obsolète et ne doit pas être fusionnée ni déployée.

## Tests exécutés

Workflow WorshipFlow : https://github.com/RMLala/Who/actions/runs/44369757026

- Tests Node du dépôt et test de contrat SDK : réussite; les appels Worker sont simulés.
- Smoke test navigateur : réussite en viewport 390 px et 1280 px.
- Build Debug API 23 et API 29 : réussite.
- Build Release signé API 23/29 et vérification des signatures : réussite.
- Workflows de conformité AppForge V3 : réussis.

Ces contrôles ne prouvent pas l’activation d’une licence réelle ni la synchronisation D1 de production.

## Contrat HTTP de la source officielle et du SDK

La source officielle `RMLala/appforg/cloud/license-worker/src/index.js` (version `76f2be77705073874570748ec3b2a326cf3db7ff`) route explicitement `GET` et `PUT` sur `/v1/cloud/snapshot`. Le SDK officiel (version `9a06175c9ca610f8c99474fa165abc311de414c5`) :

- envoie `GET /v1/cloud/snapshot?resource=default` par défaut ;
- envoie le jeton dans `Authorization: Bearer <cloudToken>` ;
- écrit par `PUT` au même chemin avec `{ "expectedRevision": n, "snapshot": {...} }` ;
- utilise une révision compare-and-swap; la source renvoie le snapshot courant avec HTTP 409 en cas de conflit.

Le Worker source valide le jeton avant d’accéder à D1. Le client WorshipFlow conserve ces méthodes et routes.

## Comparaison du Worker déployé

URL contrôlée : `https://appforge-license-api.appforge-licences-mg.workers.dev`, identique à celle configurée dans WorshipFlow.

Constats HTTP du 18 novembre 2026 :

- `GET /health` : HTTP 200, `{"ok":true,"service":"AppForge License API",...}`.
- `GET /v1/config` : HTTP 200, `protocol: appforge-license-v1`.
- `GET /v1/cloud/snapshot?resource=default` sans jeton : HTTP 404, `{"error":"Route inconnue."}`.
- `OPTIONS /v1/cloud/snapshot?resource=default` avec préflight `PUT` : HTTP 204, mais `Access-Control-Allow-Methods: GET, POST, OPTIONS`, sans `PUT`.

La source officielle devrait reconnaître le GET et répondre HTTP 401 lorsque le jeton manque; ses en-têtes CORS déclarent aussi PUT. L’instance répond donc bien sur l’URL centrale, mais son comportement déployé est incompatible avec la source officielle observée. Cela indique vraisemblablement que le Worker actuellement publié n’est pas à cette version de la source (inférence fondée sur les réponses HTTP et le code source). Aucun jeton ni aucune requête d’écriture anonyme n’a été envoyé.

La PR reste ouverte. Aucune synchronisation D1 n’est annoncée comme opérationnelle.

## Restant à valider

- AppForge doit vérifier et aligner le déploiement du Worker central sur la source officielle; aucun Worker distinct et aucune route alternative ne sont créés côté WorshipFlow.
- Ensuite, avec une licence de test valide : activation, émission du jeton, GET authentifié, PUT d’un instantané et GET de vérification via D1, puis reprise hors ligne/réseau.
- Tester deux appareils autorisés, révocation/expiration, conflits réels, absence de doublons et isolation entre licences.
- Aucun code de licence de test valide n’a été fourni; les étapes production ci-dessus restent non exécutées.

## Workflow de validation du client

Le workflow #99 a réussi le 18 novembre 2026 : tests, smoke navigateur, builds Debug API 23/29, builds Release signés API 23/29 et vérification de signatures. Les tests d’activation, jeton et snapshots restent simulés.

https://github.com/RMLala/Who/actions/runs/44369757026
