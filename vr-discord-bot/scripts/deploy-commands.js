import 'dotenv/config';
import { REST, Routes } from 'discord.js';
import { commands } from '../src/commands.js';

const { DISCORD_TOKEN, CLIENT_ID, GUILD_ID } = process.env;
if (!DISCORD_TOKEN || !CLIENT_ID || !GUILD_ID) {
  throw new Error('DISCORD_TOKEN, CLIENT_ID et GUILD_ID doivent être renseignés dans .env');
}

const rest = new REST({ version: '10' }).setToken(DISCORD_TOKEN);
await rest.put(Routes.applicationGuildCommands(CLIENT_ID, GUILD_ID), { body: commands });
console.log(`${commands.length} commandes installées sur le serveur.`);
