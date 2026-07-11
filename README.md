# Task Manager

Менеджер задач: позволяет создавать задачи, назначать исполнителей, менять статусы и группировать задачи метками.
Для работы с приложением требуется регистрация и аутентификация.

### Hexlet tests and linter status:
[![Actions Status](https://github.com/SaintCap/java-project-99/actions/workflows/hexlet-check.yml/badge.svg)](https://github.com/SaintCap/java-project-99/actions)
[![Checkstyle](https://github.com/SaintCap/java-project-99/actions/workflows/checkstyle.yml/badge.svg)](https://github.com/SaintCap/java-project-99/actions/workflows/checkstyle.yml)

## Запуск

```bash
./gradlew bootRun
```

Приложение будет доступно на `http://localhost:8080`.

## Документация API

Интерактивная документация (Swagger UI): `http://localhost:8080/swagger-ui.html`

## Проверки

```bash
# тесты
./gradlew test

# проверка стиля кода
./gradlew checkstyleMain checkstyleTest
```