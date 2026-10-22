# AI CORE Android

AI CORE est un assistant Android en français : dictée, brouillons de messages à relire, notes et outils du téléphone, avec une option de chat IA entièrement hors ligne.

## IA hors ligne Gemma

Le modèle officiel Gemma 3 1B quantifié pèse environ **529 Mo**. Google recommande de le télécharger séparément, car un modèle de cette taille ne doit pas être intégré dans l’APK. Le modèle reste sur le téléphone après import. Pour de bonnes performances, prévois au moins 4 Go de mémoire vive et environ 700 Mo libres au moment de l’installation.

1. Installe et ouvre AI CORE.
2. Dans la carte **IA hors ligne**, touche **1 · Télécharger le modèle**.
3. Sur Hugging Face, connecte-toi si demandé et accepte les conditions de la licence Gemma.
4. Télécharge le fichier **gemma3-1b-it-int4.litertlm**.
5. Reviens dans AI CORE et touche **2 · Installer le fichier**. Choisis le fichier téléchargé.
6. Quand le statut indique **IA hors ligne prête**, pose ta question.

Le téléchargement et l’import du modèle sont séparés de l’APK. L’IA locale fonctionne ensuite sans connexion. Les liens vers météo, cartes, recherche et autres applications ont toujours besoin d’Internet.

## Fonctions

- Chat local avec Gemma 3 1B, une fois le modèle installé.
- Dictée française avec le service vocal Android après autorisation du micro.
- Préparation de messages pour WhatsApp, SMS, Telegram, Signal et autres applications; tu relis et envoies toi-même.
- Heure, date, calculatrice, notes, météo, cartes, recherche, agenda, YouTube, Spotify et dépôt GitHub.

## Télécharger l’APK depuis GitHub

1. Ouvre l’onglet **Actions** du dépôt.
2. Sélectionne la dernière exécution de **Build AI CORE APK**.
3. Dans **Artifacts**, télécharge **AI-CORE-Android-APK** et extrais le ZIP.
4. Ouvre le fichier APK sur ton téléphone et confirme l’installation si Android le demande.

L’APK debug ne contient pas le modèle Gemma. Le modèle se télécharge à part pour respecter ses conditions d’accès et éviter un faux fichier de 500 Mo.
