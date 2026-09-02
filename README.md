# JapaneseLogic — Kodular shared Procedure Extension

This package converts the verb-tense logic from `DetailSearch.bky` into one shared Kodular Extension.

## Main blocks

### `TransferVerbTense(hiraganaTransfer, kanjiTransfer)`
Returns a 2-item list:

1. Hira result list — 11 items
2. Kanji result list — 11 items

Order is the same as the original `DetailSearch` block:

`Te, Nai, Ru, Ta, Re, Ba, Yo, Ro, Rare, Sase, Ru+な`

### `SetVerbSpecialList(list)`
Pass the same `ListVerbSpecial` list that the original app loads from TinyDB/Web data. Normal verbs work with the default empty list; special verbs require this list.

### `GetHiraResults` / `GetKanjiResults`
Properties containing the last calculated result lists.

## Build without installing Java on your PC

The included GitHub Actions workflow builds the `.aix` file in GitHub's runner. Create a GitHub repository, upload this folder, then run **Actions → Build JapaneseLogic AIX → Run workflow**. Download the `JapaneseLogic-aix` artifact.

## Kodular usage

Import `JapaneseLogic.aix` into each Screen that needs the logic. Then use:

`call JapaneseLogic1.TransferVerbTense(Hiragana, Kanji)`

The extension contains the processing logic; Screen blocks should handle UI only.
