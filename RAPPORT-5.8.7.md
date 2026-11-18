# WorshipFlow 5.8.7 — Intégration AppForge V3

Date : 18 novembre 2026  
Branche : `work/worshipflow-v3-cloud-20261117`  
PR WorshipFlow : https://github.com/RMLala/Who/pull/9  
SDK source : https://github.com/RMLala/appforg/blob/main/cloud/license-worker/client/appforge-cloud-sync.js

## État précédent

- WorshipFlow vérifiait une preuve de licence ECDSA et gardait ses données métier dans IndexedDB.
- Le client de synchronisation avait un ancien parcours AppForge Cloud et des routes séparées de celles du Worker central.
- La synchronisation locale/Cloud gérait les snapshots, révisions, conflits et copies de récupération.
- Le dépôt AppForge contient le Worker central et le SDK officiel.

## Changements poussés dans la PR

- Ajout de la copie exacte du SDK officiel AppForge dans `www/appforge-cloud-sync.js`.
- Configuration de WorshipFlow vers `https://appforge-license-api.appforge-licences-mg.workers.dev`.
- L’activation passe par le SDK, sans compte Cloud, e-mail, mot de passe ou organisation. Le SDK renouvelle le jeton par `POST /v1/cloud/token` et utilise `GET/PUT /v1/cloud/snapshot`.
- Le SDK est relié aux données de WorshipFlow par des fonctions de lecture/restauration de snapshot. Les médias audio locaux sont exclus.
- Les données et changements locaux restent disponibles hors ligne; la reprise réseau, le versionnage et la gestion de conflit sont conservés.
- « Connecté » exige une synchronisation réussie et un jeton non expiré. Aucun jeton n’est affiché.
- Aucune clé serveur ni `CLOUD_TOKEN_SECRET` n’est inclus dans WorshipFlow. Aucune route ou méthode SDK n’a été modifiée.

## Tests client

Workflow WorshipFlow #99 : https://github.com/RMLala/Who/actions/runs/44369757026

- Tests Node et test du contrat SDK : réussite, appels Worker simulés.
- Smoke test navigateur : réussite en viewport 390 px et 1280 px.
- Builds Debug API 23/29 : réussite.
- Builds Release signés API 23/29 et vérification des signatures : réussite.

Ces contrôles ne prouvent pas l’activation d’une licence réelle ni la synchronisation D1.

## Contrat HTTP officiel

La source officielle `RMLala/appforg/cloud/license-worker/src/index.js` (SHA `76f2be77705073874570748ec3b2a326cf3db7ff`) route explicitement `GET` et `PUT` sur `/v1/cloud/snapshot`. Le SDK officiel (SHA `9a06175c9ca610f8c99474fa165abc311de414c5`) :

- envoie `GET /v1/cloud/snapshot?resource=default` avec `Authorization: Bearer <cloudToken>` ;
- écrit par `PUT` au même chemin avec `{ "expectedRevision": n, "snapshot": {...} }` ;
- utilise une révision compare-and-swap; la source renvoie HTTP 409 et l’instantané courant en cas de conflit.

Le Worker source valide le jeton avant l’accès D1. WorshipFlow conserve ce contrat.

## Déploiement central constaté

Le workflow central [Deploy AppForge license and cloud Worker #4](https://github.com/RMLala/appforg/actions/runs/44358161206) a terminé avec succès le 18 novembre 2026. Son journal montre :

- Wrangler a appliqué 7 migrations à la base liée `appforge-licenses`, ID `a77521ea-dc78-4b80-b495-405127e275e7`.
- Wrangler a déployé le Worker `appforge-license-api` et a indiqué cette URL : `https://appforge-license-api.lalainaadclick.workers.dev`.
- Le workflow a ensuite vérifié `https://appforge-license-api.appforge-licences-mg.workers.dev` pour seulement `/health` et `/v1/config`; il n’a pas vérifié le snapshot ni confirmé que la cible déployée était l’URL qu’il venait d’annoncer.

Le fichier `cloud/license-worker/wrangler.toml` fixe le nom `appforge-license-api` et l’ID D1 ci-dessus. Le déploiement a donc utilisé la configuration Wrangler du dépôt, mais la cible Workers.dev émise par Wrangler ne correspond pas à l’URL centrale configurée dans WorshipFlow.

## Vérifications HTTP après ce déploiement

Au 18 novembre 2026 :

**Worker réellement annoncé par Wrangler — `appforge-license-api.lalainaadclick.workers.dev`**
- `GET /health` : HTTP 200.
- `GET /v1/cloud/snapshot?resource=default` sans jeton : HTTP 401, `Jeton Cloud invalide ou expiré.`.
- Préflight OPTIONS demandant PUT : HTTP 204, CORS inclut `GET, POST, PUT, PATCH, DELETE, OPTIONS`.

**URL centrale configurée dans WorshipFlow — `appforge-license-api.appforge-licences-mg.workers.dev`**
- `GET /health` et `GET /v1/config` : HTTP 200.
- `GET /v1/cloud/snapshot?resource=default` sans jeton : HTTP 404, `Route inconnue.`.
- Préflight OPTIONS demandant PUT : HTTP 204, CORS annonce `GET, POST, OPTIONS`, sans PUT.

Cela explique la divergence : le Worker source a bien été déployé avec les routes attendues, mais Wrangler l’a publié sur un autre sous-domaine de compte. Le workflow a vérifié une ancienne cible et a ainsi pu réussir malgré le mauvais sous-domaine. La cible effective dépend des secrets `CLOUDFLARE_ACCOUNT_ID` et `CLOUDFLARE_API_TOKEN`; leurs valeurs ne sont pas visibles ici. Il faut les configurer pour le compte propriétaire du sous-domaine `appforge-licences-mg`, puis confirmer que l’ID D1 lié correspond à la base à préserver. Ne pas copier ces secrets dans le dépôt ou le chat.

Le workflow #4 annonce l’application de 7 migrations sur son D1 lié. Comme la cible d’account/sous-domaine ne correspond pas à l’URL attendue, cette base ne doit pas être considérée comme la base centrale voulue sans confirmation de son propriétaire et de son ID. Aucun effacement D1 n’a été exécuté par cette intégration.

## État et validations restantes

La PR WorshipFlow #9 reste ouverte. La synchronisation n’est pas déclarée opérationnelle.

Après correction des secrets de déploiement et vérification du D1 cible, il reste à tester avec une licence de test valide :
1. activation de licence et émission du jeton ;
2. GET snapshot authentifié ;
3. PUT d’un instantané, puis GET pour vérifier les données et la révision dans D1 ;
4. passage hors ligne et reprise après reconnexion.

Ces tests authentifiés de production n’ont pas été exécutés; aucun code de licence valide n’a été fourni. Aucun Worker séparé n’a été créé.

## Liens

- PR WorshipFlow #9 : https://github.com/RMLala/Who/pull/9
- Déploiement Worker #4 : https://github.com/RMLala/appforg/actions/runs/44358161206
- Tests et APK WorshipFlow #99 : https://github.com/RMLala/Who/actions/runs/44369757026
