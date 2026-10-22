# Bot Discord AI CORE • Communauté VR

Un bot léger pour aider les joueurs à se retrouver : recherche de groupes, annonces de soirées avec fil de discussion, candidatures envoyées aux modérateurs avec boutons d’acceptation/refus, et accueil des nouveaux membres. Il ne contacte personne en privé et ne fabrique pas de faux membres.

## Fonctionnalités

- `/groupe` publie le jeu, le casque, l’heure et le nombre de places, puis ouvre un fil pour organiser la partie.
- `/sortie` annonce une soirée ou activité et ouvre un fil aux participants.
- `/candidature` ouvre un formulaire privé. Les réponses sont envoyées dans le salon de modération défini par `APPLICATIONS_CHANNEL_ID`.
- Les membres ayant la permission **Gérer le serveur** peuvent accepter ou refuser une candidature. Si `ACCEPTED_ROLE_ID` est défini, l’acceptation attribue aussi ce rôle au candidat.
- `/aide` rappelle les commandes. L’accueil des nouveaux est optionnel.

## Installation

1. Installe Node.js 22.12 ou plus récent sur l’ordinateur qui fera tourner le bot.
2. Dans [Discord Developer Portal](https://discord.com/developers/applications), crée une application, puis ajoute un bot dans l’onglet **Bot**.
3. Copie le jeton du bot dans un endroit sûr. **Ne le publie jamais et ne l’envoie à personne.** Si tu penses qu’il a fuité, régénère-le dans le portail.
4. Dans **OAuth2 → URL Generator**, coche les scopes `bot` et `applications.commands`. Pour les permissions, coche `View Channels`, `Send Messages`, `Embed Links`, `Create Public Threads`, `Send Messages in Threads`, `Read Message History` et `Manage Roles` si tu veux l’attribution automatique du rôle. Ouvre le lien généré pour inviter le bot sur ton serveur.
5. Dans **Bot → Privileged Gateway Intents**, active **Server Members Intent** si tu veux l’accueil des nouveaux ou le rôle automatique.
6. Télécharge ce dossier depuis GitHub ou décompresse le ZIP. Copie `.env.example` en `.env`, puis renseigne :
   - `DISCORD_TOKEN` : jeton du bot
   - `CLIENT_ID` : ID de l’application
   - `GUILD_ID` : ID du serveur (active le mode développeur dans Discord, clic droit sur le serveur → Copier l’identifiant)
   - `APPLICATIONS_CHANNEL_ID` : ID du salon privé réservé aux candidatures
   - `ACCEPTED_ROLE_ID` : ID du rôle à donner en cas d’acceptation (facultatif, et le rôle du bot doit être placé au-dessus de ce rôle)
   - `WELCOME_CHANNEL_ID` : ID du salon d’accueil (facultatif)
7. Ouvre un terminal dans ce dossier et lance :

   ```bash
   npm install
   npm run deploy
   npm start
   ```

Garde le terminal ouvert pour que le bot reste connecté. Pour le faire fonctionner 24 h/24, il faudra ensuite le mettre sur un hébergement Node.js qui reste allumé. Le code ne stocke aucun jeton ; `.env` est exclu de Git.

## Dépannage

- Les commandes n’apparaissent pas : vérifie `CLIENT_ID`, `GUILD_ID` et relance `npm run deploy`.
- Les annonces ne partent pas : donne au bot accès au salon et les permissions listées ci-dessus.
- Les candidatures n’apparaissent pas : vérifie `APPLICATIONS_CHANNEL_ID` et l’accès du bot au salon privé.
- Le rôle n’est pas donné : vérifie `ACCEPTED_ROLE_ID`, `Manage Roles` et l’ordre des rôles.
- Si un jeton a été publié par erreur, réinitialise-le immédiatement dans le Developer Portal.
