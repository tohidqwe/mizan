#!/usr/bin/env python3
import base64, gzip, html, json, os, re, subprocess, sys, time
from pathlib import Path

EVENT = Path(os.environ.get("GITHUB_EVENT_PATH", ""))
OUT = Path(os.environ.get("AIF_OUT", "generated-app"))

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
    m = re.match(r"^\[AIF-V2:([a-f0-9-]{36})\](?:\s+.*)?$", title, re.I)
    if not m: die("invalid issue title")
    fields = {}
    for line in (issue.get("body") or "").splitlines():
        if "=" in line:
            k,v = line.split("=",1)
            fields[k.strip()] = v.strip()

    protocol = fields.get("protocol")
    if protocol == "AIF_REQUEST_V3":
        token = fields.get("payload_gz_b64url","")
        try:
            padded = token + "=" * (-len(token) % 4)
            raw = base64.urlsafe_b64decode(padded.encode())
            payload = json.loads(gzip.decompress(raw).decode("utf-8"))
        except Exception as e:
            die(f"invalid compressed payload: {e}")
        rid = str(payload.get("id",""))
        app = str(payload.get("app_name",""))
        package = str(payload.get("package_name","")).strip().lower()
        mode = str(payload.get("mode","prompt"))
        prompt = str(payload.get("prompt",""))
        url = str(payload.get("url",""))
    elif protocol == "AIF_REQUEST_V2":
        rid = fields.get("request_id","")
        app = b64d(fields.get("app_name_b64",""))
        prompt = b64d(fields.get("prompt_b64","")) if fields.get("prompt_b64") else ""
        url = b64d(fields.get("url_b64","")) if fields.get("url_b64") else ""
        mode = fields.get("mode","prompt")
        package = fields.get("package_name","").strip().lower()
    else:
        die("unsupported protocol")

    if rid.lower() != m.group(1).lower(): die("request id mismatch")
    if not re.match(r"^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$", package):
        die("invalid package name")
    if mode not in ("prompt","url"): die("unsupported mode")
    if mode == "prompt" and len(prompt.strip()) < 8: die("prompt too short")
    if mode == "url" and not re.match(r"^https://", url, re.I): die("URL mode requires HTTPS")
    if len(prompt) > 50000: die("prompt too long: maximum 50000 characters")
    return {"id":rid, "actor":actor, "app_name":app[:80], "package":package, "mode":mode, "prompt":prompt, "url":url}

def ai(messages, model="copilot", temperature=0.35, max_tokens=7000):
    combined = []
    for m in messages:
        role = str(m.get("role","user")).upper()
        combined.append(role + ":\n" + str(m.get("content","")))
    prompt = "\n\n".join(combined)
    prompt += "\n\nReturn only the requested artifact. Do not add commentary."
    last = None
    for attempt in range(2):
        try:
            p = subprocess.run(
                ["copilot", "-p", prompt, "-s", "--no-ask-user"],
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
                timeout=240,
                env=os.environ.copy(),
            )
            out = re.sub(r"\x1b\[[0-9;]*m", "", p.stdout or "").strip()
            if p.returncode == 0 and len(out) > 40:
                return out
            last = RuntimeError((p.stderr or out or ("copilot exit " + str(p.returncode)))[:1200])
        except Exception as e:
            last = e
        time.sleep(4 + attempt * 4)
    raise RuntimeError(f"GitHub Copilot generation unavailable after retries: {last}")

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

def detect_system_capabilities(req):
    p = req["prompt"].lower()
    checks = {
        "vpn": ["vpn", "وی پی ان", "وی‌پی‌ان", "تونل", "v2ray", "wireguard", "openvpn"],
        "gps": ["gps", "location", "موقعیت", "مکان", "لوکیشن", "geofence"],
        "camera": ["camera", "دوربین", "عکس بگیرد", "اسکن"],
        "microphone": ["microphone", "میکروفون", "ضبط صدا", "voice recorder"],
        "bluetooth": ["bluetooth", "بلوتوث", "ble"],
        "background_location": ["background location", "موقعیت پس‌زمینه", "ردیابی مداوم"],
        "notifications": ["notification", "نوتیفیکیشن", "اعلان", "یادآوری"],
        "nfc": ["nfc", "ان اف سی", "ان‌اف‌سی"]
    }
    return [name for name, words in checks.items() if any(w in p for w in words)]

def run_agent(role, task, payload, max_tokens=2800):
    system = f"""You are the {role} inside a senior software delivery team.
Be concrete, skeptical, and faithful to the user's request. Never invent capabilities that the implementation cannot actually provide.
Return ONLY valid JSON."""
    raw = ai([
        {"role":"system","content":system},
        {"role":"user","content":task+"\n\nINPUT:\n"+json.dumps(payload,ensure_ascii=False)}
    ], temperature=0.2, max_tokens=max_tokens)
    return extract_json(raw)

def planner(req):
    product = run_agent(
        "Product Manager",
        """Turn the request into a domain-specific product definition.
Required JSON keys: domain, summary, users, features, workflows, acceptance.
features/workflows/acceptance must be specific and testable. Do not collapse a domain app into notes/todo unless explicitly requested.""",
        {"app_name":req["app_name"],"prompt":req["prompt"]},
        2600
    )

    architect = run_agent(
        "Android Software Architect",
        """Design an implementable offline-first app architecture for the product.
Required JSON keys: screens, entities, state_rules, navigation, visual_direction.
screens must be an array of 4-8 objects with id,title,purpose for non-trivial apps.
entities must be domain-specific. The current lightweight generator can only implement local/offline WebView features; do not claim native system capabilities are implemented.""",
        product,
        3200
    )

    security = run_agent(
        "Security and Capability Engineer",
        """Audit requested capabilities and classify them.
Required JSON keys: system_capabilities, privacy_risks, permission_notes, implementation_constraints.
For every system capability say whether a real native Android implementation is required. Never treat a visual toggle as proof that a capability works.""",
        {"request":req["prompt"],"product":product,"architecture":architect},
        2200
    )

    qa = run_agent(
        "QA Lead",
        """Create release gates for this app.
Required JSON keys: must_prove, negative_checks, regression_risks.
must_prove must contain concrete checks tied to the requested product, and must reject generic notes/todo substitutions.""",
        {"request":req["prompt"],"product":product,"architecture":architect,"security":security},
        2200
    )

    spec = {
        "domain": product.get("domain",""),
        "summary": product.get("summary",""),
        "features": product.get("features",[]),
        "workflows": product.get("workflows",[]),
        "acceptance": product.get("acceptance",[]),
        "screens": architect.get("screens",[]),
        "entities": architect.get("entities",[]),
        "state_rules": architect.get("state_rules",[]),
        "navigation": architect.get("navigation",[]),
        "visual_direction": architect.get("visual_direction",""),
        "security": security,
        "qa": qa,
        "agent_reports": {
            "product_manager": product,
            "architect": architect,
            "security_engineer": security,
            "qa_lead": qa
        }
    }

    screens = spec.get("screens") or []
    if len(screens) < 3:
        raise RuntimeError("Agent Team rejected architecture: fewer than 3 meaningful screens")

    domain = str(spec.get("domain","")).strip().lower()
    p = req["prompt"].lower()
    note_requested = any(x in p for x in ["یادداشت","note","notes","todo","to-do","وظایف","task"])
    if not note_requested and domain in {"notes","note app","todo","to-do","task manager","یادداشت"}:
        raise RuntimeError("Agent Team rejected generic notes/todo substitution")

    requested_native = detect_system_capabilities(req)
    unsupported_native = [x for x in requested_native if x in {"vpn","gps","camera","microphone","bluetooth","background_location","nfc"}]
    if unsupported_native:
        raise RuntimeError(
            "Proof-of-Function blocked fake system capability: "
            + ", ".join(unsupported_native)
            + ". This request requires the native-capability generator; a WebView simulation will not be released."
        )
    return spec

def build_proof_report(doc, spec, req):
    problems = validate_html(doc, spec, req)
    low = doc.lower()
    checks = {
        "complete_html": "<html" in low and "</html>" in low,
        "domain_persistence": ("localstorage" in low) if (spec.get("entities") or []) else True,
        "minimum_screens": len(re.findall(r'data-aif-screen\\s*=', doc, re.I)) >= min(3, len(spec.get("screens") or [])),
        "minimum_controls": len(re.findall(r'<(?:button|input|select|textarea)\\b', doc, re.I)) >= 6,
        "no_external_network": re.search(r'https?://', doc, re.I) is None,
        "not_generic_notes": not any(x in low for x in ["notes app","todo list"]) or any(x in req["prompt"].lower() for x in ["note","todo","یادداشت","وظیفه"])
    }
    return {
        "passed": not problems and all(checks.values()),
        "checks": checks,
        "problems": problems,
        "qa_requirements": spec.get("qa",{}).get("must_prove",[])
    }

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
    for repair in range(3):
        problems = validate_html(doc,spec,req)
        if not problems: return doc
        repair_prompt = "Repair this offline single-file app so it satisfies every listed problem and the architecture. Return ONLY the full corrected HTML.\nPROBLEMS:\n- " + "\n- ".join(problems) + "\nARCHITECTURE:\n" + json.dumps(spec,ensure_ascii=False) + "\nCURRENT HTML:\n" + doc[:45000]
        doc = clean_html(ai([{"role":"system","content":"You are a strict senior UI engineer. Preserve working domain features and fix all validation failures."},{"role":"user","content":repair_prompt}], temperature=0.25, max_tokens=10000))
    problems = validate_html(doc,spec,req)
    if problems:
        raise RuntimeError("Auto-Repair exhausted: " + "; ".join(problems))
    proof = build_proof_report(doc,spec,req)
    if not proof["passed"]:
        raise RuntimeError("Proof-of-Function failed: " + "; ".join(proof["problems"]))
    Path("aif-proof.json").write_text(json.dumps(proof,ensure_ascii=False,indent=2),"utf-8")
    Path("aif-agents.json").write_text(json.dumps(spec.get("agent_reports",{}),ensure_ascii=False,indent=2),"utf-8")
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
    manifest_out = {
        "request_id":req["id"],
        "app_name":req["app_name"],
        "package":req["package"],
        "mode":req["mode"],
        "memory_key":req["package"],
        "memory_version_strategy":"continue_same_package",
        "features_v3":["agent_team","proof_of_function","project_memory","auto_repair"],
        "spec":spec
    }
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
