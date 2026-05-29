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
- **GraalVM CE 25.x с `native-image`** (например `brew install --cask graalvm-jdk`) для сборки нативного бинаря и интеграционных тестов. Параметр `graalvm.version` в `pom.xml` синхронизирован с 25.0.3, исходники собираются как JPMS-модуль `pro.fbtw.lamag` (`src/main/java/module-info.java`), и native-image берёт зависимости с module-path.
- Maven 3.8+
- Git (для подмодуля рантайма)
- *Опционально:* `lamac` (Lama 1.30) — нужен для сравнительных performance-тестов

> **Оптимизирующий рантайм.** Чтобы Truffle компилировал гостевой код (а не только интерпретировал), на module/class-path должен быть `org.graalvm.truffle:truffle-runtime` — он подключён зависимостью со scope `runtime`. Без него движок печатает `No optimizing Truffle runtime found ...` и опции вроде `engine.TraceCompilation` вообще не существуют. Unit-тесты (`mvn test`) запускаются по class-path (`<useModulePath>false</useModulePath>` в surefire), поэтому язык находится через сгенерированный `META-INF/services` и одинаково работает на обычном JDK и на GraalVM JDK.

Если `native-image` не найден при запуске `./scripts/build_native.sh`, передайте путь к GraalVM через `JAVA_HOME` — скрипт автоматически подцепит `$JAVA_HOME/bin`:

```bash
JAVA_HOME=/opt/homebrew/opt/graalvm ./scripts/build_native.sh
```

Инициализация сабмодуля (обязательно перед сборкой тестовых фикстур):

```bash
git submodule update --init --recursive
```

## Сборка

### JVM (для отладки и unit-тестов)

```bash
mvn -B package -DskipTests
```

В `target/` появятся скомпилированный модуль `target/classes` и его рантайм-зависимости
в `target/modules` (включая оптимизирующий `truffle-runtime`). Запуск на JVM идёт с
module-path — Truffle поставляется как Multi-Release JAR, поэтому склейка в один uber-jar
ломает инициализацию рантайма:

```bash
java --module-path target/modules:target/classes \
     --enable-native-access=org.graalvm.truffle \
     -m pro.fbtw.lamag/pro.fbtw.lamag.Main path/to/program.lama
# или просто:
./scripts/lamag_jvm.sh path/to/program.lama
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
- `LAMAG_SKIP_TESTS` — список пропускаемых тестов (через пробел). По умолчанию пропускаются неподдерживаемые фичи (как и в эталонной Truffle-реализации): `test092 test094 test095 test096 test098 test105 test111` — пользовательские инфиксные операторы (092/095/098), захват изменяемых локальных переменных по значению (094/096), eta-расширение (105) и точный формат сообщения об ошибке `undefined name` (111). Поставьте `LAMAG_SKIP_TESTS=""`, чтобы прогнать их тоже.

### 3. Performance (native-image vs lamac)

> `performance/Sort.lama` сортирует список из 10 000 элементов рекурсивным
> bubble-sort без хвостовой рекурсии, поэтому удерживает живыми все
> промежуточные списки (~4.7 ГБ). На JVM с G1 (`scripts/lamag_jvm.sh`, `-Xmx8g`)
> это ~80 с; нативный бинарь использует Serial GC (единственный в native-image
> CE) и считает дольше (~3–4 мин), но завершается корректно.

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

### 4. JVM-режим (без native-image)

Альтернатива native-бинарю — гонять те же сьюты через shaded jar и обычный JVM. Полезно, когда `native-image` не установлен или нужно сравнить поведение JIT/AOT.

```bash
./scripts/build_jvm.sh         # mvn package -> target/classes + target/modules
./scripts/make_examples.sh
./test/run_io_jvm.sh

./scripts/make_perf.sh
./test/run_performance_jvm.sh
```

`run_io_jvm.sh` и `run_performance_jvm.sh` — тонкие обёртки, которые подменяют `LAMAG_BIN` на `scripts/lamag_jvm.sh` (запускает интерпретатор с module-path `java --module-path target/modules:target/classes -m pro.fbtw.lamag/...`) и делегируют в обычные `run_io.sh` / `run_performance.sh`. Все их переменные окружения (`FIXTURE_DIR`, `LAMAC_BIN`, `SKIP_LAMAC` и т.д.) работают как обычно. Дополнительно:

- `LAMAG_MODULES` / `LAMAG_CLASSES` — пути к module-path (по умолчанию `target/modules` и `target/classes`).
- `JAVA` — путь к `java` (по умолчанию `$JAVA_HOME/bin/java` или из PATH).
- `JAVA_OPTS` — дополнительные опции JVM (например `-Xmx4g` для тяжёлых программ вроде `Sort.lama`).

Оптимизирующий рантайм Truffle подключается зависимостью `org.graalvm.truffle:truffle-runtime` (scope `runtime`); без неё движок откатывается на интерпретатор без JIT.

### Очистка фикстур

```bash
./scripts/clean_temp.sh
```

## CI

Workflow [.github/workflows/ci.yml](.github/workflows/ci.yml) разбит на две независимые джобы:

1. **`unit-tests`** — поднимает обычный Liberica JDK 17 (без встроенного Truffle) и гоняет `mvn -B test`. Под обычным JDK наш `truffle-api` с classpath регистрируется без проблем.
2. **`integration-tests`** — зависит от первой джобы, поднимает GraalVM Community 25 (с встроенным `native-image`), ставит Lama 1.30 через opam, и прогоняет:
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
| lamag           | 212.46                 | +23%   |
| lamac -i        | 574.08                 | +58%   |