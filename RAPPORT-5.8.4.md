# WorshipFlow 5.8.4 — corrections demandées

## Corrections fonctionnelles

- PIN de secours : exactement 4 chiffres pour toute nouvelle configuration. Si une installation existante utilise l’ancien PIN de 6 à 12 chiffres, l’utilisateur peut l’utiliser une fois; après déverrouillage biométrique ou PIN, l’application impose la conversion vers un nouveau PIN à 4 chiffres.
- Biométrie : Android BiometricPrompt prioritaire; le PIN à 4 chiffres déverrouille la même clé en secours.
- Données locales : tous les magasins IndexedDB sont chiffrés AES-GCM, blobs audio inclus; la migration des données antérieures est idempotente.
- Licence et Cloud : après activation d’une licence payante, la synchronisation est activée et la connexion Cloud est proposée pour le premier choix d’organisation. Le premier accès demande l’e-mail et le mot de passe du compte Cloud. Le mot de passe est effacé du formulaire et jamais enregistré.
- Après cette première connexion, le jeton de session et l’organisation sont conservés dans le stockage local chiffré. La synchronisation automatique reprend au prochain lancement; seules une expiration ou une révocation de session imposent une nouvelle connexion.
- Le panneau Réglages affiche l’état de connexion et l’organisation quand la session est active; il ne montre plus le bouton de connexion comme si la session était absente. Les sessions sont invalidées après réponse HTTP 401.
- Tri des chants : le contrôle visible utilise « N° croissant » (FR), « N° mitombo » (MG), « Number ascending » (EN). La version de livraison passe à 5.8.4 pour distinguer le nouvel APK des anciens builds 5.8.3.

## Vérifications

Le workflow [44108769659](https://github.com/RMLala/Who/actions/runs/44108769659) a réussi sur le code applicatif `c709011`: 16 tests Node, UI mobile/ordinateur, Debug API 23/29, Release API 23/29 et signatures vérifiées. Artefacts: Debug `14339458016`, Release API 23 `14339861942`, Release API 29 `14341025095`. Les tests automatisés ne remplacent pas la validation biométrique sur téléphone, la connexion AppForge réelle ni une synchronisation avec un compte Cloud autorisé.

L’activation administrative de WORSHIPFLOW n’est pas incluse dans ce commit : aucun accès à la console AppForge Studio n’est connecté ici.
