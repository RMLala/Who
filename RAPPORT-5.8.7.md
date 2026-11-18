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

Workflow WorshipFlow : https://github.com/RMLala/Who/actions/runs/44367395819

- Tests Node du dépôt : réussite, incluant activation simulée, jeton, upload d’un snapshot, état hors ligne et absence de jeton en clair par défaut.
- Smoke test navigateur : réussite en viewport 390 px et 1280 px.
- Build Debug API 23 et API 29 : réussite.
- Build Release signé et vérification des signatures : en cours au moment de ce rapport.
- Workflows de conformité AppForge V3 : réussis.

Les appels d’activation et de synchronisation automatisés utilisent des réponses Worker simulées. Ils ne prouvent pas l’activation MVola ni la synchronisation D1 en production.

## Contrat HTTP du Worker central et du SDK

Le code source actuel de `RMLala/appforg/cloud/license-worker/src/index.js` route `GET` et `PUT` sur le chemin exact `/v1/cloud/snapshot`. Le SDK officiel `client/appforge-cloud-sync.js` utilise le même chemin et :

- envoie `GET /v1/cloud/snapshot?resource=default` par défaut ;
- envoie le jeton de synchronisation dans `Authorization: Bearer <cloudToken>` ;
- écrit avec `PUT /v1/cloud/snapshot?resource=default` et le JSON `{ "expectedRevision": 0, "snapshot": {} }` ;
- attend une révision compare-and-swap; le Worker renvoie le snapshot courant avec HTTP 409 en cas de conflit.

Le Worker exige ce jeton avant de lire ou d’écrire D1.

## Vérification de l’instance de production

- `GET /health` retourne `ok:true`.
- `GET /v1/config` retourne la clé publique attendue et `protocol: appforge-license-v1`.
- Une requête navigateur `GET /v1/cloud/snapshot?resource=default` sans jeton a retourné `{"error":"Route inconnue."}`. Comme elle ne comportait aucun jeton Bearer, ce résultat ne valide pas le chemin authentifié ni l’accès D1. Il diffère cependant du code source actuel, qui devrait rejeter l’absence de jeton avec HTTP 401. Une requête authentifiée exige une licence de test et n’a pas encore été réalisée.

La PR reste ouverte; aucune synchronisation de production n’est déclarée réussie.

## Restant à valider

- Avec un jeton Cloud obtenu par une licence de test, appeler le GET authentifié puis écrire/lire un snapshot D1 et vérifier la révision retournée. Comparer le résultat au code source avant toute correction côté application.
- Activer une licence de test autorisée et valider l’émission/renouvellement du jeton ainsi qu’un snapshot D1.
- Tester deux appareils autorisés, révocation/expiration, conflits réels, perte et reprise réseau, absence de doublons et isolation entre licences.
- La vérification MVola réelle nécessite une activation de test; aucun code de licence n’a été fourni pour ce travail.
