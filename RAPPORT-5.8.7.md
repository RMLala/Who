# WorshipFlow 5.8.7 — Intégration AppForge V3

Date : 18 novembre 2026  
Branche : `work/worshipflow-v3-cloud-20261117`  
PR WorshipFlow : https://github.com/RMLala/Who/pull/9  
SDK source : https://github.com/RMLala/appforg/blob/main/cloud/license-worker/client/appforge-cloud-sync.js

## Changements WorshipFlow

- SDK officiel AppForge copié dans `www/appforge-cloud-sync.js`.
- URL configurée : `https://appforge-license-api.appforge-licences-mg.workers.dev`.
- Activation sans compte Cloud, e-mail, mot de passe ni organisation.
- Jeton renouvelé par `POST /v1/cloud/token); snapshots par `GET/PUT /v1/cloud/snapshot`.
- Données locales conservées hors ligne; aucune clé serveur ni `CLOUD_TOKEN_SECRET` dans l’application.
- « Connecté » exige une synchronisation réussie avec jeton valide. Aucune route/méthode SDK n’a été modifiée.

## Contrat HTTP officiel

La source officielle `RMLala/appforg/cloud/license-worker/src/index.js` (SHA `76f2be77705073874570748ec3b2a326cf3db7ff`) route explicitement `GET` et `PUT` sur `/v1/cloud/snapshot`. Le SDK officiel (SHA `9a06175c9ca610f8c99474fa165abc311de414c5`) :

- lit `GET /v1/cloud/snapshot?resource=default` avec `Authorization: Bearer <cloudToken>` ;
- écrit par `PUT` au même chemin avec `{ "expectedRevision": n, "snapshot": {...} }` ;
- utilise compare-and-swap; la source renvoie HTTP 409 et l’instantané courant en cas de conflit.

WorshipFlow conserve exactement ce contrat.

## Déploiement et Cloudflare

Le workflow [Deploy AppForge license and cloud Worker #4](https://github.com/RMLala/appforg/actions/runs/44358161206) a réussi. Wrangler a déployé `appforge-license-api` sur `https://appforge-license-api.lalainaadclick.workers.dev` et appliqué 7 migrations à `appforge-licenses`, D1 ID `a77521ea-dc78-4b80-b495-405127e275e7`.

La capture Cloudflare et le tableau de bord connecté confirment que le compte `Lalainaadclick` a le sous-domaine `lalainaadclick.workers.dev`, ce Worker et cette base D1. L’ID correspond à `cloud/license-worker/wrangler.toml`. L’utilisateur confirme que ses secrets et la base sont déjà correctement configurés; aucune demande de les ressaisir n’est nécessaire.

Le workflow #4 a vérifié l’ancienne URL `appforge-license-api.appforge-licences-mg.workers.dev` pour seulement `/health` et `/v1/config`, sans vérifier le snapshot ou confirmer le domaine annoncé par Wrangler.

## Vérifications HTTP

Au 18 novembre 2026 :

**Déploiement du compte `Lalainaadclick`**
- `GET /health` : HTTP 200.
- GET snapshot sans jeton : HTTP 401, `Jeton Cloud invalide ou expiré.`.
- Préflight PUT : HTTP 204, CORS autorise `GET, POST, PUT, PATCH, DELETE, OPTIONS`.

**URL configurée dans WorshipFlow (`appforge-licences-mg`)**
- `GET /health` et `GET /v1/config` : HTTP 200.
- GET snapshot sans jeton : HTTP 404, `Route inconnue.`.
- Préflight PUT : HTTP 204, CORS annonce `GET, POST, OPTIONS`, sans PUT.

La source est donc bien déployée dans le compte visible, mais le sous-domaine Workers.dev de ce compte ne correspond pas à l’URL utilisée par WorshipFlow. Le tableau de bord Cloudflare précise que changer ce sous-domaine déplace **tous les Workers existants et nouveaux** du compte. Un seul Worker apparaît dans le compte consulté. Ce réglage n’a pas été modifié; il faut éviter de le changer sans confirmer la disponibilité/propriété de `appforge-licences-mg) et son effet sur les autres URLs.

## Tests et état

Workflow WorshipFlow #99 : https://github.com/RMLala/Who/actions/runs/44369757026

- Tests Node et contrat SDK simulé : réussite.
- Smoke navigateur 390 px et 1280 px : réussite.
- Builds Debug API 23/29 : réussite.
- Builds Release signés API 23/29 et signatures : réussite.

La PR #9 reste ouverte. Aucune activation, obtention de jeton ni lecture/écriture D1 authentifiée en production n’a été validée; la synchro n’est pas déclarée opérationnelle. Aucun code de licence de test valide n’a été fourni.

Avant la validation finale, il faut aligner le domaine du Worker avec `appforge-license-api.appforge-licences-mg.workers.dev` sans modifier le contrat SDK, puis tester avec une licence valide l’activation, le jeton, GET authentifié, PUT et relecture D1, et reprise hors ligne.

## Liens

- PR WorshipFlow #9 : https://github.com/RMLala/Who/pull/9
- Déploiement Worker #4 : https://github.com/RMLala/appforg/actions/runs/44358161206
- Tests et APK WorshipFlow #99 : https://github.com/RMLala/Who/actions/runs/44369757026
