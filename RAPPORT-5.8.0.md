# Rapport de livraison — WorshipFlow 5.8.0

## Modifications réalisées

1. **Chants** : filtre Sans thème, tri alphabétique inverse ajouté aux tris existants.
2. **Inspirations / Versets** : bouton + ouvrant un véritable éditeur, validation et retour en haut, thèmes visibles, filtre et tri par thème.
3. **Ressources** : titre, édition riche (gras, italique, souligné, listes, couleur et surlignage), thèmes cochables et conservation du formatage dans la lecture. Le HTML enregistré est nettoyé pour exclure scripts et éléments non autorisés.
4. **Annuler / Rétablir** : historique relié aux modifications, commandes désactivées lorsque l’historique est vide, protection contre les clics concurrents.
5. **Lecture Pendant** : choix d’un instant mm:ss relatif au chant ; déclenchement selon la position audio et recalcul après un déplacement dans la piste. Sans audio, délai relatif au démarrage du chant.
6. **Préparation du culte** : bouton Démarrer au-dessus de la liste et flèches haut/bas accessibles sur les chants.
7. **Lecteur et suivi** : minuteur compact à droite dans le bloc de lecture, pause/reprise conservée, espace accru pour les paroles ; notation du culte de 1 à 5 et commentaire ; historique des sessions, durée, nombre de sessions, chants distincts et note moyenne des cultes notés. La durée de session mesure le temps passé dans le lecteur, pauses comprises. Plein écran Web vérifié et correction Android exécutant les changements de fenêtre sur le thread UI.

Les fonctions antérieures restent dans le projet, notamment recherche Tononkira et import groupé. Elles n’ont pas toutes fait l’objet d’un nouveau test en ligne lors de cette livraison.

## Vérifications exécutées

- 16 contrôles automatisés réussis dans Chromium : filtre sans thème, emplacement du démarrage, déplacement/annuler/rétablir, ajout et association de ressource, conservation du formatage nettoyé, historique ressource, emplacement du minuteur, programmation temporelle et retour arrière, barre de commandes aux largeurs 320/390/768/1380 px, plein écran Web, notation/statistiques et persistance après rechargement.
- Aucune erreur JavaScript de page détectée dans ce parcours.
- Essai supplémentaire avec un vrai fichier WAV et le lecteur HTML audio : apparition au seuil programmé, disparition après retour avant le seuil.
- Inspection visuelle du lecteur mobile avec audio et de l’éditeur de ressource.
- `npm ci` et `npx cap sync android` réussis.
- Identité vérifiée entre le HTML standalone, le HTML Web et la copie embarquée Android ; cohérence des configurations JSON et workflow ; contrôle de l’intégrité ZIP.

## Android Release : état exact

Sources et workflows fournis pour API23 et API29, en Debug et Release. Les Release utilisent une clé commune fournie par les secrets GitHub et vérifient la signature des APK. Aucun keystore ni secret n’est inclus.

**Aucun APK n’a été généré ici.** La tentative locale s’est arrêtée lors du téléchargement de Gradle (réseau inaccessible depuis Java). GitHub n’étant pas connecté, le workflow n’a pas pu être lancé à distance. Le plein écran natif doit donc encore être vérifié sur appareil, tout comme les installations/mises à jour des deux variantes. Le guide décrit le lancement des builds avec les secrets existants.

Ce rapport décrit les contrôles réellement exécutés ; il ne garantit pas l’absence de tout défaut sur tous les appareils.
