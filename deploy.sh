#!/bin/bash

# Скрипт для быстрой загрузки Jopatok на GitHub
# Использование: ./deploy-to-github.sh YOUR_USERNAME

set -e

if [ -z "$1" ]; then
    echo "❌ Использование: $0 YOUR_USERNAME"
    echo "Пример: $0 myusername"
    exit 1
fi

USERNAME=$1
REPO_URL="https://github.com/$USERNAME/Jopatok.git"

echo "🚀 Загрузка Jopatok на GitHub..."
echo "📁 Репозиторий: $REPO_URL"
echo ""

# Проверка наличия git
if ! command -v git &> /dev/null; then
    echo "❌ Git не найден. Установи git."
    exit 1
fi

# Проверка наличия curl
if ! command -v curl &> /dev/null; then
    echo "❌ curl не найден. Установи curl."
    exit 1
fi

cd /storage/emulated/0/termux/Jopatok

# Инициализация git если нужно
if [ ! -d ".git" ]; then
    echo "📦 Инициализация Git..."
    git init
    git add .
    git commit -m "Initial commit: Jopatok v1.0"
fi

# Проверка remote
if ! git remote get-url origin &> /dev/null; then
    echo "🔗 Добавление remote origin..."
    git remote add origin "$REPO_URL"
else
    echo "🔄 Обновление remote origin..."
    git remote set-url origin "$REPO_URL"
fi

# Переименование ветки
git branch -M main 2>/dev/null || true

echo ""
echo "📤 Пуш на GitHub..."
echo "💡 Если запросит пароль — используй Personal Access Token"
echo "📝 Создать токен: https://github.com/settings/tokens/new (scope: repo, workflow)"
echo ""

# Пуш с обработкой ошибок
if git push -u origin main --force; then
    echo ""
    echo "✅ Успешно!"
    echo ""
    echo "📊 Твой репозиторий: https://github.com/$USERNAME/Jopatok"
    echo "🔨 Сборка Actions: https://github.com/$USERNAME/Jopatok/actions"
    echo ""
    echo "🎉 GitHub Actions должен автоматически запустить сборку APK!"
else
    echo ""
    echo "❌ Ошибка пуша. Проверь:"
    echo "   1. Репозиторий создан на GitHub"
    echo "   2. Правильный логин/пароль (или токен)"
    echo ""
    echo "💡 Создай Personal Access Token:"
    echo "   https://github.com/settings/tokens/new"
    echo "   Scope: repo, workflow"
    echo ""
    echo "   Затем используй токен вместо пароля"
    exit 1
fi
