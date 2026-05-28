# lamag

[![CI](https://github.com/netos23/lamag/actions/workflows/ci.yml/badge.svg)](https://github.com/netos23/lamag/actions/workflows/ci.yml)

Интерпретатор языка LaMa на базе GraalVM Truffle. Парсер реализован через ANTLR4, фронтенд языка регистрируется как Truffle-язык `lama` и запускается из `Polyglot Context`. Для распространения и для тестов используется бинарь GraalVM **native-image**.

## Оглавление
- [Зависимости](#зависимости)
- [Сборка](#сборка)
- [Запуск](#запуск)
- [Тесты](#тесты)
- [CI](#ci)
- [Вспомогательные скрипты](#вспомогательные-скрипты)

## Зависимости

- JDK 17+ (рекомендуется **GraalVM Community Edition 22.3.x** или новее)
- `native-image` (`gu install native-image` — для GraalVM 22.3)
- Maven 3.8+
- Git (для подмодуля рантайма)
- *Опционально:* `lamac` (Lama 1.30) — нужен для сравнительных performance-тестов

Инициализация сабмодуля (обязательно перед сборкой тестовых фикстур):

```bash
git submodule update --init --recursive
```

## Сборка

### JVM (для отладки и unit-тестов)

```bash
mvn -B package -DskipTests
```

В `target/` появится shaded-jar `lamag-1.0-SNAPSHOT-all.jar`, который можно запускать так:

```bash
java -jar target/lamag-1.0-SNAPSHOT-all.jar path/to/program.lama
```

### Native-image (используется в тестах)

```bash
./scripts/build_native.sh
```

Эквивалентно:

```bash
mvn -B -Pnative -DskipTests package
```

После сборки бинарник лежит в `target/lamag` и не требует JVM.

## Запуск

```bash
target/lamag path/to/program.lama < input.txt
```

Программа читает Lama-исходник напрямую (без предварительной компиляции в байткод).

## Тесты

Все тесты используют фикстуры из подмодуля `third_party/Lama/regression` и `third_party/Lama/performance`. Подготовительные скрипты копируют их в `regression/` и `performance/`.

### 1. JVM unit-тесты

```bash
./test/run_unit_tests.sh
# или
mvn -B test
```

### 2. IO-регрессии (native-image)

Сравнение stdout `lamag` с эталонными `.t` при подаче `.input`:

```bash
./scripts/make_examples.sh
./test/run_io.sh
```

Переменные окружения:
- `FIXTURE_DIR` — каталог с `*.lama` (по умолчанию `regression/`).
- `LAMAG_BIN` — путь к native-бинарю (по умолчанию `target/lamag`).

### 3. Performance (native-image vs lamac)

```bash
./scripts/make_perf.sh
./test/run_performance.sh
```

Скрипт замеряет время `lamag <file>`, `lamac -i <file>` и `lamac -s <file>` для каждого `.lama`. Если `lamac` не найден или установлен `SKIP_LAMAC=true`, замеряется только `lamag`.

Переменные окружения:
- `FIXTURE_DIR` — каталог с `*.lama` (по умолчанию `performance/`).
- `LAMAG_BIN` — путь к native-бинарю.
- `LAMAC_BIN` — путь или имя `lamac` (по умолчанию ищется в PATH).
- `SKIP_LAMAC` — `true`, чтобы пропустить запуски `lamac`.

### Очистка фикстур

```bash
./scripts/clean_temp.sh
```

## Вспомогательные скрипты

- [scripts/](scripts) — сборка native-image, копирование фикстур, очистка, docker-окружение для `lamac`. Подробнее — [scripts/README.MD](scripts/README.MD).
- [docker/](docker) — Dockerfile с предустановленным `lamac`. Подробнее — [docker/README.MD](docker/README.MD).
