# WorshipFlow 5.8.7 — Cloud automatique AppForge V3

Date : 18 novembre 2026  
Branche : `work/worshipflow-v3-cloud-20261117`  
PR application : https://github.com/RMLala/Who/pull/9  
PR Worker licence/Cloud : https://github.com/RMLala/appforg/pull/10

## État avant cette modification

- L’application savait activer et vérifier une licence signée ECDSA.
- Les données restaient disponibles localement et la synchronisation snapshot gérait les révisions, conflits et copies de récupération.
- L’accès Cloud utilisait encore un formulaire e-mail/mot de passe puis un choix d’organisation. Ce parcours ne respectait pas la règle V3 sans compte client.
- Le Worker de synchronisation Cloud n’était pas versionné dans les dépôts consultés.

## Modifications poussées

- Retrait du formulaire Cloud, de la sélection d’organisation et de l’ancienne session utilisateur.
- Après vérification d’une licence payante, l’application demande automatiquement un jeton court au Worker Cloud, lié à la licence et à l’installation. Le renouvellement revalide la licence.
- L’application transmet la preuve de licence et l’installation au Worker pour chaque lecture/écriture Cloud. Le statut « connecté » n’apparaît qu’avec un jeton valide.
- Le jeton Cloud reste uniquement en mémoire; il n’est pas écrit dans IndexedDB. À la reprise, il est demandé de nouveau au Worker.
- L’essai gratuit n’active pas le Cloud. Les changements hors ligne restent sur l’appareil; l’application retente après reconnexion et selon le cycle existant. Les conflits utilisent des révisions compare-and-swap et conservent la copie locale.
- Ajout d’un Worker Cloudflare + D1 dédié avec limites de taille/quota, isolement par licence/application et vérification serveur des droits/appareils à chaque accès.
- Le statut du Worker licence distingue maintenant essai et licence payante avec le droit Cloud réel.
- Version applicative portée à 5.8.7 et versionCode Android à 34.

## Vérifications

- Vérifications syntaxiques des scripts HTML : réussies.
- Les scans de conformité V3 GitHub ont réussi sur les derniers commits consultés.
- Tests du Worker : 10 scénarios avec Worker/D1 simulés; ils ne sont pas des tests sur l’instance Cloudflare de production.
- CI WorshipFlow 5.8.7 : tests Node, navigateur mobile/bureau et builds Debug/Release sont lancés par GitHub Actions; les résultats finaux sont liés à la PR.
- Aucun APK de téléphone réel ni test de bout en bout MVola/Cloudflare n’a été exécuté ici.

## Reste à faire avant d’avoir le Cloud actif en production

Le PR Worker #10 prépare le code, mais ne déploie pas Cloudflare. Il faut créer la base D1, remplacer l’identifiant d’exemple dans `wrangler.toml`, configurer `CLOUD_TOKEN_SECRET`, appliquer la migration et déployer le Worker. Le Worker licence mis à jour doit aussi être déployé pour fournir les droits `kind/cloud/licenseId`.

Jusqu’à ces déploiements, l’écran doit signaler une erreur ou une connexion en attente; il ne doit pas indiquer que le Cloud est connecté. Les tests réels de validation MVola, expiration/révocation, deux appareils, reconnexion réseau, absence de doublons et isolation restent à exécuter contre les Workers et la D1 déployés.
