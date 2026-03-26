# 📤 Инструкция по загрузке на GitHub

## Шаг 1: Создай репозиторий на GitHub

1. Зайди на https://github.com
2. Нажми **+** → **New repository**
3. Название: `Jopatok`
4. Описание: "🎬 TikTok-style local video player for Android"
5. **НЕ** ставь галочки на "Initialize with README"
6. Нажми **Create repository**

## Шаг 2: Привяжи удалённый репозиторий

```bash
cd /storage/emulated/0/termux/Jopatok

# Замени YOUR_USERNAME на свой логин GitHub
git remote add origin https://github.com/YOUR_USERNAME/Jopatok.git

# Переименуй ветку в main
git branch -M main

# Запуш код
git push -u origin main
```

## Шаг 3: Проверь GitHub Actions

1. Зайди в свой репозиторий на GitHub
2. Перейди во вкладку **Actions**
3. Должен запуститься workflow **Android Build**
4. После завершения скачай APK из артефактов

## Шаг 4: (Опционально) Создай релиз

```bash
# Создай тег версии
git tag v1.0.0

# Запуш тег
git push origin v1.0.0
```

После пуша тега автоматически создастся релиз с APK!

---

## 🔧 Если нужны права доступа

Создай Personal Access Token:
1. GitHub → Settings → Developer settings → Personal access tokens
2. Generate new token (classic)
3. Выбери scope: `repo`, `workflow`
4. Скопируй токен
5. При пуше используй: `https://YOUR_USERNAME:TOKEN@github.com/YOUR_USERNAME/Jopatok.git`

## 📱 Скачать APK

После успешной сборки:
- Зайди в **Actions** → выбери запуск → скачай артефакт `app-debug.zip`
- Или в **Releases** (если создал тег) → скачай `app-release.apk`

---

**Готово!** 🎉
