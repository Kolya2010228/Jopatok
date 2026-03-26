# 🚀 Автоматические релизы Jopatok

## 📋 Как работает

### 1. Автоматическая сборка при пуше
При каждом пуше в ветку `main` автоматически:
- ✅ Собирается Debug APK
- ✅ Загружается в артефакты (30 дней)

### 2. Ручной запуск релиза (workflow_dispatch)

Через GitHub UI:
1. Зайди в **Actions** → **Android Build & Release**
2. Нажми **Run workflow**
3. Выбери тип версии:
   - **patch** → 0.0.1 → 0.0.2 (багфиксы)
   - **minor** → 0.0.1 → 0.1.0 (новые функции)
   - **major** → 0.0.1 → 1.0.0 (большие изменения)
4. Нажми **Run workflow**

Workflow автоматически:
- ✅ Обновит версию в `build.gradle`
- ✅ Создаст тег (например, `v0.0.2`)
- ✅ Запушит изменения
- ✅ Соберёт Release APK и AAB
- ✅ Создаст релиз на GitHub

### 3. Релиз по тегу

При пуше тега (например, `v0.0.1`):
- ✅ Собирается Release APK
- ✅ Собирается AAB (Android App Bundle)
- ✅ Создаётся релиз с changelog
- ✅ Загружаются файлы в релиз

---

## 📝 Команды

### Создать релиз вручную (через API)

```bash
# Patch версия (0.0.1 → 0.0.2)
curl -X POST \
  -H "Authorization: token ghp_YOUR_TOKEN" \
  -H "Accept: application/vnd.github.v3+json" \
  https://api.github.com/repos/Kolya2010228/Jopatok/actions/workflows/android-build.yml/dispatches \
  -d '{"ref":"main","inputs":{"version_type":"patch"}}'

# Minor версия (0.0.1 → 0.1.0)
curl -X POST \
  -H "Authorization: token ghp_YOUR_TOKEN" \
  -H "Accept: application/vnd.github.v3+json" \
  https://api.github.com/repos/Kolya2010228/Jopatok/actions/workflows/android-build.yml/dispatches \
  -d '{"ref":"main","inputs":{"version_type":"minor"}}'

# Major версия (0.0.1 → 1.0.0)
curl -X POST \
  -H "Authorization: token ghp_YOUR_TOKEN" \
  -H "Accept: application/vnd.github.v3+json" \
  https://api.github.com/repos/Kolya2010228/Jopatok/actions/workflows/android-build.yml/dispatches \
  -d '{"ref":"main","inputs":{"version_type":"major"}}'
```

### Создать тег вручную

```bash
git tag v0.0.1
git push origin v0.0.1
```

---

## 📊 Версионирование

| Тип | Было | Стало | Когда использовать |
|-----|------|-------|-------------------|
| patch | 0.0.1 | 0.0.2 | Багфиксы, мелкие правки |
| minor | 0.0.1 | 0.1.0 | Новые функции |
| major | 0.0.1 | 1.0.0 | Большие изменения, релиз |

---

## 🔗 Ссылки

- **Репозиторий**: https://github.com/Kolya2010228/Jopatok
- **Actions**: https://github.com/Kolya2010228/Jopatok/actions
- **Releases**: https://github.com/Kolya2010228/Jopatok/releases

---

**Jopatok** © 2026 - Автоматические релизы 🚀
