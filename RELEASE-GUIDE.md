# WorshipFlow 5.8.3 — compilation Android

Deux variantes : API 23 (Android 6+) et API 29 (Android 10+). La version est 5.8.3 (versionCode par défaut 30).

1. Copier le contenu de l’archive à la racine du dépôt, avec `.github/workflows/android.yml`, `package.json`, `package-lock.json`, `android/` et `www/`.
2. Pour signer une Release, configurer les secrets GitHub `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` et `ANDROID_KEY_PASSWORD`. Ne jamais committer le keystore ou les mots de passe.
3. GitHub Actions → WorshipFlow Android → Run workflow → `build_type=release`. Les pushes sur `main` créent des APK Debug.
4. `version_code=0` calcule automatiquement 1000 + le numéro du lancement GitHub. Sinon, fournir un code supérieur à la version distribuée précédente.
5. Télécharger les artefacts `WorshipFlow-5.8.3-api23-release` et `WorshipFlow-5.8.3-api29-release`.

Compilation Debug locale : `npm ci`, `npx cap sync android`, puis dans `android/` : `./gradlew assembleApi23Debug assembleApi29Debug`.

Release locale : définir `ANDROID_KEYSTORE_PATH`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` et `ANDROID_KEY_PASSWORD`, puis lancer `./gradlew assembleApi23Release assembleApi29Release -PreleaseCode=N`.
