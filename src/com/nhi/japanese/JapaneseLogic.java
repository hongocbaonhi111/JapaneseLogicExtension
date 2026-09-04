package com.nhi.japanese;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.util.YailList;

import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Japanese verb conjugation logic for Kodular.
 *
 * IMPORTANT:
 * This version keeps the ORIGINAL classification logic:
 *   - CheckShiThreeGr
 *   - CheckWithKJspecial / ListVerbSpecial
 *   - CheckLastIsKanji
 *   - CheckKiThreeGr
 *   - CheckIki
 *
 * The extension returns ENDINGS ONLY.
 * Kodular can combine the returned ending with its own Hira/Kanji stem.
 *
 * TransferVerbTense(hiraganaStem, kanjiStem)
 * returns:
 *   [HiraEndings(11), KanjiEndings(11)]
 *
 * Order:
 *   Te, Nai, Ru, Ta, Re, Ba, Yo, Ro, Rare, Sase, Ru+na
 */
@DesignerComponent(
    version = 7,
    description = "Shared Japanese verb conjugation logic using the original DetailSearch checks.",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    iconName = "aiwebres/icon.png")
@SimpleObject(external = true)
public class JapaneseLogic extends AndroidNonvisibleComponent
    implements com.google.appinventor.components.runtime.Component {

  // Original ListKeyTense
  private static final String[] KEY_TENSE = {
      "い", "き", "ぎ", "ち", "ぢ", "に",
      "ひ", "び", "ぴ", "み", "り", "し", "じ"
  };

  // Original ListKeyTe / ListKeyTa
  private static final String[] KEY_TE = {
      "って", "って", "って", "んで", "んで", "んで", "いて", "いで"
  };

  private static final String[] KEY_TA = {
      "った", "った", "った", "んだ", "んだ", "んだ", "いた", "いだ"
  };

  // Original ListKeyRu
  private static final String[] KEY_RU = {
      "う", "く", "ぐ", "つ", "づ", "ぬ", "ふ",
      "ぶ", "ぷ", "む", "る", "す", "ず"
  };

  // Original ListKeyRe
  private static final String[] KEY_RE = {
      "える", "ける", "げる", "てる", "でる", "ねる", "へる",
      "べる", "ぺる", "める", "れる", "せる", "ぜる"
  };

  // Original ListKeyBa
  private static final String[] KEY_BA = {
      "えば", "けば", "げば", "てば", "でば", "ねば", "へば",
      "べば", "ぺば", "めば", "れば", "せば", "ぜば"
  };

  // Original ListKeyYo
  private static final String[] KEY_YO = {
      "おう", "こう", "ごう", "とう", "どう", "のう", "ほう",
      "ぼう", "ぽう", "もう", "ろう", "そう", "ぞう"
  };

  // Original ListKeyNai
  private static final String[] KEY_NAI = {
      "わない", "かない", "がない", "たない", "だない", "なない",
      "はない", "ばない", "ぱない", "まない", "らない", "さない",
      "ざない"
  };

  // Original ListKeyRo
  private static final String[] KEY_RO = {
      "え!", "け!", "げ!", "て!", "で!", "ね!", "へ!",
      "べ!", "ぺ!", "め!", "れ!", "せ!", "ぜ!"
  };

  // Original ListKeyRare
  private static final String[] KEY_RARE = {
      "われる", "かれる", "がれる", "たれる", "だれる", "なれる",
      "はれる", "ばれる", "ぱれる", "まれる", "られる", "される",
      "ざれる"
  };

  // Original ListKeySase
  private static final String[] KEY_SASE = {
      "わせる", "かせる", "がせる", "たせる", "だせる", "なせる",
      "はせる", "ばせる", "ぱせる", "ませる", "らせる", "させる",
      "ざせる"
  };

  private static final Pattern KATAKANA = Pattern.compile("[ァ-ン]+");
  private static final Pattern KANJI_2_END = Pattern.compile("[一-龯]{2}$");
  private static final Pattern KANJI_ONLY = Pattern.compile("[一-龯]+");

  /**
   * This is the original ListVerbSpecial loaded by the Kodular app.
   * It must be passed to the extension through SetVerbSpecialList().
   */
  private final ArrayList<String> verbSpecial = new ArrayList<String>();

  private YailList hiraResults = emptyYail();
  private YailList kanjiResults = emptyYail();

  public JapaneseLogic(ComponentContainer container) {
    super(container.$form());
  }

  /**
   * Pass the ORIGINAL ListVerbSpecial from Kodular.
   *
   * The original CheckWithKJspecial removes:
   *   "
   *   [
   *   ]
   *
   * before checking whether KanjiCheck ends with that value.
   */
  @SimpleFunction(description = "Set the original ListVerbSpecial used by DetailSearch.")
  public void SetVerbSpecialList(YailList list) {
    verbSpecial.clear();

    if (list == null) {
      return;
    }

    for (Object item : list.toArray()) {
      if (item != null) {
        verbSpecial.add(String.valueOf(item));
      }
    }
  }

  @SimpleFunction(description = "Clear ListVerbSpecial.")
  public void ClearVerbSpecialList() {
    verbSpecial.clear();
  }

  /**
   * Main function.
   *
   * IMPORTANT:
   * This version uses Kanji ONLY for classification.
   * It does NOT combine Hira/Kanji with the result.
   */
  @SimpleFunction(description = "Return the 11 conjugation endings using the original DetailSearch checks.")
  public YailList TransferVerbTense(
      String hiraganaTransfer,
      String kanjiTransfer) {

    String hira = normalize(hiraganaTransfer);
    String kanji = normalize(kanjiTransfer);

    String[] endings = conjugationEndings(hira, kanji);

    ArrayList<Object> h = new ArrayList<Object>();
    ArrayList<Object> k = new ArrayList<Object>();

    for (String ending : endings) {
      h.add(ending);
      k.add(ending);
    }

    hiraResults = YailList.makeList(h);
    kanjiResults = YailList.makeList(k);

    ArrayList<Object> result = new ArrayList<Object>();
    result.add(hiraResults);
    result.add(kanjiResults);

    return YailList.makeList(result);
  }

  @SimpleFunction(description = "Return the same 11 endings as TransferVerbTense.")
  public YailList GetTenseEndings(
      String hiraganaStem,
      String kanjiStem) {
    return TransferVerbTense(hiraganaStem, kanjiStem);
  }

  @SimpleFunction(description = "Return only the Hira ending list.")
  public YailList TransferVerbTenseHira(
      String hiraganaTransfer,
      String kanjiTransfer) {
    TransferVerbTense(hiraganaTransfer, kanjiTransfer);
    return hiraResults;
  }

  @SimpleFunction(description = "Return only the Kanji ending list.")
  public YailList TransferVerbTenseKanji(
      String hiraganaTransfer,
      String kanjiTransfer) {
    TransferVerbTense(hiraganaTransfer, kanjiTransfer);
    return kanjiResults;
  }

  @SimpleProperty(description = "Last Hira result list.")
  public YailList getHiraResults() {
    return hiraResults;
  }

  @SimpleProperty(description = "Last Kanji result list.")
  public YailList getKanjiResults() {
    return kanjiResults;
  }

  private static YailList emptyYail() {
    return YailList.makeList(new ArrayList<Object>());
  }

  /**
   * Keep the input behavior simple:
   *   - remove [ ]
   *   - remove ます / ました if accidentally supplied
   *
   * The normal input for this extension is the stem:
   *   かい / 買い
   *   べんきょうし / 勉強し
   */
  private static String normalize(String s) {
    if (s == null) {
      return "";
    }

    s = s.trim()
        .replace("[", "")
        .replace("]", "");

    if (s.endsWith("ました") && s.length() > 3) {
      s = s.substring(0, s.length() - 3);
    } else if (s.endsWith("ます") && s.length() > 2) {
      s = s.substring(0, s.length() - 2);
    }

    return s;
  }

  /**
   * Generate ONLY the ending list.
   *
   * This follows the original order of DetailSearch:
   *   Te, Nai, Ru, Ta, Re, Ba, Yo, Ro, Rare, Sase, Ru+na
   */
  private String[] conjugationEndings(String hira, String kanji) {

    if (hira == null || hira.length() == 0) {
      return blankForms();
    }

    // ============================================================
    // ORIGINAL FLAG CHECKS
    // ============================================================

    boolean shi = checkShiThreeGr(hira, kanji);
    boolean special = checkWithKJspecial(kanji);
    boolean lastKanji = checkLastIsKanji(kanji);
    boolean ki = checkKiThreeGr(hira, kanji);
    boolean iki = checkIki(hira, kanji);

    // lastKanji is intentionally calculated because it existed in
    // the original Check/Procedure flow. It does not by itself
    // override another classification.
    @SuppressWarnings("unused")
    boolean unusedLastKanji = lastKanji;

    // ============================================================
    // する
    // ============================================================
    // This MUST be before generic final し.
    if (shi) {
      return forms(
          "して",
          "しない",
          "する",
          "した",
          "できる",
          "すれば",
          "しよう",
          "しろ!",
          "られる",
          "させる",
          "するな");
    }

    // ============================================================
    // 来る
    // ============================================================
    if (ki) {
      return forms(
          "きて",
          "こない",
          "くる",
          "きた",
          "こられる",
          "くれば",
          "こよう",
          "こい!",
          "こられる",
          "こさせる",
          "くるな");
    }

    // ============================================================
    // 行く
    // ============================================================
    if (iki) {
      return forms(
          "って",
          "かない",
          "く",
          "った",
          "ける",
          "けば",
          "こう",
          "け!",
          "かれる",
          "かせる",
          "くな");
    }

    // ============================================================
    // ORIGINAL ListVerbSpecial
    // ============================================================
    if (special) {
      String hiraLast = lastChar(hira);
      return ichidanForms(hiraLast);
    }
    if (hira.length() == 1 && checkLastIsKanji(kanji)
         && kanji.length() == 1) {
    String hiraLast = lastChar(hira);
    return ichidanForms(hiraLast);
    }
    String last = lastChar(hira);

    // ============================================================
    // The original procedure treats final え as Ichidan.
    // ============================================================
    // Ichidan
    if ("えけげせぜてでねへべぺめれ".contains(last)) {
       String hiraLast = lastChar(hira);
       return ichidanForms(hiraLast);
    }

    // ============================================================
    // Generic endings: EXACTLY based on the original key lists.
    // ============================================================

    if ("い".equals(last)) {
      return endingsFromIndex(KEY_TENSE, last,
          "って", "わない", "う", "った", "える",
          "えば", "おう", "え!", "われる", "わせる", "うな");
    }

    if ("き".equals(last)) {
      return endingsFromIndex(KEY_TENSE, last,
          "いて", "かない", "く", "いた", "ける",
          "けば", "こう", "け!", "かれる", "かせる", "くな");
    }

    if ("ぎ".equals(last)) {
      return endingsFromIndex(KEY_TENSE, last,
          "いで", "がない", "ぐ", "いだ", "げる",
          "げば", "ごう", "げ!", "がれる", "がせる", "ぐな");
    }

    // Generic す. Reached only when CheckShiThreeGr is false.
    if ("し".equals(last)) {
      return forms(
          "して", "さない", "す", "した", "せる",
          "せば", "そう", "せ!", "される", "させる", "すな");
    }

    if ("ち".equals(last)) {
      return endingsFromIndex(KEY_TENSE, last,
          "って", "たない", "つ", "った", "てる",
          "てば", "とう", "て!", "たれる", "たせる", "つな");
    }

    if ("ぢ".equals(last)) {
      return endingsFromIndex(KEY_TENSE, last,
          "って", "だない", "づ", "った", "でる",
          "でば", "どう", "で!", "だれる", "だせる", "づな");
    }

    if ("に".equals(last)) {
      return endingsFromIndex(KEY_TENSE, last,
          "んで", "なない", "ぬ", "んだ", "ねる",
          "ねば", "のう", "ね!", "なれる", "なせる", "ぬな");
    }

    if ("ひ".equals(last)) {
      return endingsFromIndex(KEY_TENSE, last,
          "いて", "はない", "ふ", "いた", "へる",
          "へば", "ほう", "へ!", "はれる", "はせる", "ふな");
    }

    if ("び".equals(last)) {
      return endingsFromIndex(KEY_TENSE, last,
          "んで", "ばない", "ぶ", "んだ", "べる",
          "べば", "ぼう", "べ!", "ばれる", "ばせる", "ぶな");
    }

    if ("ぴ".equals(last)) {
      return endingsFromIndex(KEY_TENSE, last,
          "んで", "ぱない", "ぷ", "んだ", "ぺる",
          "ぺば", "ぽう", "ぺ!", "ぱれる", "ぱせる", "ぷな");
    }

    if ("み".equals(last)) {
      return endingsFromIndex(KEY_TENSE, last,
          "んで", "まない", "む", "んだ", "める",
          "めば", "もう", "め!", "まれる", "ませる", "むな");
    }

    if ("り".equals(last)) {
      return endingsFromIndex(KEY_TENSE, last,
          "って", "らない", "る", "った", "れる",
          "れば", "ろう", "れ!", "られる", "らせる", "るな");
    }

    if ("じ".equals(last)) {
      return endingsFromIndex(KEY_TENSE, last,
          "して", "ざない", "ず", "した", "ぜる",
          "ぜば", "ぞう", "ぜ!", "ざれる", "ざせる", "ずな");
    }

    // Dictionary-form Ichidan fallback, retained from the original
    // Java implementation.
    if ("る".equals(last) && hira.length() >= 2) {
      String prev = hira.substring(hira.length() - 2, hira.length() - 1);
      if (isIOrEColumn(prev)) {
        String hiraLast = lastChar(hira);
        return ichidanForms(hiraLast);
      }
    }

    String hiraLast = lastChar(hira);
    return ichidanForms(hiraLast);
  }

  /**
   * Equivalent to the original CheckShiThreeGr.
   *
   * IMPORTANT:
   * The original App Inventor block uses:
   *   text_segment(KanjiCheck, length(KanjiCheck)-2, 2)
   *
   * Because App Inventor text_segment is 1-based, the equivalent Java
   * substring for:
   *   勉強し
   * is:
   *   substring(0, 2) = 勉強
   *
   * Therefore:
   *   べんきょうし + 勉強し -> する
   *   はなし + 話し      -> す
   */
  private boolean checkShiThreeGr(String hira, String kanji) {

    if (hira == null || hira.length() == 0) {
      return false;
    }

    String last = safeSegment(hira, hira.length(), 1);

    if (!"し".equals(last)) {
      return false;
    }

    // Original:
    // if HiraCheck = し -> true
    if ("し".equals(hira)) {
      return true;
    }

    // Original:
    // if first Hira character matches [ァ-ン]+ -> true
    String first = safeSegment(hira, 1, 1);

    if (KATAKANA.matcher(first).matches()) {
      return true;
    }

    // Original Kanji check:
    // text_segment(KanjiCheck, length(KanjiCheck)-2, 2)
    //
    // Correct Java equivalent:
    // substring(length - 3, length - 1)
    //
    // This checks the TWO Kanji immediately before final し.
    if (kanji != null && kanji.length() >= 3) {

      String beforeShi2 =
          kanji.substring(kanji.length() - 3, kanji.length() - 1);

      if (KANJI_2_END.matcher(beforeShi2).matches()) {
        return true;
      }

    } else if (hira.length() > 3 &&
               (kanji == null || kanji.length() == 0)) {

      // Original fallback
      return true;
    }

    return false;
  }

  /**
   * Exact logic of original CheckWithKJspecial.
   *
   * It checks whether KanjiCheck ENDS WITH one of ListVerbSpecial.
   */
  private boolean checkWithKJspecial(String kanji) {

    if (kanji == null) {
      kanji = "";
    }

    for (String verbSp : verbSpecial) {

      if (verbSp == null) {
        continue;
      }

      String removed = verbSp
          .replace("\"", "")
          .replace("]", "")
          .replace("[", "");

      String trimmed = removed.trim();

      if (kanji.length() >= trimmed.length()) {

        int start = kanji.length() - trimmed.length();
        String tail = kanji.substring(start);

        if (tail.equals(trimmed)) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Exact equivalent of CheckLastIsKanji:
   * [一-龯]+
   */
  private boolean checkLastIsKanji(String kanji) {
    return kanji != null && KANJI_ONLY.matcher(kanji).matches();
  }

  /**
   * Exact equivalent of CheckKiThreeGr:
   *
   * Hira final = き
   * AND Kanji contains 来
   * AND Kanji does NOT contain 出来
   */
  private boolean checkKiThreeGr(String hira, String kanji) {

    if (kanji == null || kanji.length() == 0 ||
        hira == null || hira.length() == 0) {
      return false;
    }

    String last = safeSegment(hira, hira.length(), 1);

    return "き".equals(last)
        && kanji.contains("来")
        && !kanji.contains("出来");
  }

  /**
   * Exact equivalent of CheckIki:
   *
   * Hira final two chars = いき
   * AND Kanji contains 行
   */
  private boolean checkIki(String hira, String kanji) {

    if (kanji == null || kanji.length() == 0 ||
        hira == null || hira.length() < 2) {
      return false;
    }

    String last2 = safeSegment(hira, hira.length() - 1, 2);

    return "いき".equals(last2) && kanji.contains("行");
  }

  /**
   * Return an ending list according to the final kana.
   *
   * The index corresponds to the original ListKeyTense.
   */
  private static String[] endingsFromIndex(
      String[] keyList,
      String last,
      String te,
      String nai,
      String ru,
      String ta,
      String re,
      String ba,
      String yo,
      String ro,
      String rare,
      String sase,
      String runa) {

    // keyList is deliberately supplied to keep the mapping tied to
    // the original ListKeyTense structure.
    for (String ignored : keyList) {
      if (ignored.equals(last)) {
        return forms(te, nai, ru, ta, re, ba, yo, ro, rare, sase, runa);
      }
    }

    ///String hiraLast = lastChar(hira);
    return ichidanForms(last);
  }

  /**
   * Ichidan endings.
   *
   * Since the extension returns endings only:
   *   たべ + て
   *   み + ない
   *   おき + る
   */
  private String[] ichidanForms(String hiraLast) {
    return forms(
        hiraLast + "て",
        hiraLast + "ない",
        hiraLast + "る",
        hiraLast + "た",
        hiraLast + "られ",
        hiraLast + "れば",
        hiraLast + "よう",
        hiraLast + "ろ!",
        hiraLast + "られる",
        hiraLast + "させる",
        hiraLast + "るな"
    );
}

  private static String[] forms(
      String a, String b, String c, String d, String e,
      String f, String g, String h, String i, String j, String k) {

    return new String[] {
        a, b, c, d, e, f, g, h, i, j, k
    };
  }

  private static String[] blankForms() {
    return forms("", "", "", "", "", "", "", "", "", "", "");
  }

  /**
   * 1-based segment helper matching App Inventor text_segment.
   */
  private static String safeSegment(String text, int start1, int length) {

    if (text == null || length <= 0 || start1 > text.length()) {
      return "";
    }

    int start0 = Math.max(0, start1 - 1);
    int end = Math.min(text.length(), start0 + length);

    if (start0 >= end) {
      return "";
    }

    return text.substring(start0, end);
  }

  private static String lastChar(String s) {
    if (s == null || s.length() == 0) {
      return "";
    }
    return s.substring(s.length() - 1);
  }

  /**
   * Original Ichidan helper.
   */
  private static boolean isIOrEColumn(String s) {
    return "いきぎしじちぢにひびぴみりえけげせぜてでねへべぺめれ"
        .contains(s);
  }
}
