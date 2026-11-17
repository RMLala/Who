# Rapport WorshipFlow 5.8.5

Date : 17 novembre 2026  
Branche : `work/worshipflow-v3-cloud-20261117`  
PR : #8 vers `main` (ouverte, non fusionnée)

## Correctifs livrés

- Le verrouillage biométrique/PIN est désactivé par défaut sur une nouvelle installation.
- Réglages propose explicitement d’activer ou de désactiver le verrouillage.
- Une fois activé, Android propose la biométrie en priorité et conserve le PIN à 4 chiffres comme secours.
- Sur les installations 5.8.4 qui avaient reçu le verrouillage automatiquement, la mise à jour demande une dernière authentification pour déchiffrer les données, les migrer, puis désactiver ce verrouillage imposé. Les installations qui auront explicitement choisi l’activation conservent leur préférence.
- Correction de l’écriture IndexedDB : les magasins à clé intégrée reçoivent l’objet sans clé externe; les magasins à clé explicite continuent de recevoir leur clé.
- Le retrait du verrou convertit d’abord toutes les données chiffrées en données lisibles, puis efface la clé locale. Les données ne sont pas supprimées.
- Version Android/npm : 5.8.5; versionCode Debug par défaut : 32. La CI attribue un code Release supérieur selon le numéro de workflow.
- Les corrections précédentes de synchronisation Cloud sous licence, session chiffrée et libellés de tri localisés sont conservées.

## Vérifications

Workflow Android [44139550925](https://github.com/RMLala/Who/actions/runs/44139550925) : succès.

- 16 tests Node : succès.
- Parcours UI mobile et bureau : succès, dont le démarrage sans demande de PIN et le cycle d’activation/désactivation dans Réglages.
- APK Debug API 23/29 : compilation réussie.
- APK Release signés API 23/29 : compilation et vérification des signatures réussies.

## Artefacts

- `WorshipFlow-5.8.5-debug-api23-api29`
- `WorshipFlow-5.8.5-release-api23`
- `WorshipFlow-5.8.5-release-api29`

Les validations Android sur téléphone réel restent nécessaires. La Release API 29 est celle à installer sur Android 13.
