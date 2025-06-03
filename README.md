# KeyPassManager

KeyPassManager è un password manager Android moderno, sicuro e open source, sviluppato in Kotlin con Jetpack Compose e Room. Offre cifratura avanzata, backup automatico, gestione sicura della master password e ripristino da backup in caso di database corrotto.

## Requisiti
- Android Studio Flamingo o superiore
- Android SDK 33+
- Gradle 8+
- Dispositivo/emulatore Android (API 26+)

## Installazione e Avvio
1. **Clona il repository**
   ```sh
   git clone <repo-url>
   ```
2. **Apri la cartella `KeyPassManager` in Android Studio**
3. **Sincronizza Gradle** (Android Studio lo farà automaticamente)
4. **Esegui il progetto** su un emulatore o dispositivo reale

## Funzionalità principali
- **Cifratura AES-GCM** delle credenziali tramite Android Keystore
- **Master password** separata dalla chiave di cifratura del database
- **Backup automatico** delle credenziali in memoria interna ad ogni inserimento
- **Ripristino credenziali** da backup tramite pulsante dedicato
- **Gestione database corrotto**: dialog di ripristino/reset
- **UI moderna** con Material3, supporto dark/light
- **Copia rapida** di username/password
- **Logout automatico** dopo timeout di inattività

## Manuale Utente

### Primo avvio
- All'apertura, imposta una master password e un'email di recupero.
- La master password serve per accedere e per derivare la chiave di cifratura del database.

### Aggiunta credenziale
- Premi il pulsante "+" in basso a destra.
- Inserisci compagnia, username e password.
- Premi "Salva". La credenziale viene cifrata e salvata.

### Visualizzazione e copia
- Tocca una credenziale per vedere i dettagli.
- Usa l'icona copia per copiare username o password negli appunti.

### Eliminazione credenziale
- Tieni premuto su una credenziale e conferma dal bottom sheet.

### Backup e ripristino
- Ogni nuova credenziale viene salvata anche in un file di backup cifrato nella memoria interna (`/data/data/<package>/files/keypassbackup`).
- Premi il pulsante "Ripristina backup" per recuperare tutte le credenziali dai backup.

### Gestione database corrotto
- Se il database risulta danneggiato, viene mostrato un dialog che permette di resettare o ripristinare le credenziali dai backup.

### Logout e sicurezza
- Dopo 15 minuti di inattività, la sessione viene chiusa automaticamente.
- Tutte le preferenze e le chiavi sono salvate solo in EncryptedSharedPreferences.

## Documentazione delle principali funzioni

- **EncryptionUtils**: Utility per cifrare/decifrare stringhe con AES-GCM tramite Android Keystore.
- **DatabasePassphraseProvider**: Gestisce la master password, la derivazione della chiave di cifratura e la sessione sicura.
- **CredentialRepository**: Gestisce l'accesso ai dati, backup/ripristino, inserimento, aggiornamento ed eliminazione credenziali.
- **CredentialViewModel**: Espone lo stato delle credenziali e le funzioni di business alla UI.
- **CredentialListScreen**: Schermata principale, mostra la lista, gestisce selezione, eliminazione, backup/ripristino e dialog di uscita.
- **CredentialEditScreen**: Schermata per inserimento/modifica credenziale.
- **MasterPasswordScreen**: Schermata di login/registrazione e gestione database corrotto.
- **NavGraph**: Gestisce la navigazione tra le schermate principali.

Per dettagli su ogni funzione, consulta i commenti nel codice sorgente.

---

Per problemi o richieste, apri una issue sul repository.
