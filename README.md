# GoodOK Messenger

Полнофункциональный мессенджер для Android с WebRTC звонками, поддержкой RuStore Push и мультиязычностью.

## Функции

### Регистрация и авторизация
- Email и пароль
- **Телефон (обязательно)** - обязательное поле при регистрации
- **Подтверждение пароля** - повторный ввод пароля
- **Выбор языка** - 7 языков на выбор
- **Выбор дизайна** - 4 темы оформления
- **Отпечаток пальца** - вход по биометрии

### Сообщения
- Текстовые сообщения
- Отправка фото
- Отправка видео
- Реальные сообщения в реальном времени через Firebase

### Звонки (WebRTC)
- Голосовые звонки
- Видеозвонки
- Бесплатные STUN серверы Google
- Сигнализация через Firebase Realtime Database

### Контакты
- Импорт из телефонной книги
- Определение зарегистрированных пользователей

### Каналы
- Создание каналов
- Подписка на каналы

### Настройки
- Смена языка
- Смена темы
- Push-уведомления
- Вход по отпечатку пальца

## Поддерживаемые языки

1. 🇷🇺 Русский
2. 🇬🇧 English
3. 🇧🇾 Беларуская
4. 🇺🇦 Українська
5. 🇩🇪 Deutsch
6. 🇵🇱 Polski
7. 🇫🇷 Français

## Темы оформления

1. **Классическая** - светлая тема с синими акцентами
2. **Современная** - темная тема с фиолетовыми акцентами
3. **Неон** - темная тема с неоновыми цветами
4. **Детская** - яркая тема для детей

---

## Инструкция по настройке RuStore Push

RuStore Push используется вместо FCM для пользователей в Беларуси и России, где Google FCM заблокирован.

### Шаг 1: Регистрация в RuStore

1. Перейдите на https://www.rustore.ru/
2. Зарегистрируйтесь как разработчик
3. Создайте новое приложение
4. Получите **Project ID** для Push-уведомлений

### Шаг 2: Добавление зависимости

Добавьте в `settings.gradle`:
```gradle
dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://repo.rustore.ru/repository/maven-public/")
        }
    }
}
```

Добавьте в `app/build.gradle`:
```gradle
dependencies {
    implementation 'ru.rustore.sdk:pushclient:2.1.0'
}
```

### Шаг 3: Инициализация в Application

```kotlin
class GoodOKApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Инициализация RuStore Push
        RuStorePushClient.init(
            context = this,
            projectId = "YOUR_PROJECT_ID", // Ваш Project ID из RuStore
            logger = DefaultLogger(true) // Включить логирование
        )
    }
}
```

### Шаг 4: Получение Push-токена

```kotlin
RuStorePushClient.getToken()
    .addOnSuccessListener { token ->
        // Сохраните токен и отправьте на ваш сервер
        savePushToken(token)
    }
    .addOnFailureListener { error ->
        // Обработка ошибки
    }
```

### Шаг 5: Обработка Push-уведомлений

Создайте сервис:

```kotlin
class RuStorePushService : RuStoreMessagingService() {

    override fun onNewToken(token: String) {
        // Новый токен получен
        savePushToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // Показать уведомление
        showNotification(
            title = message.data["title"],
            body = message.data["body"],
            data = message.data
        )
    }
}
```

Добавьте в AndroidManifest.xml:

```xml
<service
    android:name=".services.RuStorePushService"
    android:exported="false">
    <intent-filter>
        <action android:name="ru.rustore.sdk.pushclient.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

### Шаг 6: Отправка Push с сервера

```bash
curl -X POST "https://push.rustore.ru/api/v1/projects/YOUR_PROJECT_ID/messages:send" \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "message": {
      "token": "USER_PUSH_TOKEN",
      "notification": {
        "title": "Новое сообщение",
        "body": "Привет! Как дела?"
      },
      "data": {
        "type": "message",
        "chat_id": "chat_123"
      }
    }
  }'
```

### Важные замечания

1. **RuStore SDK работает только на устройствах с RuStore**
2. **Для эмуляторов и устройств без RuStore используйте FCM**
3. **Project ID уже настроен в приложении**: `aOBS0Uq93YSSduAYzZuxmCaX0GIypOT2EuHkQjZJBtnXLlnFXzWnbiPxpphtWIg9`

---

## Сборка APK

### Требования

- Android Studio Hedgehog или новее
- JDK 17
- Android SDK 34

### Шаги сборки

1. Клонируйте репозиторий
2. Добавьте `google-services.json` в `app/`
3. Откройте проект в Android Studio
4. Синхронизируйте Gradle
5. Build -> Build Bundle(s) / APK(s) -> Build APK(s)

### Подпись APK

Создайте файл `keystore.properties`:
```properties
storePassword=your_password
keyPassword=your_key_password
keyAlias=your_key_alias
storeFile=path/to/keystore.jks
```

---

## Структура проекта

```
app/src/main/java/com/goodok/app/
├── data/
│   ├── local/          # Локальное хранилище
│   ├── model/          # Модели данных
│   └── remote/         # Firebase сервисы
├── ui/
│   ├── auth/           # Авторизация
│   ├── main/           # Главный экран
│   ├── chats/          # Чаты
│   ├── calls/          # Звонки
│   ├── contacts/       # Контакты
│   ├── channels/       # Каналы
│   └── settings/       # Настройки
├── services/           # Сервисы
├── webrtc/            # WebRTC менеджер
└── util/              # Утилиты
```

---

## Troubleshooting

### Ошибка "auth credential is incorrect"
- Проверьте правильность email и пароля
- Убедитесь, что пользователь зарегистрирован

### Контакты не импортируются
- Проверьте разрешение на чтение контактов
- На Android 13+ запросите разрешение runtime

### Звонки не работают
- Проверьте разрешения на микрофон и камеру
- Убедитесь в стабильности интернет-соединения

### Push-уведомления не приходят
- Проверьте токен устройства
- Убедитесь, что RuStore установлен (для RuStore Push)
- Для FCM: проверьте google-services.json

---

## Лицензия

MIT License

---

## Автор

GoodOK Team
