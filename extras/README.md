# Шейдеры для KonoCraft (по желанию)

На Fabric 1.20.1 шейдер-пак сам по себе не работает: нужны два мода, **Sodium** (оптимизация) и **Iris** (загрузчик шейдеров). Оба лежат здесь, в `mods/`, и в разделе *Assets* каждого релиза.

1. Скачайте `sodium-fabric-0.5.13-mc1.20.1.jar` и `iris-1.7.6-mc1.20.1.jar`.
2. Положите оба файла в папку `mods`, туда же, где лежит `konocraft-*.jar`. Например: `D:\manicraft\mods`.
3. Рядом с `mods` создайте папку **`shaderpacks`**, например `D:\manicraft\shaderpacks`.
4. Скачайте шейдер **Complementary Unbound** с официальной страницы <https://modrinth.com/shader/complementary-unbound> (кнопка *Download*, версия для Iris). Положите **zip как есть, не распаковывая**, в `shaderpacks`.
   Сам шейдер мы не перезаливаем: его лицензия запрещает выкладывать файл где-либо, кроме официальных страниц (<https://www.complementary.dev/>, Modrinth, CurseForge).
5. Запустите **Fabric 1.20.1** → *Настройки → Настройки графики → Пакеты шейдеров...* → выберите `ComplementaryUnbound` → *Применить* → *Готово*. Меню шейдеров открывается и клавишей **O**.

Советы:
- Тормозит — в том же меню нажмите *Настройки шейдера...* и выберите профиль пониже (Low / Medium) или выключите шейдеры. Sodium ускоряет игру и без шейдеров, его можно оставить.
- Iris по умолчанию занимает клавиши **R** и **K**, а в KonoCraft это навык и карта авантюриста. Мод сам снимает эти две клавиши с Iris, поэтому навыки и карта работают; включать и выключать шейдеры можно через меню **O**.
- Fabric API отдельно ставить не нужно: он уже встроен в мод.

## Лицензии

- **Sodium** (CaffeineMC), PolyForm Shield 1.0.0: <https://polyformproject.org/licenses/shield/1.0.0>. Текст лицензии лежит в `mods/SODIUM-LICENSE.md` и внутри jar. Исходники: <https://github.com/CaffeineMC/sodium>.
- **Iris** (IrisShaders), LGPL-3.0: текст лицензии лежит в `mods/IRIS-LICENSE.txt` и внутри jar. Исходники: <https://github.com/IrisShaders/Iris>.
- **Complementary Unbound** (EminGT): <https://www.complementary.dev/>, распространяется только с официальных страниц.

Оба jar лежат здесь без изменений, в том виде, в каком их выпустили авторы.
