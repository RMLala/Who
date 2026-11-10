# Rapport de livraison — WorshipFlow 5.8.1

## Modifications réalisées

- Ajout dans **Options du culte** de « Exporter les audios du culte ».
- Android ouvre le sélecteur de dossiers du système, crée un sous-dossier nommé avec la date et le nom du culte, puis copie chaque audio référencé dans ce culte, y compris les blobs restaurés dans IndexedDB (copie native par fragments). Les fichiers sources ne sont pas déplacés ni supprimés.
- Dans Chrome/Edge compatible, l’utilisateur choisit un dossier et les audios copiés dans le stockage du navigateur y sont écrits. Si l’API de choix de dossier n’existe pas, l’application télécharge les fichiers individuellement.
- La carte de sauvegarde indique maintenant séparément le nombre et la taille des audios copiés dans l’application et le nombre de fichiers liés au stockage du téléphone. Une sauvegarde JSON inclut les audios copiés dans IndexedDB si la case correspondante est cochée; les liens Android vers les fichiers sources ne sont pas incorporés au JSON.
- Version de l’application, versionCode Android et cache PWA mis à jour en 5.8.1.

## Réponse sur le stockage et la lecture

Sur Android, choisir un audio du téléphone n’en fait pas une copie dans WorshipFlow : Android accorde à l’application l’accès au fichier d’origine et le lecteur le lit directement depuis ce dossier. L’application conserve son URI et les métadonnées. En revanche, l’import audio dans la version navigateur/standalone stocke un Blob dans IndexedDB, donc une copie dans le stockage du navigateur. L’export du culte copie ces fichiers dans le dossier de destination choisi.

## Vérifications

- `node --check` sur le script JavaScript intégré : réussi.
- `npx cap sync android` : réussi; les fichiers web ont été synchronisés avec les assets Android et les plugins Capacitor détectés.
- La compilation Gradle Debug a été tentée, mais le wrapper n’a pas pu télécharger Gradle 8.2.1 (réseau indisponible). Aucun APK n’a donc été généré ici.
- La Release signée nécessite aussi le keystore et ses secrets, absents de cet environnement. Le workflow GitHub et les instructions Release sont à jour pour produire les APK signés quand ces secrets sont configurés.
