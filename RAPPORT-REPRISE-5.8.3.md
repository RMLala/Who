# Rapport — reprise WorshipFlow 5.8.3

## Diagnostic confirmé

- La branche `upgrade/android-release-api23-api29` contient encore les sources 5.7.14.
- `main` contient les sources 5.8.3, mais son exécution GitHub Actions s’arrête à `setup-android` : l’action tente d’installer l’ancien package SDK `tools`, introuvable. Aucun APK 5.8.3 n’a été produit par cette exécution.
- L’intégration de `main` dans la branche de travail par la connexion GitHub a été refusée (403 `Resource not accessible by integration`). `main` et la branche distante restent inchangées.

## Préparation faite dans cette archive

- Reprise du projet complet 5.8.3 (versionName 5.8.3, package `com.worshipflow.app`, minSdk 23/29).
- Workflow configuré pour installer explicitement `platform-tools`, `platforms;android-34` et `build-tools;34.0.0`, sans l’ancien package `tools`.
- Builds Debug API 23/29, Releases signés API 23/29, vérification des signatures et téléchargement des quatre APK comme artefacts distincts.
- Secrets du workflow alignés sur ceux déjà configurés dans l’organisation : `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
- `versionName` reste 5.8.3; `versionCode` Debug reste 30 et le Release reçoit un code automatiquement supérieur à 30.
- Correctif audio : pause/reprise et déplacement restent désactivés pendant la préparation Android; les sauvegardes incluant l’audio embarquent aussi les fichiers liés au téléphone; les anciennes sauvegardes contenant seulement un URI devenu invalide restaurent le chant comme audio manquant.

## Contrôles

- JavaScript intégré : `node --check` réussi.
- `package.json`, `capacitor.config.json` et manifeste Android : valides.
- Archive ZIP : vérifiée.
- Je n’ai pas pu pousser cette archive sur GitHub ni démarrer le build distant : l’intégration GitHub a refusé l’écriture avec une erreur 403. La compilation réelle reste à valider après application et push depuis GitHub Desktop.

## Synchronisation Cloudflare et licences

Le client WorshipFlow 5.8.3 ne contient pas encore l’intégration AppForge Cloud ni l’écran de licence. Ce paquet ne les ajoute pas et ne prétend pas qu’elles sont actives dans l’APK.
