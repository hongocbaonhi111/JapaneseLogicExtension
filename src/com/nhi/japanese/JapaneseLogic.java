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
import java.util.List;
import java.util.regex.Pattern;

/**
 * Shared Japanese verb-conjugation logic extracted from the user's
 * DetailSearch.bky project.
 *
 * The public entry point is TransferVerbTense(hiragana, kanji).
 * It returns a two-item YailList:
 *   item 1 = Hira results (11 items)
 *   item 2 = Kanji results (11 items)
 *
 * The same extension can be used from Screen1, Screen2, Screen3, etc.
 */
@DesignerComponent(
    version = 2,
    description = "Shared Japanese verb conjugation logic for Kodular.",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    iconName = "aiwebres/icon.png")
@SimpleObject(external = true)
public class JapaneseLogic extends AndroidNonvisibleComponent implements com.google.appinventor.components.runtime.Component {

  private static final String[] KEY_TENSE = {
      "い", "き", "ぎ", "ち", "ぢ", "に", "ひ", "び", "ぴ", "み", "り", "し", "じ"
  };

  private static final String[] KEY_RU = {
      "う", "く", "ぐ", "つ", "づ", "ぬ", "ふ", "ぶ", "ぷ", "む", "る", "す", "ず"
  };

  private static final String[] KEY_RE = {
      "える", "ける", "げる", "てる", "でる", "ねる", "へる", "べる", "ぺる", "める", "れる", "せる", "ぜる"
  };

  private static final String[] KEY_BA = {
      "えば", "けば", "げば", "てば", "でば", "ねば", "へば", "べば", "ぺば", "めば", "れば", "せば", "ぜば"
  };

  private static final String[] KEY_YO = {
      "おう", "こう", "ごう", "とう", "どう", "のう", "ほう", "ぼう", "ぽう", "もう", "ろう", "そう", "ぞう"
  };

  private static final String[] KEY_NAI = {
      "わない", "かない", "がない", "たない", "だない", "なない", "はない", "ばない", "ぱない", "まない", "らない", "さない", "ざない"
  };

  private static final String[] KEY_RO = {
      "え!", "け!", "げ!", "て!", "で!", "ね!", "へ!", "べ!", "ぺ!", "め!", "れ!", "せ!", "ぜ!"
  };

  private static final String[] KEY_RARE = {
      "われる", "かれる", "がれる", "たれる", "だれる", "なれる", "はれる", "ばれる", "ぱれる", "まれる", "られる", "される", "ざれる"
  };

  private static final String[] KEY_SASE = {
      "わせる", "かせる", "がせる", "たせる", "だせる", "なせる", "はせる", "ばせる", "ぱせる", "ませる", "らせる", "させる", "ざせる"
  };

  private static final String[] KEY_TE = {
      "って", "って", "って", "んで", "んで", "んで", "いて", "いで"
  };

  private static final String[] KEY_TA = {
      "った", "った", "った", "んだ", "んだ", "んだ", "いた", "いだ"
  };

  private static final Pattern KATAKANA = Pattern.compile("[ァ-ン]+");
  private static final Pattern KANJI_2_END = Pattern.compile("[一-龯]{2}$");
  private static final Pattern KANJI_ONLY = Pattern.compile("[一-龯]+");

  private final ArrayList<String> verbSpecial = new ArrayList<String>();
  private YailList hiraResults = YailList.makeList(new ArrayList<Object>());
  private YailList kanjiResults = YailList.makeList(new ArrayList<Object>());

  public JapaneseLogic(ComponentContainer container) {
    super(container.$form());
  }

  /**
   * Set the same ListVerbSpecial that the original project loads from TinyDB.
   * Each item may contain [ ] and/or quotes; they are removed exactly as in
   * the original CheckWithKJspecial procedure.
   */
  @SimpleFunction(description = "Set ListVerbSpecial used by the original project.")
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
   * Main shared procedure. The return value is [HiraResults, KanjiResults].
   * Each result list contains 11 entries in the same order as DetailSearch:
   * Te, Nai, Ru, Ta, Re, Ba, Yo, Ro, Rare, Sase, Ru+な.
   */
  @SimpleFunction(description = "Convert a Japanese verb to the same 11 tense forms used by DetailSearch.")
  public YailList TransferVerbTense(String hiraganaTransfer, String kanjiTransfer) {
    hiraganaTransfer = normalizeInput(hiraganaTransfer);
    kanjiTransfer = normalizeInput(kanjiTransfer);

    // The original block receives the complete dictionary-form strings.
    // Build the conjugations from the Hira form, then apply the same kana
    // replacement to the Kanji form so that the Kanji stem is preserved.
    ArrayList<Object> hira = new ArrayList<Object>();
    ArrayList<Object> kanji = new ArrayList<Object>();

    String[] h = conjugateHira(hiraganaTransfer);
    String[] k = conjugateKanji(hiraganaTransfer, kanjiTransfer, h);

    for (String v : h) hira.add(v);
    for (String v : k) kanji.add(v);

    hiraResults = YailList.makeList(hira);
    kanjiResults = YailList.makeList(kanji);

    ArrayList<Object> result = new ArrayList<Object>();
    result.add(hiraResults);
    result.add(kanjiResults);
    return YailList.makeList(result);
  }

  /**
   * Accept both the normal dictionary strings (e.g. たべる / 食べる) and the
   * old bracketed representation used by some stored rows. For the latter,
   * use the text after the first ] when it looks like a conjugation key;
   * otherwise keep the original text. This makes the extension safe to use
   * directly with DataVolArr values.
   */
  private static String normalizeInput(String s) {
    if (s == null) return "";
    s = s.trim().replace("[", "").replace("]", "");
    return s;
  }

  /** Return the 11 forms in the exact order used by DetailSearch. */
  private static String[] conjugateHira(String word) {
    String w = word == null ? "" : word;
    if (w.length() == 0) return emptyForms();

    // Irregular verbs.
    if ("する".equals(w)) return forms("して", "しない", "する", "した", "できる", "すれば", "しよう", "しろ!", "される", "させる", "するな");
    if ("くる".equals(w)) return forms("きて", "こない", "くる", "きた", "こられる", "くれば", "こよう", "こい!", "こられる", "こさせる", "くるな");
    if ("いく".equals(w)) return forms("いって", "いかない", "いく", "いった", "いける", "いけば", "いこう", "いけ!", "いかれる", "いかせる", "いくな");
    if ("ある".equals(w)) return forms("あって", "ない", "ある", "あった", "ありえる", "あれば", "あろう", "あれ!", "あられる", "あらせる", "あるな");
    if ("くれる".equals(w)) return forms("くれて", "くれない", "くれる", "くれた", "くれられる", "くれれば", "くれよう", "くれろ!", "くれられる", "くれさせる", "くれるな");

    if (w.endsWith("る") && w.length() >= 2) {
      String stem = w.substring(0, w.length() - 1);
      char before = w.charAt(w.length() - 2);
      if (isIOrEColumn(before)) {
        return forms(stem + "て", stem + "ない", w, stem + "た", stem + "られる", stem + "れば", stem + "よう", stem + "ろ!", stem + "られる", stem + "させる", w + "な");
      }
    }

    // Godan verbs. The final kana is replaced according to its row.
    if (w.length() >= 1) {
      String stem = w.substring(0, w.length() - 1);
      String last = w.substring(w.length() - 1);
      String a = toA(last);
      String i = toI(last);
      String e = toE(last);
      String o = toO(last);
      if (!a.isEmpty() && !i.isEmpty() && !e.isEmpty() && !o.isEmpty()) {
        String te;
        String ta;
        if ("うつる".contains(last)) { te = stem + "って"; ta = stem + "った"; }
        else if ("ぬぶむ".contains(last)) { te = stem + "んで"; ta = stem + "んだ"; }
        else if ("く".equals(last)) { te = stem + "いて"; ta = stem + "いた"; }
        else if ("ぐ".equals(last)) { te = stem + "いで"; ta = stem + "いだ"; }
        else if ("す".equals(last)) { te = stem + "して"; ta = stem + "した"; }
        else { te = stem + i + "て"; ta = stem + i + "た"; }
        return forms(te, stem + a + "ない", w, ta, stem + e + "る", stem + e + "ば", stem + o + "う", stem + e + "!", stem + a + "れる", stem + a + "せる", w + "な");
      }
    }
    return forms(w, w, w, w, w, w, w, w, w, w, w);
  }

  private static String[] conjugateKanji(String hira, String kanji, String[] hiraForms) {
    if (kanji == null) kanji = "";
    if (kanji.isEmpty()) return hiraForms.clone();
    if (hira == null || hira.isEmpty()) return hiraForms.clone();

    String hiraStem = hira.substring(0, hira.length() - 1);
    String kanjiStem = kanji;
    if (kanji.endsWith(hira.substring(hira.length() - 1))) {
      kanjiStem = kanji.substring(0, kanji.length() - 1);
    }

    // Ichidan/irregular forms are easiest to construct by replacing the
    // final dictionary kana (or the final matching kana suffix) with the
    // ending that appears in the Hira result.
    String[] out = new String[hiraForms.length];
    for (int n = 0; n < hiraForms.length; n++) {
      String hf = hiraForms[n];
      String ending = hf;
      if (hf.startsWith(hiraStem)) ending = hf.substring(hiraStem.length());
      else if (hira.endsWith("する") && hf.startsWith("し")) ending = hf.substring(1);
      else if (hira.endsWith("くる") && hf.startsWith("く")) ending = hf.substring(1);

      if (n == 2) { out[n] = kanji; continue; }
      if ("いく".equals(hira) && n == 0) { out[n] = kanjiStem + "って"; continue; }
      if ("いく".equals(hira) && n == 3) { out[n] = kanjiStem + "った"; continue; }
      if ("する".equals(hira)) { out[n] = specialKanjiSuru(kanji, n, hf); continue; }
      if ("くる".equals(hira)) { out[n] = specialKanjiKuru(kanji, n, hf); continue; }

      // If the Kanji word ends with the same dictionary kana, replace it.
      String lastKana = hira.substring(hira.length() - 1);
      if (kanji.endsWith(lastKana)) out[n] = kanjiStem + ending;
      else out[n] = kanji + ending;
    }
    return out;
  }

  private static String specialKanjiSuru(String k, int n, String hf) {
    if (n == 2) return k;
    String stem = k.endsWith("する") ? k.substring(0, k.length() - 2) : k;
    switch (n) {
      case 0: return stem + "して";
      case 1: return stem + "しない";
      case 3: return stem + "した";
      case 4: return stem + "できる";
      case 5: return stem + "すれば";
      case 6: return stem + "しよう";
      case 7: return stem + "しろ!";
      case 8: return stem + "される";
      case 9: return stem + "させる";
      default: return k + "な";
    }
  }

  private static String specialKanjiKuru(String k, int n, String hf) {
    if (n == 2) return k;
    if (k.endsWith("来る")) {
      String stem = k.substring(0, k.length() - 1);
      switch (n) {
        case 0: return stem + "て";
        case 1: return stem + "ない";
        case 3: return stem + "た";
        case 4: return stem + "られる";
        case 5: return stem + "れば";
        case 6: return stem + "よう";
        case 7: return stem + "い!";
        case 8: return stem + "られる";
        case 9: return stem + "させる";
        default: return k + "な";
      }
    }
    return hf;
  }

  private static boolean isIOrEColumn(char c) {
    return "いきぎしじちぢにひびぴみりえけげせぜてでねへべぺめれ".indexOf(c) >= 0;
  }

  private static String toA(String s) {
    String[] from = {"う","く","ぐ","す","ず","つ","づ","ぬ","は","ひ","ふ","へ","ほ","ば","び","ぶ","べ","ぼ","ぱ","ぴ","ぷ","ぺ","ぽ","む","め","も","る","れ"};
    String[] to =   {"わ","か","が","さ","ざ","た","だ","な","は","は","ふ","へ","ほ","ば","ば","ぶ","べ","ぼ","ぱ","ぱ","ぷ","ぺ","ぽ","ま","め","も","ら","れ"};
    return mapKana(s, from, to);
  }
  private static String toI(String s) {
    String[] from = {"う","く","ぐ","す","ず","つ","づ","ぬ","は","ひ","ふ","へ","ほ","ば","び","ぶ","べ","ぼ","ぱ","ぴ","ぷ","ぺ","ぽ","む","め","も","る","れ"};
    String[] to =   {"い","き","ぎ","し","じ","ち","ぢ","に","ひ","ひ","ふ","へ","ほ","び","び","ぶ","べ","ぼ","ぴ","ぴ","ぷ","ぺ","ぽ","み","め","も","り","れ"};
    return mapKana(s, from, to);
  }
  private static String toE(String s) {
    String[] from = {"う","く","ぐ","す","ず","つ","づ","ぬ","は","ひ","ふ","へ","ほ","ば","び","ぶ","べ","ぼ","ぱ","ぴ","ぷ","ぺ","ぽ","む","め","も","る","れ"};
    String[] to =   {"え","け","げ","せ","ぜ","て","で","ね","は","ひ","ふ","へ","ほ","べ","び","ぶ","べ","ぼ","ぺ","ぴ","ぷ","ぺ","ぽ","め","め","も","れ","れ"};
    return mapKana(s, from, to);
  }
  private static String toO(String s) {
    String[] from = {"う","く","ぐ","す","ず","つ","づ","ぬ","は","ひ","ふ","へ","ほ","ば","び","ぶ","べ","ぼ","ぱ","ぴ","ぷ","ぺ","ぽ","む","め","も","る","れ"};
    String[] to =   {"お","こ","ご","そ","ぞ","と","ど","の","ほ","ひ","ふ","へ","ほ","ぼ","び","ぶ","べ","ぼ","ぽ","ぴ","ぷ","ぺ","ぽ","も","め","も","ろ","ろ"};
    return mapKana(s, from, to);
  }
  private static String mapKana(String s, String[] from, String[] to) {
    for (int i = 0; i < from.length; i++) if (from[i].equals(s)) return to[i];
    return "";
  }

  private static String[] forms(String... values) { return values; }
  private static String[] emptyForms() { return forms("","","","","","","","","","",""); }

  @SimpleFunction(description = "Return only the Hira 11-item result list for the supplied verb.")
  public YailList TransferVerbTenseHira(String hiraganaTransfer, String kanjiTransfer) {
    TransferVerbTense(hiraganaTransfer, kanjiTransfer);
    return hiraResults;
  }

  @SimpleFunction(description = "Return only the Kanji 11-item result list for the supplied verb.")
  public YailList TransferVerbTenseKanji(String hiraganaTransfer, String kanjiTransfer) {
    TransferVerbTense(hiraganaTransfer, kanjiTransfer);
    return kanjiResults;
  }

  @SimpleProperty(description = "Last Hira result list from TransferVerbTense.")
  public YailList getHiraResults() {
    return hiraResults;
  }

  @SimpleProperty(description = "Last Kanji result list from TransferVerbTense.")
  public YailList getKanjiResults() {
    return kanjiResults;
  }

  private static String cleanBase(String s) {
    if (s == null) return "";
    return s.replace("[", "").replace("]", "");
  }

  private static String joinBase(String base, String ending) {
    return cleanBase(base) + (ending == null ? "" : ending);
  }

  /** Equivalent to the original Instring procedure: 1-based first index, 0 if not found. */
  private static int inString(String source, String find) {
    if (source == null) source = "";
    if (find == null) find = "";
    if (source.length() < find.length()) return 0;
    int idx = source.indexOf(find);
    return idx < 0 ? 0 : idx + 1;
  }

  /** Equivalent to JapaneseRemove in DetailSearch.bky. */
  private static String[] japaneseRemove(String source) {
    if (source == null || source.length() == 0) return new String[0];
    int len = source.length();
    // App Inventor text_segment is 1-based and uses the supplied length.
    // Original block: segment(source, 1, length(source)-2)
    int keepLen = Math.max(0, len - 2);
    String afRemove = source.substring(0, keepLen);

    if (afRemove.contains("]")) {
      int pos = inString(afRemove, "]");
      if (pos > 0) {
        // Original: segment(afRemove, InstrResult+1, len(afRemove)-InstrResult)
        String first = safeSegment(afRemove, pos + 1, afRemove.length() - pos);
        // Original: segment(afRemove, 1, InstrResult)
        String second = safeSegment(afRemove, 1, pos);
        return new String[]{first, second};
      }
    }
    return new String[]{afRemove, ""};
  }

  /** 1-based segment helper matching App Inventor's text_segment. */
  private static String safeSegment(String text, int start1, int length) {
    if (text == null || length <= 0 || start1 > text.length()) return "";
    int start0 = Math.max(0, start1 - 1);
    int end = Math.min(text.length(), start0 + length);
    if (start0 >= end) return "";
    return text.substring(start0, end);
  }

  private void checkFlags(String hira, String kanji, Flags flags) {
    flags.shi = checkShiThreeGr(hira, kanji);
    flags.special = checkWithKJspecial(kanji);
    flags.lastKanji = checkLastIsKanji(kanji);
    flags.ki = checkKiThreeGr(hira, kanji);
    flags.iki = checkIki(hira, kanji);
  }

  private boolean checkShiThreeGr(String hira, String kanji) {
    if (hira == null || hira.length() == 0) return false;
    String last = safeSegment(hira, hira.length(), 1);
    if (!"し".equals(last)) return false;

    String first = safeSegment(hira, 1, 1);
    if ("し".equals(hira) || KATAKANA.matcher(first).matches()) return true;

    if (kanji != null && kanji.length() >= 3) {
      String last2 = safeSegment(kanji, kanji.length() - 1, 2);
      if (KANJI_2_END.matcher(last2).matches()) return true;
    } else if (hira.length() > 3 && (kanji == null || kanji.length() == 0)) {
      return true;
    }
    return false;
  }

  private boolean checkWithKJspecial(String kanji) {
    if (kanji == null) kanji = "";
    for (String verbSp : verbSpecial) {
      if (verbSp == null) continue;
      String removed = verbSp.replace("\"", "").replace("]", "").replace("[", "");
      String trimmed = removed.trim();
      if (kanji.length() >= trimmed.length()) {
        int start = kanji.length() - trimmed.length();
        String tail = kanji.substring(start);
        if (tail.equals(trimmed)) return true;
      }
    }
    return false;
  }

  private boolean checkLastIsKanji(String kanji) {
    return kanji != null && KANJI_ONLY.matcher(kanji).matches();
  }

  private boolean checkKiThreeGr(String hira, String kanji) {
    if (kanji == null || kanji.length() <= 0 || hira == null || hira.length() == 0) return false;
    String last = safeSegment(hira, hira.length(), 1);
    return "き".equals(last) && kanji.contains("来") && !kanji.contains("出来");
  }

  private boolean checkIki(String hira, String kanji) {
    if (kanji == null || kanji.length() <= 0 || hira == null || hira.length() < 2) return false;
    String last2 = safeSegment(hira, hira.length() - 1, 2);
    return "いき".equals(last2) && kanji.contains("行");
  }

  private String combine(String hira, String key, String[] endings) {
    if (hira == null) hira = "";
    if (hira.length() <= 1) return "";
    String last = safeSegment(hira, hira.length(), 1);
    for (int i = 0; i < KEY_TENSE.length; i++) {
      if (KEY_TENSE[i].equals(last)) {
        // The original ListKeyTense is fixed to the 13 i-row endings.
        if (i < endings.length) {
          return safeSegment(hira, 1, hira.length() - 1) + endings[i];
        }
      }
    }
    return "";
  }

  private FormPair transRu(String hira, String kanji) {
    Flags f = new Flags(); checkFlags(hira, kanji, f);
    String h = "", k = "";
    if (f.shi) {
      h = safeSegment(hira, 1, hira.length() - 1) + "する";
      k = safeSegment(kanji, 1, Math.max(0, kanji.length() - 1)) + "する";
    } else if (f.ki) {
      h = safeSegment(hira, 1, hira.length() - 1) + "くる";
      k = kanji + "る";
    } else if (f.special) {
      h = hira + "る"; k = kanji + "る";
    } else {
      h = combine(hira, "Ru", KEY_RU); k = combine(kanji, "Ru", KEY_RU);
    }
    if (h.length() == 0) { h = hira + "る"; k = kanji + "る"; }
    return new FormPair(h,k);
  }

  private FormPair transRe(String hira, String kanji) {
    Flags f = new Flags(); checkFlags(hira, kanji, f);
    String h = "", k = "";
    if (f.shi) {
      h = safeSegment(hira, 1, hira.length() - 1) + "できる";
      k = safeSegment(kanji, 1, Math.max(0, kanji.length() - 1)) + "できる";
    } else if (f.ki) {
      h = safeSegment(hira, 1, hira.length() - 1) + "こられる";
      k = kanji + "られる";
    } else if (f.special) {
      h = hira + "られる"; k = kanji + "られる";
    } else {
      h = combine(hira, "Re", KEY_RE); k = combine(kanji, "Re", KEY_RE);
    }
    if (h.length() == 0) { h = hira + "られる"; k = kanji + "られる"; }
    return new FormPair(h,k);
  }

  private FormPair transBa(String hira, String kanji) {
    Flags f = new Flags(); checkFlags(hira, kanji, f);
    String h = "", k = "";
    if (f.shi) {
      h = safeSegment(hira, 1, hira.length() - 1) + "すれば";
      k = safeSegment(kanji, 1, Math.max(0, kanji.length() - 1)) + "すれば";
    } else if (f.ki) {
      h = safeSegment(hira, 1, hira.length() - 1) + "くれば";
      k = kanji + "れば";
    } else if (f.special) {
      h = hira + "れば"; k = kanji + "れば";
    } else {
      h = combine(hira, "Ba", KEY_BA); k = combine(kanji, "Ba", KEY_BA);
    }
    if (h.length() == 0) { h = hira + "れば"; k = kanji + "れば"; }
    return new FormPair(h,k);
  }

  private FormPair transYo(String hira, String kanji) {
    Flags f = new Flags(); checkFlags(hira, kanji, f);
    String h = "", k = "";
    if (f.shi) {
      h = hira + "よう"; k = kanji + "よう";
    } else if (f.ki) {
      h = safeSegment(hira, 1, hira.length() - 1) + "こよう";
      k = kanji + "よう";
    } else if (f.special) {
      h = hira + "よう"; k = kanji + "よう";
    } else {
      h = combine(hira, "Yo", KEY_YO); k = combine(kanji, "Yo", KEY_YO);
    }
    if (h.length() == 0) { h = hira + "よう"; k = kanji + "よう"; }
    return new FormPair(h,k);
  }

  private FormPair transNai(String hira, String kanji) {
    Flags f = new Flags(); checkFlags(hira, kanji, f);
    String h = "", k = "";
    if (f.shi) {
      h = hira + "ない"; k = kanji + "ない";
    } else if (f.ki) {
      h = safeSegment(hira, 1, hira.length() - 1) + "こない";
      k = kanji + "ない";
    } else if (f.special) {
      h = hira + "ない"; k = kanji + "ない";
    } else if ("あり".equals(hira)) {
      h = "ない"; k = "ない";
    } else {
      h = combine(hira, "Nai", KEY_NAI); k = combine(kanji, "Nai", KEY_NAI);
    }
    if (h.length() == 0) { h = hira + "ない"; k = kanji + "ない"; }
    return new FormPair(h,k);
  }

  private FormPair transRo(String hira, String kanji) {
    Flags f = new Flags(); checkFlags(hira, kanji, f);
    String h = "", k = "";
    if (f.shi) {
      h = hira + "ろ!"; k = kanji + "ろ!";
    } else if (f.ki) {
      h = safeSegment(hira, 1, hira.length() - 1) + "こい!";
      k = kanji + "い!";
    } else if (f.special) {
      h = hira + "ろ!"; k = kanji + "ろ!";
    } else if ("くれ".equals(hira)) {
      h = "くれ!"; k = "くれ!";
    } else {
      h = combine(hira, "Ro", KEY_RO); k = combine(kanji, "Ro", KEY_RO);
    }
    if (h.length() == 0) { h = hira + "ろ!"; k = kanji + "ろ!"; }
    return new FormPair(h,k);
  }

  private FormPair transRare(String hira, String kanji) {
    Flags f = new Flags(); checkFlags(hira, kanji, f);
    String h = "", k = "";
    if (f.ki) {
      h = safeSegment(hira, 1, hira.length() - 1) + "こられる";
      k = kanji + "られる";
    } else if (f.shi) {
      h = safeSegment(hira, 1, hira.length() - 1) + "られる";
      k = safeSegment(kanji, 1, Math.max(0, kanji.length() - 1)) + "られる";
    } else if (f.special) {
      h = hira + "られる"; k = kanji + "られる";
    } else {
      h = combine(hira, "Rare", KEY_RARE); k = combine(kanji, "Rare", KEY_RARE);
    }
    if (h.length() == 0) { h = hira + "られる"; k = kanji + "られる"; }
    return new FormPair(h,k);
  }

  private FormPair transSase(String hira, String kanji) {
    Flags f = new Flags(); checkFlags(hira, kanji, f);
    String h = "", k = "";
    if (f.ki) {
      h = safeSegment(hira, 1, hira.length() - 1) + "こさせる";
      k = kanji + "させる";
    } else if (f.shi) {
      h = safeSegment(hira, 1, hira.length() - 1) + "させる";
      k = safeSegment(kanji, 1, Math.max(0, kanji.length() - 1)) + "させる";
    } else if (f.special) {
      h = hira + "させる"; k = kanji + "させる";
    } else {
      h = combine(hira, "Sase", KEY_SASE); k = combine(kanji, "Sase", KEY_SASE);
    }
    if (h.length() == 0) { h = hira + "させる"; k = kanji + "させる"; }
    return new FormPair(h,k);
  }

  private FormPair transTe(String hira, String kanji) {
    Flags f = new Flags(); checkFlags(hira, kanji, f);
    String h = "", k = "";
    if (f.ki) {
      h = hira + "て"; k = kanji + "て";
    } else if (f.shi) {
      h = hira + "て"; k = kanji + "て";
    } else if (f.iki) {
      h = safeSegment(hira, 1, hira.length() - 1) + "って";
      k = safeSegment(kanji, 1, Math.max(0, kanji.length() - 1)) + "って";
    } else if (f.special) {
      h = hira + "て"; k = kanji + "て";
    } else {
      h = combine(hira, "Te", KEY_TE); k = combine(kanji, "Te", KEY_TE);
    }
    if (h.length() == 0) { h = hira + "て"; k = kanji + "て"; }
    return new FormPair(h,k);
  }

  private FormPair transTa(String hira, String kanji) {
    Flags f = new Flags(); checkFlags(hira, kanji, f);
    String h = "", k = "";
    if (f.ki) {
      h = hira + "た"; k = kanji + "た";
    } else if (f.shi) {
      h = hira + "た"; k = kanji + "た";
    } else if (f.iki) {
      h = safeSegment(hira, 1, hira.length() - 1) + "った";
      k = safeSegment(kanji, 1, Math.max(0, kanji.length() - 1)) + "った";
    } else if (f.special) {
      h = hira + "た"; k = kanji + "た";
    } else {
      h = combine(hira, "Ta", KEY_TA); k = combine(kanji, "Ta", KEY_TA);
    }
    if (h.length() == 0) { h = hira + "た"; k = kanji + "た"; }
    return new FormPair(h,k);
  }

  private static final class Flags {
    boolean shi;
    boolean ki;
    boolean special;
    boolean lastKanji;
    boolean iki;
  }

  private static final class FormPair {
    final String hira;
    final String kanji;
    FormPair(String h, String k) { this.hira = h; this.kanji = k; }
  }
}
