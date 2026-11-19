# AES-Based Distributed Cloud System

A Java client-server prototype for encrypted file transfer and storage over TCP sockets, built with Java Swing interfaces and custom/JCA AES encryption routines.

## Project Structure

- `AES.java` — Custom AES-128 block cipher implementation (`SubBytes`, `ShiftRows`, `MixColumns`, `AddRoundKey`, key expansion, and decryption counterparts).
- `Client.java` — Swing desktop client (`localhost:5050`) supporting file selection, key entry, client-side AES encryption/decryption, and periodic file list polling.
- `Server.java` — Multi-threaded TCP socket server (`port 5050`) with a live log window, client handler threads, and in-memory file storage.
- `FileInfo.java` — Serializable payload model carrying filename, encrypted byte array, and uploader metadata.
- `CloudSystem.java` — NetBeans entry-point stub (`com.mycompany.cloudsystem`).

## How It Works

1. The **Server** starts a `ServerSocket` on port `5050`, opens a Swing log viewer, and spawns a dedicated client handler thread for each connecting client.
2. On **upload**, the **Client** reads the file bytes, derives a 16-byte key via SHA-1 and encrypts with AES before sending the filename and ciphertext over an `ObjectOutputStream`.
3. The **Server** stores encrypted file entries in memory and broadcasts updates, while the **Client** fetches entries and decrypts downloaded bytes locally using the user's key.

## Notes & Known Limitations

- `Client.java` sends `"UPLOAD"`, `"DOWNLOAD"`, and `"LIST"` commands and uses `FileInfo`, whereas `Server.java` handles `"UL"`, `"DL"`, and `"LS"` with an internal `Fi` class—aligning these protocol strings and payload classes is needed for full end-to-end interoperability.
- `Client` currently uses `Cipher.getInstance("AES")` with a SHA-1 derived 16-byte key, and `Server` stores files in memory rather than persistent storage.
