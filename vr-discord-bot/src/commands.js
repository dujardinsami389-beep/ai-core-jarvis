import { SlashCommandBuilder, ChannelType } from 'discord.js';

const channelOption = builder => builder.addChannelOption(option => option
  .setName('salon')
  .setDescription('Salon où publier la recherche ou la sortie')
  .addChannelTypes(ChannelType.GuildText)
  .setRequired(true));

export const commands = [
  new SlashCommandBuilder().setName('aide').setDescription('Voir comment utiliser le bot VR'),
  new SlashCommandBuilder().setName('candidature').setDescription('Envoyer une candidature à la communauté VR'),
  channelOption(new SlashCommandBuilder().setName('groupe').setDescription('Trouver des joueurs pour une session VR'))
    .addStringOption(option => option.setName('jeu').setDescription('Jeu VR').setRequired(true).setMaxLength(80))
    .addStringOption(option => option.setName('casque').setDescription('Quest, PCVR, PS VR2…').setRequired(true).setMaxLength(50))
    .addStringOption(option => option.setName('heure').setDescription('Quand ? Ex. ce soir vers 20 h').setRequired(true).setMaxLength(80))
    .addIntegerOption(option => option.setName('places').setDescription('Nombre de places recherchées').setMinValue(1).setMaxValue(20)),
  channelOption(new SlashCommandBuilder().setName('sortie').setDescription('Annoncer une soirée ou un événement VR'))
    .addStringOption(option => option.setName('titre').setDescription('Nom de la soirée').setRequired(true).setMaxLength(100))
    .addStringOption(option => option.setName('jeu').setDescription('Jeu ou activité').setRequired(true).setMaxLength(80))
    .addStringOption(option => option.setName('quand').setDescription('Date et heure, avec fuseau si utile').setRequired(true).setMaxLength(100))
    .addStringOption(option => option.setName('details').setDescription('Infos utiles pour participer').setMaxLength(500))
].map(command => command.toJSON());
