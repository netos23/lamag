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

- **Обычный JDK 17+** (Liberica, Temurin, OpenJDK) для `mvn test` и для разработки в IDE.
- **GraalVM CE 22.3.x + native-image** для сборки нативного бинаря и интеграционных тестов.
- Maven 3.8+
- Git (для подмодуля рантайма)
- *Опционально:* `lamac` (Lama 1.30) — нужен для сравнительных performance-тестов

> **Важно:** при запуске `mvn test` под GraalVM JDK 17 встроенный модуль `org.graalvm.truffle` маскирует наш classpath-зависимый `truffle-api`, и ServiceLoader не находит `LamaLanguageProvider` (ошибка `Installed languages are: []`). Поэтому для unit-тестов используется обычный JDK, а GraalVM подключается только для нативной сборки.

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

## CI

Workflow [.github/workflows/ci.yml](.github/workflows/ci.yml) разбит на две независимые джобы:

1. **`unit-tests`** — поднимает обычный Liberica JDK 17 (без встроенного Truffle) и гоняет `mvn -B test`. Под обычным JDK наш `truffle-api` с classpath регистрируется без проблем.
2. **`integration-tests`** — зависит от первой джобы, поднимает GraalVM Community 17 + `native-image`, ставит Lama 1.30 через opam, и прогоняет:
   - `./scripts/build_native.sh` — сборка native-image бинаря (тесты пропускаются: `SKIP_TESTS=true`);
   - `./scripts/make_examples.sh` / `./scripts/make_perf.sh` — подготовка фикстур;
   - `./test/run_io.sh` — IO-регрессии на native-бинаре;
   - `./test/run_performance.sh` — замеры производительности.

Готовый бинарь `target/lamag` загружается артефактом `lamag-linux-x64`.

## Разработка в IntelliJ IDEA

Грамматика `src/main/antlr/.../Lama.g4` компилируется плагином `antlr4-maven-plugin` в `target/generated-sources/antlr4`, и `build-helper-maven-plugin` подключает этот каталог как Source Root, чтобы IDEA видела генерированные классы парсера/лексера сразу после Maven Reimport.

В `.idea/runConfigurations/` лежат две шаренные конфигурации:

- **lamag** — запуск `pro.fbtw.lamag.Main` с предзапуском `mvn generate-sources`, поэтому грамматика автоматически пересобирается перед каждым запуском приложения.
- **lamag tests** — запуск JUnit-тестов с тем же `generate-sources` перед стартом.

После изменения `Lama.g4` обычный «Run» в IDE сначала вызовет ANTLR и подхватит обновлённый парсер без ручных шагов.

## Вспомогательные скрипты

- [scripts/](scripts) — сборка native-image, копирование фикстур, очистка, docker-окружение для `lamac`. Подробнее — [scripts/README.MD](scripts/README.MD).
- [docker/](docker) — Dockerfile с предустановленным `lamac`. Подробнее — [docker/README.MD](docker/README.MD).



| Интерпретатор   | Время выполнения (сек) | Score~ |
|-----------------|------------------------|--------|
| lamar-verified* | 105.12                 | -      |
| lamar*          | 114.84                 | +9%    |
| lamac -s        | 171.67                 | +50%   |
| lamac -i        | 574.08                 | +234%  |