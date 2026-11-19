# Rapport WorshipFlow 5.8.8 — AppForge V3

Date : 19 novembre 2026  
Branche : `work/worshipflow-v3-cloud-20261117`  
PR #9 (laissée ouverte) : https://github.com/RMLala/Who/pull/9

## Changements

- URL par défaut, unique pour licence et Cloud : `https://appforge-license-api.lalainaadclick.workers.dev`. Elle est fixée dans l’application et n’est pas redemandée au lancement.
- Clé publique de vérification alignée sur la réponse actuelle de `GET /v1/config`. La configuration publique observée le 19 novembre renvoie le protocole `appforge-license-v1`.
- À l’initialisation, les anciennes préférences de connexion Cloud (URL Worker, organisation et e-mail) sont retirées du seul enregistrement AppForge Cloud et sauvegardées ainsi nettoyées. Les tables de données métier ne sont pas touchées.
- SDK client remplacé par la version officielle actuelle du dépôt AppForge, SHA `b3518f016f859645ce987c088dd1b75a6777305f`. Le contrat reste celui du Worker : activation, statut, renouvellement du jeton et GET/PUT de l’instantané avec Bearer.
- Correction de l’éligibilité Cloud : un essai signé non expiré peut obtenir et renouveler son jeton Cloud pendant sa période serveur de 72 heures. À l’expiration, l’accès Cloud est bloqué et les données métier locales sont conservées.
- Après activation d’une licence payée, le jeton retourné par le Worker est repris automatiquement et la synchronisation redémarre; les conflits existants restent soumis au choix de l’utilisateur.
- La révocation efface l’état de session/licence Cloud local et conserve les données métier hors ligne.
- Version applicative portée à 5.8.8; versionCode Android par défaut 34.

## Contrôles effectués

Requêtes HTTP réelles au Worker central :

- `GET /v1/config` : HTTP 200; protocole et clé publique présents.
- `GET /v1/cloud/snapshot?resource=default` sans jeton : HTTP 401 (« Jeton Cloud invalide ou expiré. »).
- Préflight `OPTIONS` pour `PUT` sur la route snapshot : HTTP 204; CORS autorise PUT.

Ces requêtes ne prouvent pas une synchronisation D1 authentifiée.

Contrôles source :

- L’application utilise le produit `WORSHIPFLOW` (le SDK normalise l’identifiant en majuscules).
- La route du Worker et le SDK officiel sont GET/PUT `/v1/cloud/snapshot?resource=default`; aucun compte Cloud client n’est demandé.
- `GET /v1/config` n’expose pas les produits D1. L’identifiant enregistré dans D1 n’a donc pas été vérifié directement; il reste à confirmer auprès d’une licence de test valide.
- Aucun secret administrateur, clé privée ou `CLOUD_TOKEN_SECRET` ajouté à l’application.

## CI et fichiers téléchargeables

- `npm test` exécuté localement après la correction : 18 réussites, 0 échec. Les scénarios activation/statut/jeton/snapshot, essai valide/expiré et activation payée avec conflit sont simulés; ils ne constituent pas une preuve D1 de production.
- Le smoke navigateur local n’a pas démarré : l’exécutable Chromium de Playwright n’est pas installé dans cet environnement. Le workflow Android PR vérifie le navigateur et les quatre variantes APK; le prochain run doit confirmer cette révision.
- Le workflow Android #130 a d’abord échoué sur les miroirs Ubuntu (HTTP 503); sa relance a révélé un droit Cloud manquant dans le fixture UI du test navigateur. Le fixture est corrigé; un nouveau run PR est nécessaire pour confirmer le smoke et les builds.
- Workflow Android #127 réussi : tests frontend/AppForge, smoke navigateur 390/1280 px, builds Debug API 23/29 et Release signés API 23/29; vérification des signatures réussie : https://github.com/RMLala/Who/actions/runs/44501804887
- Scans AppForge #112 et central compliance #105 : réussis.
- Artefacts téléchargeables (ZIP contenant APK) :
  - Debug API 23 + API 29 : https://api.github.com/repos/RMLala/Who/actions/artifacts/14517268141/zip
  - Release signé API 23 : https://api.github.com/repos/RMLala/Who/actions/artifacts/14518206532/zip
  - Release signé API 29 : https://api.github.com/repos/RMLala/Who/actions/artifacts/14517024936/zip

Ces artefacts valident la compilation et la signature, pas la synchronisation D1 en production.
## À faire avant de déclarer la synchronisation opérationnelle

Avec une licence de test valide et le bon produit D1 :

1. activation et vérification du statut;
2. obtention puis renouvellement du jeton Cloud;
3. écriture d’un instantané via PUT, lecture via GET et preuve de révision dans D1;
4. révocation et refus d’accès;
5. mode hors ligne, données locales intactes, reprise réseau sans doublon ni perte.

Aucune licence de test valide n’est disponible pour effectuer ces opérations authentifiées. La synchronisation réelle reste donc non confirmée et la PR #9 reste ouverte.
