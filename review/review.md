# Взаимное рецензирование КТ3

Автор работы и рецензии: Пестряков Максим Александрович, ИКБО-31-24, GitHub `kokuyouu`.

Рецензируемый и рецензент моей работы: Котельников Дмитрий Владимирович, ИКБО-31-24, GitHub `kotel24`.

Использован способ рецензирования через GitHub Issues. Ниже сохранены полные тексты опубликованных замечаний, чтобы файл можно было проверить без перехода по ссылкам. Дата подготовки файла: 24.11.2026.

## Кого и какой код я рецензировал

- Репозиторий Котельникова: <https://github.com/kotel24/ar-trainer>.
- Основной проверенный коммит: [`994846f36509bf8bf74992fe481fd2a76dc72f08`](https://github.com/kotel24/ar-trainer/commit/994846f36509bf8bf74992fe481fd2a76dc72f08), замечания № 1, 2, 4 и 5.
- Проверка загрузки зависимостей: [`1426dd20ceb94b141ac5abcde0348d84ce4e2d4f`](https://github.com/kotel24/ar-trainer/commit/1426dd20ceb94b141ac5abcde0348d84ce4e2d4f), замечание № 3. Это версия, указанная в опубликованном замечании.
- Тема: тренажёр дополненной реальности для промышленных предприятий; проверяется Java-ядро сценария станка 2Н135.

## Что проверял

1. Сопоставил `models/classes.puml` с объявлениями классов и связями в `src/java`: сущности, `ScenarioGraph`, `AttemptChecker`, команды, правила. У `Dependency`, `CheckRule` и `ScenarioGraph` обнаружены места, где название затрудняет чтение направления и условий проверки; конкретные замечания приведены ниже.
2. Проверил поведение наследников `CheckRule`: `DangerousDependencyRule` обнаруживает опасную невыполненную зависимость, `OrderRule` — нарушение порядка при первом выполнении шага. Вызов выполняется через общий тип. Проверил интерфейс `TrainerCommand` и реализации `DemoCommand`, `CheckCommand`.
3. Проверил коллекции `List<Step>`, `Map<String, Step>`, `Map<String, List<Dependency>>`, `Set<String>` и `PriorityQueue<Step>` в графе сценария; закрытые поля и копирование входных списков.
4. Проверил читаемость параметров, названий и разделение файлов; замечания № 1, 2, 4 и 5 относятся к этим местам.
5. Повторил команды `src/java/README.md` на отдельной копии исходников с JDK 23.0.2, совместимым с указанным JDK 21: `javac -encoding UTF-8 -d out *.java`, затем `java -cp out Trainer demo data 1` и `java -cp out Trainer check data 2`. Данные для проверки копировались во временный каталог. Обучение выводит 12 шагов; попытка 2 получает 75 баллов, результат «не сдано», что совпадает с README.
6. Изучил фильтрацию внешних ссылок в `CsvStore.graph()`: неизвестный идентификатор обязательного шага не должен удалять требование из графа. Это содержательная ошибка из замечания № 3.

## Мои опубликованные замечания

### Замечание № 1 — criticalSteps() стоит переименовать в criticalStepIds()

Место: `src/java/ScenarioGraph.java:32`. Опубликовано 21.11.2026: <https://github.com/kotel24/ar-trainer/issues/1>.

Полный текст:

> https://github.com/kotel24/ar-trainer/blob/994846f36509bf8bf74992fe481fd2a76dc72f08/src/java/ScenarioGraph.java#L32
> Метод возвращает набор строковых ID, а соседний steps() возвращает объекты Step

Предлагаемое исправление: переименовать метод в `criticalStepIds()`, чтобы тип возвращаемых элементов был понятен из имени.

Ответ автора (<https://github.com/kotel24/ar-trainer/issues/1#issuecomment-7061917791>):

> Переименовал `criticalSteps()` в `criticalStepIds()` и обновил вызовы. Исправление в ветке `review-fixes`: https://github.com/kotel24/ar-trainer/commit/e0933309d258dcefc0291b1cb8c81793d43497bc

Состояние Issue на 24.11.2026: CLOSED.

### Замечание № 2 — Параметры CheckRule.matches() скрывают условия проверки

Место: `src/java/CheckRule.java:8`. Опубликовано 21.11.2026: <https://github.com/kotel24/ar-trainer/issues/2>.

Полный текст:

> https://github.com/kotel24/ar-trainer/blob/994846f36509bf8bf74992fe481fd2a76dc72f08/src/java/CheckRule.java#L8
> Предлагаю заменить missing на hasUnperformedPrerequisite, dangerous на hasUnmetCriticalPrerequisite, а firstExecution на firstOccurrenceOfStep

Предлагаемое исправление: использовать названия, выражающие проверяемые условия, и обновить места вызова.

Ответ автора (<https://github.com/kotel24/ar-trainer/issues/2#issuecomment-7061923726>):

> Уточнил параметры `CheckRule.matches()` и обновил места вызова. [Коммит с исправлением](https://github.com/kotel24/ar-trainer/commit/2c7889594b4576cdc21c1aaefe0900161f145839).

Состояние Issue на 24.11.2026: CLOSED.

### Замечание № 3 — Опечатка в ID шага молча отключает обязательную зависимость

Место: `src/java/CsvStore.java:56`. Опубликовано 21.11.2026: <https://github.com/kotel24/ar-trainer/issues/3>.

Полный текст:

> https://github.com/kotel24/ar-trainer/blob/1426dd20ceb94b141ac5abcde0348d84ce4e2d4f/src/java/CsvStore.java#L56
> CsvStore отбрасывает зависимость с неизвестным required_step_id. В проверенном сценарии попытка вместо остановки получает «сдано».

Предлагаемое исправление: проверять существование обоих шагов при загрузке, отклонять неверную зависимость с диагностикой вместо удаления из графа.

Ответ автора (<https://github.com/kotel24/ar-trainer/issues/3#issuecomment-7061925428>):

> Добавил проверку идентификаторов шагов при загрузке `dependencies.csv`. [Коммит с исправлением](https://github.com/kotel24/ar-trainer/commit/d606d9c132789fc3c0be5f9590379b8611ce6771).

Состояние Issue на 24.11.2026: CLOSED.

### Замечание № 4 — Уточнить имена полей в Dependency

Место: `src/java/Dependency.java:1`. Опубликовано 21.11.2026: <https://github.com/kotel24/ar-trainer/issues/4>.

Полный текст:

> https://github.com/kotel24/ar-trainer/blob/994846f36509bf8bf74992fe481fd2a76dc72f08/src/java/Dependency.java#L1
> stepId – шаг, который зависит от другого, а requiredId – шаг, который должен быть выполнен раньше. Предлагаю dependentStepId и requiredStepId, чтобы направление зависимости читалось из объявления

Предлагаемое исправление: назвать поля `dependentStepId` и `requiredStepId`, обновить обращения в графе и проверке попытки.

Ответ автора (<https://github.com/kotel24/ar-trainer/issues/4#issuecomment-7061925750>):

> Уточнил имена полей `Dependency` и обновил их использования. [Коммит с исправлением](https://github.com/kotel24/ar-trainer/commit/d0d5572ca4d45f7b7cf47b21bcf02272385e5582).

Состояние Issue на 24.11.2026: CLOSED.

### Замечание № 5 — Разнести реализации CheckRule по отдельным файлам

Место: `src/java/CheckRule.java:15`, создание объектов — `src/java/AttemptChecker.java`. Опубликовано 21.11.2026: <https://github.com/kotel24/ar-trainer/issues/5>.

Полный текст:

> https://github.com/kotel24/ar-trainer/blob/994846f36509bf8bf74992fe481fd2a76dc72f08/src/java/CheckRule.java#L15
> DangerousDependencyRule и OrderRule объявлены в CheckRule.java, а создаются в AttemptChecker.java.

Предлагаемое исправление: вынести обе реализации в одноимённые файлы, сохранив иерархию и поведение.

Ответ автора (<https://github.com/kotel24/ar-trainer/issues/5#issuecomment-7061926132>):

> Вынес `DangerousDependencyRule` и `OrderRule` в отдельные файлы. [Коммит с исправлением](https://github.com/kotel24/ar-trainer/commit/b1e5d0ce63391243b1969eca9d63433ec7ac1de6).

Состояние Issue на 24.11.2026: CLOSED.

## Полученная рецензия на мой код

Репозиторий: <https://github.com/kokuyouu/practice>. Рецензент — Котельников Дмитрий Владимирович. Проверенный коммит: [`04f56a4eb05058dab43b197d4d0966e34397cd6d`](https://github.com/kokuyouu/practice/commit/04f56a4eb05058dab43b197d4d0966e34397cd6d), 21.11.2026.

Все три замечания исправлены отдельным коммитом [`ea222cb`](https://github.com/kokuyouu/practice/commit/ea222cb) от 23.11.2026 — `fix: validate inputs and index campaign data (#1, #2, #3)`. Он отделён от исходной реализации и от обновления отчёта. Тексты ниже скопированы полностью.

### Полученное замечание № 1 — `readiness` молча игнорирует `--order`

Источник: <https://github.com/kokuyouu/practice/issues/1>. Автор: `kotel24`, 21.11.2026.

> На штатных данных запустил `readiness --stage S1 --order O2`. Java завершилась с кодом `0` и сообщила, что этап `S1` готов к согласованию, хотя он относится к заказу `O1`. Python-версия отклонила ту же команду с кодом `2`.
>
> Предлагаю проверять допустимые параметры для каждой команды. Для `readiness` параметр `--order` должен приводить к понятной ошибке и коду `2`. [CommandLine.java, строки 27–55](https://github.com/kokuyouu/practice/blob/04f56a4eb05058dab43b197d4d0966e34397cd6d/src/java/main/CommandLine.java#L27-L55) · [Main.java, строка 58](https://github.com/kokuyouu/practice/blob/04f56a4eb05058dab43b197d4d0966e34397cd6d/src/java/Main.java#L58)

**Исправлено в `ea222cb`.** В `src/java/main/CommandLine.java` добавлены учёт переданных параметров и `validateOptions()`: каждой команде разрешены только её параметры; общие `--data` и `--json` сохранены. `readiness --stage S1 --order O2` возвращает код 2, пустой stdout и сообщение `Ошибка: параметр --order недопустим для команды readiness`. Восемь сочетаний недопустимых параметров покрыты регрессионными тестами.

Состояние Issue на 24.11.2026: CLOSED.

### Полученное замечание № 2 — Повторяющееся поле `status` в JSON меняет результат проверки

Источник: <https://github.com/kokuyouu/practice/issues/2>. Автор: `kotel24`, 21.11.2026.

> В копии `sample_data.json` указал у заказа `O1` два поля `status`: сначала `completed`, затем `in_progress`. Команда `readiness --stage S1` приняла файл и сообщила, что этап готов. Первое значение было потеряно при построении JSON-объекта.
>
> Предлагаю отклонять повторяющиеся имена полей при чтении JSON. Для этого примера ожидается ошибка с указанием поля `status` и кодом `2`, без вывода о готовности этапа. [JsonDataSource.java, строки 38–53](https://github.com/kokuyouu/practice/blob/04f56a4eb05058dab43b197d4d0966e34397cd6d/src/java/main/JsonDataSource.java#L38-L53)

**Исправлено в `ea222cb`.** В `src/java/main/JsonDataSource.java` рекурсивный `readValue(JsonReader)` проверяет имена через `Set<String>` до добавления поля. Отклоняются дубликаты верхнего уровня и вложенных объектов, включая экранированные Unicode-имена. Повтор `status` даёт код 2 и сообщение с полем и JSON-путём; одинаковые поля разных объектов допустимы. Дополнительно проверен строгий синтаксис JSON.

Состояние Issue на 24.11.2026: CLOSED.

### Полученное замечание № 3 — Проверка связей при загрузке повторно обходит списки

Источник: <https://github.com/kokuyouu/practice/issues/3>. Автор: `kotel24`, 21.11.2026.

> Для каждой связи «задача — материал» `validateContext()` линейно ищет обе записи по ID. Для каждой задачи `Task.validateContext()` также ищет этап и просматривает участников заказа. При росте этих списков проверка может занимать квадратичное время. Она выполняется при любой команде, включая `brief`.
>
> Предлагаю один раз построить `Map` сущностей по ID и `Set` пар «заказ — сотрудник», как в процедурной версии. После правки штатные результаты и диагностика неверных связей должны сохраниться. [CampaignData.java, строки 116–135](https://github.com/kokuyouu/practice/blob/04f56a4eb05058dab43b197d4d0966e34397cd6d/src/java/core/CampaignData.java#L116-L135) · [Task.java, строки 63–74](https://github.com/kokuyouu/practice/blob/04f56a4eb05058dab43b197d4d0966e34397cd6d/src/java/core/Task.java#L63-L74)

**Исправлено в `ea222cb`.** В `src/java/core/CampaignData.java` при создании снимка строятся семь `Map` сущностей по ID и `Map<String, Set<String>>` участников по заказу. Методы поиска и `Task.validateContext()` используют индексы и `isOrderMember()`. Порядок исходных списков и диагностика неверных связей сохранены. Построение индексов и проверка контекста занимают в среднем O(N) времени и O(N) дополнительной памяти. Это оценка загрузки, а не изменение оценок алгоритмов готовности O(N²) и просрочек O(N log N). Регрессионная проверка на 200 задачах и связях контролирует число чтений ID, отдельные тесты — область участия сотрудника и сообщение об отсутствующей записи.

Состояние Issue на 24.11.2026: CLOSED.

## Проверка после исправлений

- Компиляция `javac --release 17`, Gson 2.13.2; JUnit Jupiter 5.11.4: 35 тестовых запусков, все успешны.
- Проверяется соответствие эталону процедурной версии Python на одном и том же JSON и дате 31.10.2026, выбор последней версии, граница срока, контекст доработки, целостность связей, Unicode и внедрение нового правила.
- В отдельном сравнении до и после исправлений проверены 56 сочетаний восьми наборов и семи команд, включая неверные внешние ссылки: сохранены вывод, диагностика и коды завершения для ранее допустимых команд.
- По состоянию на 24.11.2026 неисправленных полученных замечаний нет. Полные команды сборки и запуска тестов находятся в `src/java/README.md`.
