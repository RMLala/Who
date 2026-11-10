# Rapport — WorshipFlow 5.8.3

## Correction

Le formulaire « Moment de lecture » séparait mal le titre de la ressource et le libellé « Affichage », ce qui faisait apparaître les textes sur la même ligne et se chevaucher sur téléphone. Le titre dispose maintenant de son propre bloc avec retour à la ligne; le choix du moment et le champ de durée sont des champs verticaux pleine largeur. Le champ de durée masqué reste réellement caché quand « Pendant » n’est pas sélectionné.

## Contrôles

- Syntaxe JavaScript intégrée : vérifiée avec `node --check`.
- Synchronisation des ressources Web vers Android : vérifiée avec `npx cap sync android`.
- Version de l’application : 5.8.3; versionCode Android par défaut : 30.
- Aucun APK n’a été compilé dans cet environnement; le téléchargement de Gradle 8.2.1 avait échoué faute d’accès réseau.
