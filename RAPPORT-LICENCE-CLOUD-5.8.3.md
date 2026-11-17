# WorshipFlow 5.8.3 — licences, synchronisation et sécurité locale

## Licence et synchronisation

- Produit AppForge : WORSHIPFLOW. Contrat : essai, activation et statut; preuves ECDSA P-256 vérifiées côté client contre la clé publique de service.
- La synchronisation reste désactivée sans licence payante active et session Cloud valide. Les changements locaux activent la possibilité de synchroniser; l’achat ne crée pas un compte ni une organisation.
- Connexion Cloud avec un compte existant, sélection d’organisation, session en mémoire, déconnexion et révocation.
- Synchronisation versionnée toutes les 30 secondes, reprise réseau, contrôle de révision, conflits 409 explicites et copie locale de récupération avant application.
- Instantanés limités aux chants, cultes, ressources et thèmes. Audio, chemins de fichiers, réglages appareil, licence et identifiants ne sont pas synchronisés.
- Les réponses autoritatives d’invalidité/révocation bloquent la licence. Une erreur réseau conserve la preuve valide jusqu’à expiration. Pas de renouvellement local d’essai.

## Sécurité locale ajoutée sur la PR #8

- Le plugin Android `NativeSecurityPlugin` utilise Android Keystore, AndroidX BiometricPrompt et AES-GCM. La clé de données reste en mémoire uniquement pendant la session native.
- À la première ouverture, l’utilisateur crée un PIN de 6 à 12 chiffres. La dérivation PBKDF2-HMAC-SHA256 (210 000 itérations) protège une copie de récupération de la clé de données. Si une biométrie forte est disponible, Android demande une confirmation biométrique et conserve une enveloppe séparée liée au Keystore.
- Aux ouvertures suivantes, la biométrie est proposée en premier; le PIN déverrouille la clé quand la biométrie est annulée ou indisponible.
- Tous les magasins IndexedDB (`songs`, `services`, `insps`, `kv` et `audio`) sont chiffrés AES-256-GCM au repos, y compris les blobs audio importés (chiffrés par blocs de 512 Kio). Les fichiers audio choisis par URI restent dans leur emplacement d’origine hors IndexedDB; chemins et métadonnées stockés dans les chants sont chiffrés.
- Les anciennes lignes et blobs en clair sont convertis de façon idempotente après déverrouillage; un arrêt pendant la migration ne supprime pas les données restantes.
- Le PIN ne peut pas être réinitialisé sans perte d’accès à la clé de données. Conserver une sauvegarde exportée séparément. Les sauvegardes Drive et leur chiffrement facultatif sont des fonctions distinctes.
- Le pont Android est requis pour utiliser ce parcours sécurisé. Le test UI navigateur fournit un faux pont de test et ne démontre pas une biométrie réelle.

## Vérifications

- PR #8 rebasée sur `main` avant cette livraison; modifications poussées sur `work/worshipflow-v3-cloud-20261117`.
- CI à contrôler après les derniers commits : tests Node, contrôle navigateur mobile/ordinateur, compilation Debug API 23/29, Releases signées API 23/29 et contrôle de signature.
- Ces contrôles automatisés ne valident pas la biométrie sur appareil, l’accès audio, le partage Drive, une restauration réelle, l’achat/activation réelle, ni une synchronisation avec compte réel.
- Lors de la vérification précédente, l’essai réel répondait « Application inconnue ou inactive ». La console AppForge Studio et ses permissions d’administration ne sont pas connectées à cet environnement. L’activation du produit et l’essai Cloud réel ne sont donc pas confirmés par ce changement.
- Une activation réelle exige l’ajout/activation de WORSHIPFLOW dans AppForge Studio. Aucun secret administrateur n’est intégré au dépôt ni à l’application.
