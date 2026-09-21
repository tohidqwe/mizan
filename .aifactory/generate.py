#!/usr/bin/env python3
import base64, html, json, os, re, sys, time
from pathlib import Path
import requests

EVENT = Path(os.environ.get("GITHUB_EVENT_PATH", ""))
OUT = Path(os.environ.get("AIF_OUT", "generated-app"))
API = "https://text.pollinations.ai/openai"

def die(msg):
    print("AIF ERROR:", msg, file=sys.stderr)
    raise SystemExit(2)

def b64d(v):
    try:
        return base64.b64decode(v.encode(), validate=True).decode("utf-8")
    except Exception as e:
        die(f"invalid base64 field: {e}")

def parse_request():
    ev = json.loads(EVENT.read_text("utf-8"))
    issue = ev["issue"]
    title = issue.get("title","")
    actor = ev.get("sender",{}).get("login","")
    m = re.match(r"^\[AIF-V2:([a-f0-9-]{36})\]\s*(.*)$", title, re.I)
    if not m: die("invalid issue title")
    fields = {}
    for line in (issue.get("body") or "").splitlines():
        if "=" in line:
            k,v = line.split("=",1)
            fields[k.strip()] = v.strip()
    if fields.get("protocol") != "AIF_REQUEST_V2": die("unsupported protocol")
    rid = fields.get("request_id","")
    if rid.lower() != m.group(1).lower(): die("request id mismatch")
    app = b64d(fields.get("app_name_b64",""))
    prompt = b64d(fields.get("prompt_b64","")) if fields.get("prompt_b64") else ""
    url = b64d(fields.get("url_b64","")) if fields.get("url_b64") else ""
    mode = fields.get("mode","prompt")
    package = fields.get("package_name","").strip().lower()
    if not re.match(r"^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$", package):
        die("invalid package name")
    if mode not in ("prompt","url"): die("unsupported mode")
    if mode == "prompt" and len(prompt.strip()) < 8: die("prompt too short")
    if mode == "url" and not re.match(r"^https://", url, re.I): die("URL mode requires HTTPS")
    if len(prompt) > 6000: die("prompt too long")
    return {"id":rid, "actor":actor, "app_name":app[:80], "package":package, "mode":mode, "prompt":prompt, "url":url}

def ai(messages, model="openai", temperature=0.35, max_tokens=7000):
    payload = {"model":model,"messages":messages,"temperature":temperature,"max_tokens":max_tokens,"private":True}
    last = None
    for attempt in range(4):
        try:
            r = requests.post(API, json=payload, timeout=150)
            r.raise_for_status()
            data = r.json()
            text = data.get("choices",[{}])[0].get("message",{}).get("content","")
            if text and len(text) > 40:
                return text.strip()
            last = RuntimeError("empty AI response")
        except Exception as e:
            last = e
        time.sleep(3 + attempt * 3)
    raise RuntimeError(f"AI generation unavailable after retries: {last}")

def strip_fence(s):
    s = s.strip()
    ticks = chr(96) * 3
    if s.startswith(ticks):
        first = s.find("\n")
        if first >= 0: s = s[first+1:]
    if s.rstrip().endswith(ticks):
        s = s.rstrip()[:-3]
    return s.strip()

def extract_json(s):
    s = strip_fence(s)
    a,b = s.find("{"), s.rfind("}")
    if a < 0 or b <= a: raise ValueError("no JSON object")
    return json.loads(s[a:b+1])

def planner(req):
    system = """You are a senior product architect for Android apps. Convert the user's exact request into a concrete implementation plan.
Return ONLY valid JSON. Never collapse a domain-specific request into a notes/todo app.
Required schema:
{
 "domain":"specific domain name",
 "summary":"one sentence",
 "screens":[{"id":"ascii-id","title":"visible screen title","purpose":"what user does"}],
 "entities":[{"name":"entity","fields":["field:type"]}],
 "features":["functional feature"],
 "workflows":["user workflow"],
 "acceptance":["testable acceptance condition"],
 "visual_direction":"concise UI direction"
}
Rules: 4-8 distinct screens for non-trivial apps; entities and workflows must be specific to the prompt; no fake cloud/backend claims; generated app will work offline with localStorage."""
    raw = ai([{"role":"system","content":system},{"role":"user","content":req["prompt"]}], max_tokens=3000)
    spec = extract_json(raw)
    screens = spec.get("screens") or []
    if len(screens) < 3: raise RuntimeError("AI plan is too generic: fewer than 3 screens")
    domain = str(spec.get("domain","")).strip().lower()
    p = req["prompt"].lower()
    note_requested = any(x in p for x in ["یادداشت","note","notes","todo","to-do","وظایف","task"])
    if not note_requested and domain in {"notes","note app","todo","to-do","task manager","یادداشت"}:
        raise RuntimeError("AI plan incorrectly collapsed request into a notes app")
    return spec

def clean_html(s):
    s = strip_fence(s)
    low = s.lower()
    start = low.find("<!doctype")
    if start < 0: start = low.find("<html")
    if start > 0: s = s[start:]
    end = s.lower().rfind("</html>")
    if end >= 0: s = s[:end+7]
    return s

def validate_html(doc, spec, req):
    problems = []
    low = doc.lower()
    if "<html" not in low or "</html>" not in low: problems.append("missing complete html document")
    if "localstorage" not in low and len(spec.get("entities") or []) > 0: problems.append("no local persistence")
    if re.search(r'https?://', doc, re.I): problems.append("external network URL present")
    forbidden = ["fetch(","xmlhttprequest","websocket","<iframe","<object","<embed","eval(","new function("]
    for x in forbidden:
        if x in low: problems.append("forbidden capability: "+x)
    screen_count = len(re.findall(r'data-aif-screen\s*=', doc, re.I))
    expected = min(3, len(spec.get("screens") or []))
    if screen_count < expected: problems.append(f"only {screen_count} screen containers, expected at least {expected}")
    controls = len(re.findall(r'<(?:button|input|select|textarea)\b', doc, re.I))
    if controls < 6: problems.append("too few interactive controls")
    titles = [str(x.get("title","")).strip() for x in spec.get("screens",[]) if x.get("title")]
    matched = sum(1 for t in titles if t.lower() in low)
    if matched < min(3, len(titles)): problems.append("screen titles from architecture are missing")
    p = req["prompt"].lower()
    note_requested = any(x in p for x in ["یادداشت","note","notes","todo","to-do","وظایف","task"])
    if not note_requested:
        note_hits = sum(low.count(x) for x in ["یادداشت جدید","notes app","new note","todo list"])
        if note_hits >= 2: problems.append("generic notes/todo template detected")
    return problems

def make_prompt_html(req, spec):
    system = """You are an expert front-end engineer creating the actual UI and logic for an Android APK.
Return ONLY a complete single-file HTML document. It runs offline inside Android WebView.
MANDATORY:
- Implement the supplied architecture faithfully; do not replace it with a notes/todo template.
- Persian RTL when user prompt is Persian.
- Use multiple real screens/views with navigation. Every screen root MUST have data-aif-screen="screen-id".
- Implement forms, lists, domain calculations/workflows, filters, search, status changes and useful empty states as appropriate.
- Persist app data in localStorage.
- Inline CSS and JavaScript only. No CDN, no external fonts/images/scripts.
- No fetch, XMLHttpRequest, WebSocket, iframe, object, embed, eval or Function constructor.
- No fake server, login or AI claims. Everything must work locally.
- Mobile-first polished UI, accessible controls, clear feedback.
- Screen titles in the plan must appear verbatim in visible UI.
"""
    user = "USER REQUEST:\n"+req["prompt"]+"\n\nAPP NAME:\n"+req["app_name"]+"\n\nARCHITECTURE JSON:\n"+json.dumps(spec,ensure_ascii=False)
    doc = clean_html(ai([{"role":"system","content":system},{"role":"user","content":user}], temperature=0.4, max_tokens=10000))
    for repair in range(2):
        problems = validate_html(doc,spec,req)
        if not problems: return doc
        repair_prompt = "Repair this offline single-file app so it satisfies every listed problem and the architecture. Return ONLY the full corrected HTML.\nPROBLEMS:\n- " + "\n- ".join(problems) + "\nARCHITECTURE:\n" + json.dumps(spec,ensure_ascii=False) + "\nCURRENT HTML:\n" + doc[:45000]
        doc = clean_html(ai([{"role":"system","content":"You are a strict senior UI engineer. Preserve working domain features and fix all validation failures."},{"role":"user","content":repair_prompt}], temperature=0.25, max_tokens=10000))
    problems = validate_html(doc,spec,req)
    if problems: raise RuntimeError("generated app failed semantic gate: " + "; ".join(problems))
    return doc

def write_project(req, index_html, spec=None):
    OUT.mkdir(parents=True, exist_ok=True)
    pkg_path = Path(*req["package"].split("."))
    (OUT/"app/src/main/java"/pkg_path).mkdir(parents=True, exist_ok=True)
    (OUT/"app/src/main/res/values").mkdir(parents=True, exist_ok=True)
    (OUT/"app/src/main/assets/www").mkdir(parents=True, exist_ok=True)

    (OUT/"settings.gradle").write_text("""pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); mavenCentral() } }
rootProject.name='GeneratedAIFApp'
include ':app'
""","utf-8")
    (OUT/"build.gradle").write_text("plugins { id 'com.android.application' version '8.13.2' apply false }\n","utf-8")
    (OUT/"gradle.properties").write_text("org.gradle.daemon=false\norg.gradle.parallel=false\norg.gradle.workers.max=1\nandroid.useAndroidX=false\n","utf-8")
    (OUT/"app/build.gradle").write_text(f"""plugins {{ id 'com.android.application' }}
android {{
  namespace '{req["package"]}'
  compileSdk 36
  defaultConfig {{ applicationId '{req["package"]}'; minSdk 26; targetSdk 36; versionCode 1; versionName '1.0' }}
  signingConfigs {{ release {{ storeFile file(System.getenv('SIGNING_STORE_FILE')); storePassword System.getenv('SIGNING_STORE_PASSWORD'); keyAlias System.getenv('SIGNING_KEY_ALIAS'); keyPassword System.getenv('SIGNING_STORE_PASSWORD') }} }}
  buildTypes {{ release {{ minifyEnabled false; signingConfig signingConfigs.release }} }}
}}
""","utf-8")
    internet = req["mode"] == "url"
    permission = '<uses-permission android:name="android.permission.INTERNET"/>' if internet else ''
    manifest = f'''<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
{permission}
<application android:theme="@style/AppTheme" android:label="{html.escape(req["app_name"], quote=True)}" android:usesCleartextTraffic="false">
<activity android:name=".MainActivity" android:exported="true">
<intent-filter><action android:name="android.intent.action.MAIN"/><category android:name="android.intent.category.LAUNCHER"/></intent-filter>
</activity></application></manifest>'''
    (OUT/"app/src/main/AndroidManifest.xml").write_text(manifest,"utf-8")
    (OUT/"app/src/main/res/values/styles.xml").write_text("""<?xml version="1.0" encoding="utf-8"?><resources>
<style name="AppTheme" parent="android:style/Theme.Material.Light.NoActionBar"><item name="android:fontFamily">sans</item><item name="android:windowLightStatusBar">false</item><item name="android:statusBarColor">#111116</item><item name="android:navigationBarColor">#111116</item></style>
</resources>""","utf-8")
    load = req["url"] if internet else "file:///android_asset/www/index.html"
    quoted_load = json.dumps(load)
    java = f'''package {req["package"]};
import android.app.Activity;import android.os.Bundle;import android.webkit.WebChromeClient;import android.webkit.WebSettings;import android.webkit.WebView;import android.webkit.WebViewClient;
public class MainActivity extends Activity {{
 private WebView web;
 @Override public void onCreate(Bundle b) {{ super.onCreate(b); web=new WebView(this); setContentView(web);
 WebSettings s=web.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setAllowFileAccess(true);
 s.setAllowContentAccess(false); s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
 web.setWebViewClient(new WebViewClient()); web.setWebChromeClient(new WebChromeClient()); web.loadUrl({quoted_load}); }}
 @Override public void onBackPressed() {{ if(web!=null && web.canGoBack()) web.goBack(); else super.onBackPressed(); }}
}}'''
    (OUT/"app/src/main/java"/pkg_path/"MainActivity.java").write_text(java,"utf-8")
    if not internet:
        (OUT/"app/src/main/assets/www/index.html").write_text(index_html,"utf-8")
    manifest_out = {"request_id":req["id"],"app_name":req["app_name"],"package":req["package"],"mode":req["mode"],"spec":spec}
    Path("aif-manifest.json").write_text(json.dumps(manifest_out,ensure_ascii=False,indent=2),"utf-8")

def main():
    req = parse_request()
    print("REQUEST_ID="+req["id"])
    print("APP_NAME="+req["app_name"])
    if req["mode"] == "url":
        write_project(req, "", {"domain":"web-wrapper","screens":[{"id":"web","title":req["app_name"],"purpose":"website"}]})
        return
    spec = planner(req)
    print("DOMAIN="+str(spec.get("domain","")))
    doc = make_prompt_html(req,spec)
    write_project(req,doc,spec)
    print("AIF semantic gate: PASS")

if __name__ == "__main__":
    main()
