# GoodOK Messenger

Мессенджер для Android с поддержкой RuStore Push уведомлений.

## Особенности

- 🚀 **Real-time мессенджер** с Firebase Realtime Database
- 📱 **RuStore Push уведомления** - работает без Google Play Services
- 🎨 **4 темы оформления**: Classic, Modern, Neon, Childish
- 🌍 **10 языков**: English, Русский, Беларуская, Українська, Deutsch, Français, Español, Português, 中文, English (UK)
- 💎 **Premium подписки** через Google Play Billing
- 📞 **Аудио/Видео звонки** (требует настройки WebRTC сервера)
- 📺 **Каналы** как в Telegram

## PUSH уведомления через RuStore

### Настройка RuStore Push

1. Создайте проект в [RuStore Developer Console](https://console.rustore.ru/)
2. Включите Push Notifications в настройках проекта
3. Скопируйте Project ID
4. Добавьте Project ID в `app/build.gradle`:
   ```gradle
   buildConfigField "String", "RUSTORE_PROJECT_ID", "\"ВАШ_PROJECT_ID\""
   ```

### Отправка PUSH с сервера

```bash
curl -X POST "https://push.rustore.ru/api/v1/projects/ВАШ_PROJECT_ID/messages" \
  -H "Authorization: Bearer ВАШ_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "message": {
      "token": "токен_устройства",
      "data": {
        "title": "Имя отправителя",
        "body": "Текст сообщения",
        "senderId": "uid_отправителя",
        "type": "message"
      }
    }
  }'
```

## Сборка

1. Клонируйте репозиторий
2. Добавьте `google-services.json` в папку `app/`
3. Соберите проект:
   ```bash
   ./gradlew assembleDebug
   ```

## Firebase (без FCM)

Проект использует Firebase только для:
- **Authentication** - авторизация пользователей
- **Realtime Database** - хранение сообщений и данных

Эти сервисы работают в Республике Беларусь.

## Структура проекта

```
app/src/main/java/com/goodok/app/
├── billing/          # Google Play Billing
├── data/
│   ├── local/        # Room Database, Preferences
│   ├── model/        # Data models
│   └── remote/       # Firebase Service
├── services/
│   ├── RuStorePushService.kt  # RuStore Push
│   └── PushManager.kt         # Управление PUSH
├── ui/               # Activities и Adapters
└── util/             # Helpers
```

## Требования

- Android 7.0+ (API 24)
- JDK 17
- Android Studio Hedgehog или новее

## Лицензия

MIT License
