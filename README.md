![DiscordUtils banner](https://github.com/user-attachments/assets/e0ef9048-17ce-4383-b84c-ca5ef383d292)

<p align="center">
 <img src="https://img.shields.io/badge/plugin%20version-1.1-blue?style=for-the-badge"><img src="https://img.shields.io/badge/Minecraft%20Version-1.19%2B-green?style=for-the-badge&labelColor=gray"><img src="https://img.shields.io/github/license/ItsAndrew1/DiscordUtils?style=for-the-badge&label=License&color=purple"> <br>
 <a href="https://youtu.be/cGq81EZOdig">
  <img src="https://img.shields.io/badge/showcase-video-red?style=for-the-badge&logo=youtube">
 </a>
</p>

**DiscordUtils** is a *lightweight*, *discord utility minecraft plugin*, designed to help every server owner *connect* the players with **his discord server**. **Control the punishments** of the players, track them using **logs** in specific channels, let players **appeal** their punishments and much more!

---

## ⚙️ Features
 - **Fully configurable GUI**, with info items and more!
 - **Custom block** for the *discord-item* in the GUI
 - Fully configurable **sounds**
 - Multiple ways of **displaying the discord link to the player**
 - Fully configurable and toggleable **discord bot** 
 - Clean **punishments system**
 - Clean **appealing system** (_via your discord server_)
 - Clean **logs system** (_also via your discord server_)
 - **Simple configuration**  
And **more**! Check `config.yml` for every feature!  

🔴 **DiscordUtils** works best with a *PAPER server*, because it uses some stuff which work **ONLY** on it.  
    For more information about the plugin, check the **showcase video**.

---

## 🤖 Discord Bot
 As I said earlier, **DiscordUtils** can use a _discord bot_ in order to track **the players** and **their punishments** from your Discord Server. Despite this, the feature is **toggleable**, although the plugin _will lose_ some of the main functions (e.g. _Punishments_) <br>
 Configuring the bot is fairly _easy and simple_. Here are **the main steps**:
 1️⃣ Go to the [Discord Developer Portal](https://discord.com/developers/home) and **create an application**. 
 
 2️⃣ Go to the **bot section**, fill in the _username, description, icon, banner_ and copy the **BOT TOKEN**. Also, *enable all those 3 intents* (Presence, Server Members and Message Content).
 
 3️⃣ In the **botconfig.yml** file, fill in the _bot-token_ section with that bot token.
 
 4️⃣ After this, _open Discord_, **right click on your server**, copy the _Guild ID_ and paste it in the _guild-id_ section (in **botconfig.yml** file).

 ‼️Last but not least, head over to the **config.yml** file and you should see the **open-discord-bot** section. If you have a *Network of Server*, you need the plugin on all of them + a **MySQL Database** (usually hosts give you like 2 or 3 of them). So, in order to **not override the bot**, set that value to **TRUE** on *ONLY ONE* server, and *leave the rest* to **FALSE**  (by default).
<br>

Now let's talk about **how you get the bot on your Discord Server**:
1️⃣ Head back to the [Discord Developer Portal](https://discord.com/developers/home), click on *your app* and head over to the **OAuth2 section**. 

2️⃣ At the *URL Generator*, select **bot** and **application.commands**.

3️⃣ Further down, select **Administrator** (_the bot needs to access the player's roles/permissions and more_).

4️⃣ At the bottom, **copy the generated link**, paste it in your browser and *follow the instructions* to invite it to your server.

I also told various instructions in both **.yml files**, so make sure to read them thorough. 

---

## 🪄 Commands
| Command                | Description                                       | Permission           |
|------------------------|---------------------------------------------------|----------------------|
| `/dcutils mainconfig`   | Opens up the main configuration.                        | `discordutils.commands.mainconfig` |
| `/dcutils punishments` | Opens up the Punishments Menu | `discordutils.commands.punishments` |
| `/dcutils reload`      | Reloads the configuration files.                      | `discordutils.commands.reload` |
| `/dcutils help`        | Opens the *help manual*                           | `discordutils.commands.help` |
| `/discord`             | Opens the discord GUI                             | `discordutils.discord`   |
| `/verify` | Starts the verification process | `discordutils.verify` 
| `/unverify` | Unverifies the player | `discordutils.unverify`
| `/history` | Opens the player's punishment history | `discordutils.viewhistory`

---

## 🔐 Permissions
| Permission                | Description                                       
|------------------------|---------------------------------------------------|
| `discordutils.commands` | Allows the player to use the configuration commands
| `discordutils.commands.reload` | Allows the player to reload the config. files
| `discordutils.commands.help` | Allows the player to open the _Help Manual_
| `discordutils.commands.mainconfig` | Allows the player to open the main config. menu
| `discordutils.commands.punishments` | Allows the player to open the _Punishments Menu_
| `discordutils.punishments.add` | Allows the player to add punishments
| `discordutils.punishments.add.permban` | Allows the player to add a permanent ban
| `discordutils.punishments.add.tempban` | Allows the player to add a temporary ban
| `discordutils.punishments.add.kick` | Allows the player to apply a kick
| `discordutils.punishments.add.permbanwarn` | Allows the player to apply a permanent ban warn
| `discordutils.punishments.add.tempbanwarn` | Allows the player to apply a temporary ban warn
| `discordutils.punishments.add.permmute` | Allows the player to apply a permanent mute/timeout
| `discordutils.punishments.add.tempmute` | Allows the player to apply a temporary mute/timeout
| `discordutils.punishments.add.permmutewarn` | Allows the player to apply a permanent mute/timeout warning
| `discordutils.punishments.add.tempmutewarn` | Allows the player to apply a temporary mute/timeout warning
| `discordutils.punishments.remove` | Allows the player to remove punishments
| `discordutils.punishments.remove.permban` | Allows the player to remove a permanent ban
| `discordutils.punishments.remove.tempban` | Allows the player to remove a temporary ban
| `discordutils.punishments.remove.kick` | Allows the player to remove a kick
| `discordutils.punishments.remove.permbanwarn` | Allows the player to remove a permanent ban warning
| `discordutils.punishments.remove.tempbanwarn` | Allows the player to remove a temporary ban warning
| `discordutils.punishments.remove.permmute` | Allows the player to remove a permanent mute/timeout
| `discordutils.punishments.remove.tempmute` | Allows the player to remove a temporary mute/timeout
| `discordutils.punishments.remove.permmutewarn` | Allows the player to remove a permanent mute/timeout warning
| `discordutils.punishments.remove.tempmutewarn` | Allows the player to remove a temporary mute/timout warning
| `discordutils.punishments.playerhistory` | Allows the player to view other players` punishment history

---

## 📁 Configuration Files
**DiscordUtils** uses 2 configuration files:
-  `config.yml`, in which you can _configure_ the whole plugin. Sounds, GUI, tasks and **much more**.
- `botconfig.yml`, where you can _configure_ the discord bot. Discord roles, channels, messages and **much more**.

---

## ❤️ Credits and License
**License**: MIT license  
**DiscordUtils** was developed and tested by *\_ItsAndrew_*  
Special thanks to everyone who help, test and, **most importantly**, give *feedback*!
