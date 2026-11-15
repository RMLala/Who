# WorshipFlow 5.8.3 — licence et synchronisation

## Implémentation

- Contrat réel AppForge License API : trial, activate, status. Produit : WORSHIPFLOW.
- Preuves ECDSA P-256 vérifiées avec la clé publique du service, application et installation contrôlées, expiration vérifiée. Aucun secret administrateur embarqué.
- Essai demandé au serveur, sans création d’un nouvel identifiant lors des sauvegardes/restaurations ordinaires. Les réinstallations/effacements de stockage peuvent créer une nouvelle installation : une protection contre ces contournements exige une identité serveur ou une attestation native.
- Licence bloquée après réponse autoritative d’invalidité/révocation. Une erreur réseau conserve la dernière preuve valide jusqu’à son expiration. Pas de renouvellement local de l’essai.
- Connexion Cloud avec compte existant, choix d’organisation, session en mémoire uniquement, déconnexion et révocation serveur de session.
- Instantanés versionnés : chants, cultes, ressources, thèmes. Audio, chemins d’accès, réglages de l’appareil, jetons et mots de passe exclus.
- Synchronisation manuelle et automatique toutes les 30 secondes lorsque la session et l’option sont actives, reprise au retour du réseau.
- Première divergence ou modifications simultanées : arrêt et choix explicite. Révision attendue dans chaque PUT ; réponse 409 conservée comme conflit.
- Application atomique des données cloud dans IndexedDB, conservation des accès audio propres à cet appareil, copie de récupération avant remplacement.
- Formats FR/MG/EN pour le panneau principal ; certains messages techniques restent en français.

## Vérification observée le 15 novembre 2026

- /v1/config du service de licences : réponse réelle et clé publique observées.
- /health AppForge Cloud : service en ligne, D1 et authentification email/mot de passe annoncés.
- Préflight Cloud depuis https://localhost : HTTP 204, origine Android autorisée.
- Demande d’essai réelle pour WORSHIPFLOW : « Application inconnue ou inactive. »
- 10 tests Node réussis localement : scripts HTML, format des données, ECDSA valide/invalide, expiration, installation, révocation, réseau, conflits, révisions et exclusion des secrets/chemins audio.
- Les tests UI mobile/ordinateur et les quatre builds sont lancés par GitHub Actions sur la PR. Les Release sur PR sont limitées aux branches appartenant au même dépôt ; les forks n’accèdent pas aux secrets.

## Limites et blocage de mise en production

Le produit WORSHIPFLOW doit être ajouté/activé dans AppForge Studio → Codes de déblocage → Applications, sur le Worker de licences existant. Cette opération exige la session administrateur du propriétaire ; elle n’est pas disponible dans cet environnement. Ne pas publier cette version avec le verrou de licence tant que le produit est inconnu.

Aucune activation payante, licence propriétaire ou synchronisation avec un compte réel n’est déclarée vérifiée. Les tests simulés ne remplacent pas une validation avec le service et un compte autorisé. L’accès et la session Cloud sont requis pour les essais réels de synchronisation inter-appareils.

La vérification JavaScript locale apporte un contrôle fonctionnel ; une version HTML modifiable peut être contournée. Le contrôle d’accès réel aux données Cloud est assuré par le backend. Le stockage audio n’est pas un service de transfert des fichiers MP3.

Les APK de la PR sont des candidats de test, pas une livraison de production validée. La PR reste ouverte jusqu’au test de l’essai réel et des connexions réelles. Main n’est pas modifiée.
