from __future__ import annotations

import json
import re
from pathlib import Path

from word2word import Word2word
from wordfreq import top_n_list

OUT = Path("android-civil-leitner/app/src/main/assets")
OUT.mkdir(parents=True, exist_ok=True)

CURATED = [
    ("بيع","خرید و فروش"),("مبيع","مورد معامله در بیع"),("ثمن","بهای معامله"),("بائع","فروشنده"),("مشتري","خریدار"),
    ("عقد","قرارداد"),("عقود","قراردادها"),("معاملة","معامله"),("معاملات","معاملات"),("إيجاب","ایجاب"),
    ("قبول","قبول"),("قصد","قصد"),("رضا","رضایت"),("أهلية","اهلیت"),("بلوغ","بلوغ"),("عقل","عقل"),
    ("رشد","رشد"),("إكراه","اکراه"),("اضطرار","اضطرار"),("غلط","اشتباه"),("جهالة","جهالت / مجهول بودن"),
    ("غرر","غرر / خطر ناشی از ابهام"),("شرط","شرط"),("شروط","شروط"),("صفة","صفت"),("فعل","فعل"),
    ("نتيجة","نتیجه"),("فسخ","فسخ"),("إقالة","اقاله"),("بطلان","بطلان"),("باطل","باطل"),
    ("صحيح","صحیح"),("نافذ","نافذ"),("موقوف","موقوف / وابسته به اجازه"),("لزوم","لزوم"),("جواز","جواز"),
    ("لازم","لازم"),("جائز","جایز"),("خيار","خیار"),("خيارات","خیارات"),("عيب","عیب"),
    ("غبن","غبن"),("تدليس","تدلیس"),("رؤية","رؤیت"),("تأخير","تأخیر"),("مجلس","مجلس"),
    ("حيوان","حیوان"),("تفليس","تفلیس"),("تبعض","تبعض"),("صفقة","صفقه / معامله"),("ضمان","ضمان"),
    ("ضامن","ضامن"),("مضمون","مضمون"),("حوالة","حواله"),("محيل","حواله‌دهنده"),("محتال","طلبکار در حواله"),
    ("كفالة","کفالت"),("كفيل","کفیل"),("رهن","رهن"),("راهن","راهن"),("مرتهن","مرتهن"),
    ("وديعة","ودیعه"),("مودع","ودیعه‌گذار"),("مستودع","امین در ودیعه"),("عارية","عاریه"),("مستعير","عاریه‌گیرنده"),
    ("وكالة","وکالت"),("وكيل","وکیل"),("موكل","موکل"),("شركة","شرکت"),("شريك","شریک"),
    ("مضاربة","مضاربه"),("عامل","عامل"),("مالك","مالک"),("مزارعة","مزارعه"),("زارع","زارع"),
    ("مساقاة","مساقات"),("إجارة","اجاره"),("أجير","اجیر"),("مؤجر","موجر"),("مستأجر","مستأجر"),
    ("منفعة","منفعت"),("عين","عین"),("مال","مال"),("أموال","اموال"),("ملك","ملک / مالکیت"),
    ("ملكية","مالکیت"),("تصرف","تصرف"),("غصب","غصب"),("غاصب","غاصب"),("إتلاف","اتلاف"),
    ("تسبيب","تسبیب"),("استيفاء","استیفا"),("دين","دین / بدهی"),("مدين","مدیون"),("دائن","طلبکار"),
    ("وفاء","وفای به عهد"),("إبراء","ابراء"),("مقاصة","تهاتر"),("تعهد","تعهد"),("التزام","الزام / تعهد"),
    ("ملتزم","متعهد"),("حق","حق"),("حقوق","حقوق"),("ملزم","ملزم"),("مسؤولية","مسئولیت"),
    ("ضرر","ضرر"),("سبب","سبب"),("تقصير","تقصیر"),("تعويض","جبران / خسارت"),("صلح","صلح"),
    ("جعالة","جعاله"),("جاعل","جاعل"),("قرض","قرض"),("مقرض","قرض‌دهنده"),("مقترض","قرض‌گیرنده"),
    ("هبة","هبه"),("واهب","واهب"),("موهوب","موهوب"),("وقف","وقف"),("واقف","واقف"),
    ("وصية","وصیت"),("موصي","وصیت‌کننده"),("وصي","وصی"),("إرث","ارث"),("وارث","وارث"),
    ("تركة","ترکه"),("حجب","حجب"),("شفعة","شفعه"),("شفيع","شفیع"),("نكاح","نکاح"),
    ("مهر","مهر"),("نفقة","نفقه"),("طلاق","طلاق"),("عدة","عده"),("نسب","نسب"),("حضانة","حضانت"),
    ("ولاية","ولایت"),("ولي","ولی"),("قوامة","قیمومت"),("قيم","قیم"),("حجر","حجر"),
    ("صغير","صغیر"),("مجنون","مجنون"),("سفيه","سفیه"),("إقرار","اقرار"),("بينة","بینه"),
    ("شهادة","شهادت"),("شاهد","شاهد"),("يمين","سوگند"),("دعوى","دعوا"),("قضاء","قضا"),
    ("حكم","حکم"),("قاضي","قاضی"),("مدعي","خواهان / مدعی"),("مدعى عليه","خوانده / مدعی‌علیه"),
]

def normalize_ar(s: str) -> str:
    s = re.sub(r"[\u064b-\u065f\u0670\u06d6-\u06ed]", "", s)
    s = s.replace("ـ", "").strip()
    return re.sub(r"\s+", " ", s)

def acceptable_ar(s: str) -> bool:
    return bool(re.fullmatch(r"[\u0621-\u064a\u066e-\u06d3 ]{2,30}", s))

pairs = []
seen = set()
for ar, fa in CURATED:
    ar = normalize_ar(ar)
    if ar not in seen:
        seen.add(ar)
        pairs.append((ar, fa, "CURATED_FIQH_PRIORITY"))

translator = Word2word("ar", "fa")
for raw in top_n_list("ar", 12000):
    if len(pairs) >= 1000:
        break
    ar = normalize_ar(raw)
    if not acceptable_ar(ar) or ar in seen or " " in ar:
        continue
    try:
        choices = translator(ar)
    except Exception:
        continue
    meaning = next((str(x).strip() for x in choices if str(x).strip() and str(x).strip() != ar), "")
    if not meaning:
        continue
    seen.add(ar)
    pairs.append((ar, meaning, "WORD2WORD_AR_FA"))

if len(pairs) < 1000:
    raise RuntimeError(f"Arabic-Persian bank incomplete: {len(pairs)} < 1000")

pairs = pairs[:1000]
cards = [
    {
        "id": f"ARABIC:{i:04d}",
        "domain": "ARABIC",
        "ordinal": i,
        "title": ar,
        "prompt": ar,
        "answer": fa,
        "explanation": "",
        "sourceName": "Curated fiqh priority + word2word ar-fa bilingual lexicon",
        "sourceUrl": "https://github.com/kakaobrain/word2word",
        "verificationStatus": source,
    }
    for i, (ar, fa, source) in enumerate(pairs, 1)
]

(OUT / "arabic_vocab_cards.json").write_text(
    json.dumps(cards, ensure_ascii=False, indent=2) + "\n",
    encoding="utf-8"
)
(OUT / "arabic_vocab_manifest.json").write_text(
    json.dumps({
        "count": len(cards),
        "curatedFiqhPriorityCount": sum(1 for x in cards if x["verificationStatus"] == "CURATED_FIQH_PRIORITY"),
        "uniqueArabic": len({x["prompt"] for x in cards}),
        "source": "word2word ar-fa + original curated fiqh priority terms",
    }, ensure_ascii=False, indent=2) + "\n",
    encoding="utf-8"
)

print(json.dumps({
    "ARABIC_1000_GATE": "PASS",
    "count": len(cards),
    "fiqhPriority": sum(1 for x in cards if x["verificationStatus"] == "CURATED_FIQH_PRIORITY"),
}, ensure_ascii=False))
