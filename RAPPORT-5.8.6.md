# Rapport WorshipFlow 5.8.6

Date : 18 novembre 2026
Branche : `work/worshipflow-v3-cloud-20261117`
PR : #9 vers `main` (ouverte)

## Correction de la sécurité

- Le verrouillage PIN reste facultatif et désactivé sur une nouvelle installation.
- Les réglages présentent deux choix distincts : verrouillage PIN et biométrie.
- Activer le verrouillage crée un PIN de secours à quatre chiffres sans activer la biométrie automatiquement.
- Si la biométrie est activée séparément, Android la propose d’abord; le PIN reste disponible en secours. Si la biométrie est désactivée, seul le PIN est demandé quand le verrouillage reste actif.
- Désactiver la biométrie conserve le PIN. Désactiver le verrouillage déchiffre d’abord les données locales, puis retire la configuration et sa clé.
- Les installations 5.8.5 sans choix d’activation explicite retirent le verrouillage forcé après une dernière authentification nécessaire pour accéder aux données chiffrées.

## Vérifications

Workflow Android [44235030585](https://github.com/RMLala/Who/actions/runs/44235030585), code applicatif `4c2b7aa` : succès.

- 16 tests Node : réussis.
- Interface mobile et bureau : démarrage sans verrou imposé; activation du PIN, activation/désactivation biométrique, désactivation du verrouillage : réussis.
- APK Debug API 23/29 : compilés.
- APK Release API 23/29 : compilés et signatures vérifiées.

Le lancement et l’authentification biométrique doivent encore être confirmés sur un téléphone Android réel.

Artefacts :
- `WorshipFlow-5.8.6-debug-api23-api29`
- `WorshipFlow-5.8.6-release-api23`
- `WorshipFlow-5.8.6-release-api29`