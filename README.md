# Casa Gestionale

Applicazione Android per la gestione di un negozio di prodotti per la casa.

## Funzioni principali

- Homepage con riepilogo velocissimo
- Magazzino con ricerca e gestione articoli
- Cassa con aggiunta prodotti e pagamento
- Vendite registrate in archivio locale
- Statistiche e andamento incassi
- Database SQLite locale sul dispositivo
- Fotocamera per scansione barcode o riconoscimento prodotto

## Stile grafico

- interfaccia semplice e professionale
- colori neutri e sobrii
- layout chiaro e facilmente leggibile
- orientato all'uso quotidiano di un negozio

## APK pronto

L'APK compilato e verificato è disponibile qui:

- [releases/CasaGestionale-debug.apk](releases/CasaGestionale-debug.apk)

## Anteprima locale

1. Aprire il progetto in Android Studio.
2. Verificare che sia installato:
   - JDK 17
   - Android SDK 35
3. Aprire il progetto e avviare l'app su emulatore o dispositivo reale.

## Comando di build

Da cartella del progetto:

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME=/home/codespace/android-sdk
export ANDROID_SDK_ROOT=/home/codespace/android-sdk
./gradlew --no-daemon -Dorg.gradle.jvmargs='-Xmx1g -Xms256m' assembleDebug
```

## Struttura principale

- [app/src/main/java/it/casagestionale/app/MainActivity.kt](app/src/main/java/it/casagestionale/app/MainActivity.kt) — interfaccia e navigazione
- [app/src/main/java/it/casagestionale/app/StoreDatabase.kt](app/src/main/java/it/casagestionale/app/StoreDatabase.kt) — database e logica di vendita
- [app/src/main/java/it/casagestionale/app/CameraScanner.kt](app/src/main/java/it/casagestionale/app/CameraScanner.kt) — scansione barcode e riconoscimento prodotto
- [releases/CasaGestionale-debug.apk](releases/CasaGestionale-debug.apk) — APK pronto

## Nota

Questo è un build di debug, idoneo per test e condivisione interna. Se vuoi, in un prossimo passaggio posso preparare anche una versione release, con splash screen, logo, backup database e APK firmato.