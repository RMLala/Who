# WorshipFlow — vérification V3

## Constats vérifiés
- `www/index.html` présente des textes de partage Drive manuel et restauration par fichier ; cela ne prouve pas le bon fonctionnement Android.
- `package.json` contient `@capacitor/share`, sans prouver son invocation correcte.
- `README.md` mentionne Web/PWA ; rechercher les builds effectifs.
- `www/index.html` contient un historique des cultes, distinct de l'historique des versions interdit dans la page À propos : ne pas supprimer les données des cultes.

## Vérifications / corrections à réaliser
1. Examiner la page À propos : supprimer seulement l'historique des versions, conserver historique des cultes et autres données métier.
2. Ajouter/vérifier AppForge Studio, Made in Madagascar, email support, licence/Cloud et guide.
3. Vérifier biométrie prioritaire + PIN sécurisé et tests sur Android.
4. Tester partage Drive natif, restauration, intégrité, sauvegarde chiffrée facultative, compatibilité anciens fichiers.
5. Vérifier logos et CI Debug/Release API23/API29 sans build PWA.

## Statut
Plan d'audit uniquement, sans modification fonctionnelle.