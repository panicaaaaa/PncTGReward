# pncTGReward

Telegram → Minecraft reward plugin. Players DM your Telegram bot `/reward <nickname>` and instantly get configurable rewards in-game. One fat JAR, async DB, customizable messages/titles/sounds, optional Vault & PlayerPoints.

---

## ✨ Features

- **Telegram bot integration** (shaded inside): `/reward <nickname>`
- **One-time claim per TG account per campaign**
- **Async storage** with **SQLite / MySQL / MariaDB**
- **Configurable rewards**:
  - Vault money (optional)
  - PlayerPoints (optional)
  - Console commands list
- **Fully customizable texts** (Minecraft & Telegram), **titles**, **sound**
- **Rate limit** per Telegram user
- **Admin commands**: reload config, check link by `tgId` or `playerName`
- **No LuckPerms required** (only admin node for commands)

---

## 🧩 Requirements

- **Java 16**
- **Paper/Purpur 1.16.5** (or compatible fork)
- Optional: **Vault** + any economy plugin
- Optional: **PlayerPoints**

Everything else (Telegram API, Hikari, JDBC drivers, Adventure if used) is shaded into the JAR.

---

## 🚀 Installation

1. Drop `pncTGReward-<version>.jar` into `plugins/`.
2. Start the server once to generate `config.yml`.
3. Create a Telegram bot with **@BotFather**, copy the **token**.
4. Edit `plugins/pncTGReward/config.yml`:
   - Paste `Telegram.token`
   - Pick DB (`sqlite`/`mariadb`/`mysql`)
   - Adjust rewards/messages/title/sound
5. `/pnctgr reload` (or restart the server).

---

## ⚙️ Configuration

### Database
```yaml
Database:
  Type: sqlite
  sqlite:
    file: plugins/pncTGReward/data.db
  MariaDB/MySQL:
    host: 127.0.0.1
    port: 3306
    database: tgreward
    user: root
    password: ''
```

### Telegram & Economy
```yaml
Telegram:
  token: "PUT_YOUR_BOT_TOKEN"
  campaign-id: "default"      # to allow one claim per campaign
  rate-limit-seconds: 3       # anti-spam per TG user

Economic:
  Vault: true                 # requires Vault + economy plugin
  PlayerPoints: true          # requires PlayerPoints plugin
```

### Reward & Messages
```yaml
#Настройка наград
Reward:
  Vault: 1000
  PlayerPoints: 100
  Commands:
    - 'give [player] diamond_sword'
    -
#Сообщения в игре
Messages:
  prefix: "<gray>[<light_purple>TG</light_purple>]</gray> "
  already-linked: "<red>Этот телеграм уже привязан к другому нику.</red>"
  name-already-linked: "<red>Этот ник уже привязан к другому телеграму.</red>"
  claimed-already: "<yellow>Награда по кампании уже получена.</yellow>"
  player-not-found: "<red>Игрок <white><name></white> не найден.</red>"
  success-chat: "<green>Награда выдана! Спасибо за участие.</green>"
  console-claim-log: "Выдана награда: tg=<tg_id> uuid=<uuid> name=<name> campaign=<campaign>"

#сообщения бота в тг
  telegram:
    usage: "Использование: /reward <ник>"
    ratelimited: "Слишком часто. Подожди пару секунд и попробуй снова."
    claimed-already: "❌ Уже получали награду по этой кампании."
    player-not-found: "❌ Игрок не найден: <name>"
    name-already-linked: "❌ Этот ник уже привязан к другому Telegram."
    tg-already-linked: "❌ Этот Telegram уже привязан к другому нику."
    reward-success: "✅ Награда выдана игроку <name>"
```

### Title & Sound
```yaml
#Тайтл, который вылазиит при получении награды
Title:
  enabled: true
  main: "<gradient:#8a2be2:#ff66ff>Награда получена!</gradient>"
  sub: "<white>Забегай почаще :)</white>"
  fadein: 10
  stay: 50
  fadeout: 10

#Настройка звука при выдаче награды
Sound:
  enabled: true
  name: "ENTITY_PLAYER_LEVELUP"
  volume: 1.0
  pitch: 1.0
```

> All placeholders like `<name>`, `[player]`, `<tg_id>` are replaced automatically.

---

## 🕹️ Usage

### Player flow
1. Player sends your bot a DM:  
   `/reward Steve`
2. Plugin links TG ID ↔ Minecraft UUID (1:1) and, if first time for the campaign:
   - Pays Vault/PlayerPoints
   - Executes configured commands
   - Shows title, plays sound, sends chat message (if online)
3. Bot replies with the configured success/failure text.

### Admin commands
- `/pnctgr reload` – reloads `config.yml`
- `/pnctgr check <tgId|playerName>` – shows link in either direction

Permission:
```
pnctgr.admin   # required for admin commands
```

---

## 🧪 Tips

- To restrict duplicate claims per different promo, change `Telegram.campaign-id`.
- `Reward.Commands` support placeholders: `[player]` and `<name>`.
- For offline players, commands will still execute from console.

---

## 🛠️ Build (Gradle)

- **Java 16** toolchain
- `./gradlew clean shadowJar`
- Output: `build/libs/pncTGReward-<version>.jar` (fat JAR)

All runtime libraries are shaded, except optional server-side plugins (Vault, PlayerPoints).

---

## ❓ FAQ

**Q:** Players get “already linked”  
**A:** TG ID can be linked to only one UUID, and vice versa. This prevents abuse.

**Q:** Can I disable Vault or PlayerPoints?  
**A:** Yes, toggle in `Economic` section.

**Q:** Commands don’t run?  
**A:** Ensure syntax is valid for your server and placeholders `[player]`/`<name>` are present where needed.
