import 'dotenv/config';
import {
  ActionRowBuilder, ButtonBuilder, ButtonStyle, Client, EmbedBuilder, Events,
  GatewayIntentBits, ModalBuilder, PermissionFlagsBits, TextInputBuilder, TextInputStyle
} from 'discord.js';

const { DISCORD_TOKEN, WELCOME_CHANNEL_ID, APPLICATIONS_CHANNEL_ID, ACCEPTED_ROLE_ID } = process.env;
if (!DISCORD_TOKEN) throw new Error('DISCORD_TOKEN manque dans le fichier .env');

const client = new Client({ intents: [GatewayIntentBits.Guilds, GatewayIntentBits.GuildMembers] });
const applicationButtons = applicantId => new ActionRowBuilder().addComponents(
  new ButtonBuilder().setCustomId(`application:accept:${applicantId}`).setLabel('Accepter').setStyle(ButtonStyle.Success),
  new ButtonBuilder().setCustomId(`application:reject:${applicantId}`).setLabel('Refuser').setStyle(ButtonStyle.Danger)
);

client.once(Events.ClientReady, readyClient => console.log(`Bot VR connecté : ${readyClient.user.tag}`));

client.on(Events.GuildMemberAdd, async member => {
  if (!WELCOME_CHANNEL_ID) return;
  const channel = await member.guild.channels.fetch(WELCOME_CHANNEL_ID).catch(() => null);
  if (!channel?.isTextBased()) return;
  await channel.send({ embeds: [new EmbedBuilder().setColor(0x7c5cff)
    .setTitle('Bienvenue dans la communauté VR !')
    .setDescription(`Salut ${member} ! Présente ton casque et tes jeux préférés, puis utilise **/groupe** pour trouver des coéquipiers.`)
    .setFooter({ text: 'AI CORE • Communauté VR' })] })
    .catch(error => console.error('Accueil non envoyé :', error.message));
});

client.on(Events.InteractionCreate, async interaction => {
  try {
    if (interaction.isChatInputCommand()) {
      if (interaction.commandName === 'aide') {
        return interaction.reply({ ephemeral: true, content: '**Commandes VR**\n`/groupe` pour chercher des joueurs, `/sortie` pour annoncer une soirée et `/candidature` pour rejoindre la communauté.' });
      }
      if (interaction.commandName === 'candidature') {
        const modal = new ModalBuilder().setCustomId('application:submit').setTitle('Candidature VR');
        const motivation = new TextInputBuilder().setCustomId('motivation').setLabel('Pourquoi rejoindre la communauté ?')
          .setStyle(TextInputStyle.Paragraph).setMinLength(20).setMaxLength(700).setRequired(true);
        const setup = new TextInputBuilder().setCustomId('setup').setLabel('Ton casque et tes jeux VR')
          .setStyle(TextInputStyle.Short).setMaxLength(120).setRequired(true);
        const availability = new TextInputBuilder().setCustomId('availability').setLabel('Tes disponibilités habituelles')
          .setStyle(TextInputStyle.Short).setMaxLength(120).setRequired(false);
        modal.addComponents(
          new ActionRowBuilder().addComponents(motivation),
          new ActionRowBuilder().addComponents(setup),
          new ActionRowBuilder().addComponents(availability)
        );
        return interaction.showModal(modal);
      }

      const channel = interaction.options.getChannel('salon', true);
      if (!channel.isTextBased() || !('send' in channel)) {
        return interaction.reply({ ephemeral: true, content: 'Choisis un salon texte valide.' });
      }
      let embed;
      if (interaction.commandName === 'groupe') {
        const places = interaction.options.getInteger('places');
        embed = new EmbedBuilder().setColor(0x19c6a7).setTitle('🎮 Recherche de joueurs VR')
          .addFields(
            { name: 'Jeu', value: interaction.options.getString('jeu', true), inline: true },
            { name: 'Casque / plateforme', value: interaction.options.getString('casque', true), inline: true },
            { name: 'Quand', value: interaction.options.getString('heure', true), inline: true },
            { name: 'Joueurs recherchés', value: places ? String(places) : 'À préciser dans le fil', inline: true }
          ).setDescription(`Organisé par ${interaction.user}`).setFooter({ text: 'Réponds dans le fil pour rejoindre la session.' });
      } else if (interaction.commandName === 'sortie') {
        const details = interaction.options.getString('details');
        embed = new EmbedBuilder().setColor(0x7c5cff)
          .setTitle(`🌌 ${interaction.options.getString('titre', true)}`)
          .addFields(
            { name: 'Jeu / activité', value: interaction.options.getString('jeu', true), inline: true },
            { name: 'Date et heure', value: interaction.options.getString('quand', true), inline: true }
          ).setDescription([details, `Organisé par ${interaction.user}`].filter(Boolean).join('\n\n'))
          .setFooter({ text: 'Retrouve les participants dans le fil de discussion.' });
      } else return;

      await interaction.deferReply({ ephemeral: true });
      const post = await channel.send({ embeds: [embed] });
      try {
        const thread = await post.startThread({
          name: (interaction.commandName === 'groupe' ? 'Session : ' : 'Participants : ') +
            (interaction.commandName === 'groupe' ? interaction.options.getString('jeu', true) : interaction.options.getString('titre', true)),
          autoArchiveDuration: 1440,
          reason: 'Discussion de la communauté autour d’une activité VR'
        });
        await interaction.editReply(`C’est publié dans ${channel} avec un fil pour organiser la session : ${thread}.`);
      } catch {
        await interaction.editReply(`L’annonce est publiée dans ${channel}, mais je n’ai pas pu créer le fil. Vérifie la permission « Créer des fils publics ».`);
      }
      return;
    }

    if (interaction.isModalSubmit() && interaction.customId === 'application:submit') {
      if (!APPLICATIONS_CHANNEL_ID) {
        return interaction.reply({ ephemeral: true, content: 'Les candidatures ne sont pas encore configurées. Préviens un administrateur.' });
      }
      const channel = await interaction.guild.channels.fetch(APPLICATIONS_CHANNEL_ID).catch(() => null);
      if (!channel?.isTextBased()) {
        return interaction.reply({ ephemeral: true, content: 'Le salon de candidatures est introuvable. Préviens un administrateur.' });
      }
      const embed = new EmbedBuilder().setColor(0xffc857).setTitle('📨 Nouvelle candidature VR')
        .setDescription(`Candidat : ${interaction.user} (\`${interaction.user.id}\`)`)
        .addFields(
          { name: 'Motivation', value: interaction.fields.getTextInputValue('motivation') },
          { name: 'Casque et jeux', value: interaction.fields.getTextInputValue('setup') },
          { name: 'Disponibilités', value: interaction.fields.getTextInputValue('availability') || 'Non renseignées' }
        ).setTimestamp().setFooter({ text: 'En attente de décision des modérateurs' });
      await channel.send({ embeds: [embed], components: [applicationButtons(interaction.user.id)] });
      return interaction.reply({ ephemeral: true, content: 'Candidature envoyée aux modérateurs. Merci !' });
    }

    if (interaction.isButton() && interaction.customId.startsWith('application:')) {
      if (!interaction.memberPermissions?.has(PermissionFlagsBits.ManageGuild)) {
        return interaction.reply({ ephemeral: true, content: 'Seuls les modérateurs du serveur peuvent traiter les candidatures.' });
      }
      const [, decision, applicantId] = interaction.customId.split(':');
      if (!['accept', 'reject'].includes(decision) || !/^\d{17,20}$/.test(applicantId ?? '')) return;
      if (decision === 'accept' && ACCEPTED_ROLE_ID) {
        const member = await interaction.guild.members.fetch(applicantId).catch(() => null);
        if (!member) return interaction.reply({ ephemeral: true, content: 'Cette personne n’est plus sur le serveur. Aucune décision n’a été enregistrée.' });
        await member.roles.add(ACCEPTED_ROLE_ID, `Candidature acceptée par ${interaction.user.tag}`);
      }
      const accepted = decision === 'accept';
      const updated = EmbedBuilder.from(interaction.message.embeds[0])
        .setColor(accepted ? 0x35c98a : 0xe45b66)
        .setFooter({ text: `${accepted ? 'Acceptée' : 'Refusée'} par ${interaction.user.tag}` });
      await interaction.update({ embeds: [updated], components: [new ActionRowBuilder().addComponents(
        new ButtonBuilder().setCustomId(`application:accept:${applicantId}`).setLabel(accepted ? 'Acceptée' : 'Accepter').setStyle(ButtonStyle.Success).setDisabled(true),
        new ButtonBuilder().setCustomId(`application:reject:${applicantId}`).setLabel(accepted ? 'Refuser' : 'Refusée').setStyle(ButtonStyle.Danger).setDisabled(true)
      )] });
      return;
    }
  } catch (error) {
    console.error('Erreur d’interaction :', error);
    const message = 'Action impossible. Vérifie les salons et les permissions du bot, puis réessaie.';
    if (interaction.deferred || interaction.replied) await interaction.editReply(message).catch(() => {});
    else await interaction.reply({ ephemeral: true, content: message }).catch(() => {});
  }
});

client.login(DISCORD_TOKEN);
