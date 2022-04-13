# Notes app
A simple android application for storing notes. This application is designed to store the confidential information. The app operates in offline mode and doesn't send any data in any way to the server.  It uses google auth as a single source of authentication and allows user to perform CRUD operation offline. The app is based on google's material UI and follows the clean code architecture.

Basic features
1. Simple UI
2. Support backup and restore
3. Data security
4. Backup and restore options can be accessed by dragging the bottom left corner in the home screen

Security features
1. Note data is encrypted using the user authentication and biometric information before going into the database.
2. Only the biometric authenticated user can access the notes.
3. App maintains the 12 second session time after each time user decrypts the notes data using biometric.
4. Entire backup data is AES encrypted and stored in a password protected .zip file.
5. Backup can be restored only using the encryption key(provided when backup is created) and biometric authentication.

## Screenshot
<img width='200' height='433' src='https://user-images.githubusercontent.com/31315800/132082746-fefc553e-b1d8-43ec-8c5f-5c275fe699c3.jpg'> <img width='200' height='433' src='https://user-images.githubusercontent.com/31315800/132082747-5cecfcd9-0998-45b9-8ba0-2fdc51872ada.jpg'> <img width='200' height='433' src='https://user-images.githubusercontent.com/31315800/132082745-168522ad-8cb8-4c73-b6f5-59ae4018cc5e.jpg'> <img width='200' height='433' src='https://user-images.githubusercontent.com/31315800/163075372-855968b1-33f1-4099-8214-60d116a74f15.jpg'> 
<img width='200' height='433' src='https://user-images.githubusercontent.com/31315800/132082744-68db69af-5d42-4349-aee0-63e8f4c3a19a.jpg'> <img width='200' height='433' src='https://user-images.githubusercontent.com/31315800/163075363-166ffd5b-f7ff-4274-88d9-ee3a62d11535.jpg'> <img width='200' height='433' src='https://user-images.githubusercontent.com/31315800/163075370-3d862aa7-7b85-4319-803b-0df84b9ce6ad.jpg'> 

## Download APK
[Download from here](https://rebrand.ly/NOTES-APK)