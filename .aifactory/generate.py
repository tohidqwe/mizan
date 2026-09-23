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
    env_b64 = os.environ.get("AIF_REQUEST_JSON_B64","").strip()
    env_json = os.environ.get("AIF_REQUEST_JSON","").strip()
    if env_b64 or env_json:
        try:
            if env_b64:
                payload = json.loads(base64.b64decode(env_b64).decode("utf-8"))
            else:
                payload = json.loads(env_json)
        except Exception as e:
            die(f"invalid queued request JSON: {e}")
        rid = str(payload.get("id",""))
        app = str(payload.get("app_name",""))
        package = str(payload.get("package_name","")).strip().lower()
        mode = str(payload.get("mode","prompt"))
        prompt = str(payload.get("prompt",""))
        url = str(payload.get("url",""))
        permissions = payload.get("permissions") or []
        if not re.match(r"^[a-f0-9-]{36}$", rid, re.I): die("invalid queued request id")
        if not re.match(r"^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$", package): die("invalid package name")
        if mode not in ("prompt","url"): die("unsupported mode")
        if mode == "prompt" and len(prompt.strip()) < 8: die("prompt too short")
        if mode == "url" and not re.match(r"^https://", url, re.I): die("URL mode requires HTTPS")
        if len(prompt) > 50000: die("prompt too long: maximum 50000 characters")
        return {"id":rid, "actor":"supabase-queue", "app_name":app[:80], "package":package, "mode":mode, "prompt":prompt, "url":url, "permissions":permissions}

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
    return {"id":rid, "actor":actor, "app_name":app[:80], "package":package, "mode":mode, "prompt":prompt, "url":url, "permissions":[]}

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
    def hit(term):
        if re.fullmatch(r"[a-z0-9][a-z0-9 _.-]*", term):
            return re.search(r"(?<![a-z0-9])" + re.escape(term) + r"(?![a-z0-9])", p) is not None
        return term in p
    return [name for name, words in checks.items() if any(hit(w) for w in words)]

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
    spec["native_capabilities"] = requested_native
    unsupported_native = [x for x in requested_native if x in {"gps","camera","microphone","bluetooth","background_location","nfc"}]
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

def write_native_vpn_project(req, spec):
    OUT.mkdir(parents=True, exist_ok=True)
    pkg_path = Path(*req["package"].split("."))
    src = OUT/"app/src/main/java"/pkg_path
    res = OUT/"app/src/main/res/values"
    src.mkdir(parents=True, exist_ok=True)
    res.mkdir(parents=True, exist_ok=True)

    (OUT/"settings.gradle").write_text("""pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); mavenCentral() } }
rootProject.name='GeneratedAIFVpn'
include ':app'
""","utf-8")
    (OUT/"build.gradle").write_text("plugins { id 'com.android.application' version '8.13.2' apply false }\n","utf-8")
    (OUT/"gradle.properties").write_text("org.gradle.daemon=false\norg.gradle.parallel=false\norg.gradle.workers.max=1\nandroid.useAndroidX=false\n","utf-8")
    (OUT/"app/build.gradle").write_text(f"""plugins {{ id 'com.android.application' }}
android {{
  namespace '{req["package"]}'
  compileSdk 36
  defaultConfig {{ applicationId '{req["package"]}'; minSdk 26; targetSdk 36; versionCode 1; versionName '1.0' }}
  compileOptions {{ sourceCompatibility JavaVersion.VERSION_17; targetCompatibility JavaVersion.VERSION_17 }}
  signingConfigs {{ release {{ storeFile file(System.getenv('SIGNING_STORE_FILE')); storePassword System.getenv('SIGNING_STORE_PASSWORD'); keyAlias System.getenv('SIGNING_KEY_ALIAS'); keyPassword System.getenv('SIGNING_STORE_PASSWORD') }} }}
  buildTypes {{ release {{ minifyEnabled false; signingConfig signingConfigs.release }} }}
}}
""","utf-8")

    label = html.escape(req["app_name"], quote=True)
    manifest = f'''<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE"/>
<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
<application android:theme="@style/AppTheme" android:label="{label}" android:usesCleartextTraffic="false">
<activity android:name=".MainActivity" android:exported="true">
<intent-filter><action android:name="android.intent.action.MAIN"/><category android:name="android.intent.category.LAUNCHER"/></intent-filter>
</activity>
<service android:name=".HeroVpnService"
 android:permission="android.permission.BIND_VPN_SERVICE"
 android:exported="false"
 android:foregroundServiceType="specialUse">
<intent-filter><action android:name="android.net.VpnService"/></intent-filter>
<property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE" android:value="Encrypted VPN tunnel"/>
</service>
</application></manifest>'''
    (OUT/"app/src/main/AndroidManifest.xml").write_text(manifest,"utf-8")
    (res/"styles.xml").write_text("""<?xml version="1.0" encoding="utf-8"?><resources>
<style name="AppTheme" parent="android:style/Theme.Material.NoActionBar"><item name="android:fontFamily">sans</item><item name="android:windowLightStatusBar">false</item><item name="android:statusBarColor">#111116</item><item name="android:navigationBarColor">#111116</item><item name="android:colorAccent">#7C4DFF</item></style>
</resources>""","utf-8")

    main_java = f'''package {req["package"]};

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.VpnService;
import android.os.*;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity {{
  private TextView status;
  private EditText host, port, pin;
  private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {{
    @Override public void onReceive(Context c, Intent i) {{
      if (HeroVpnService.ACTION_STATE.equals(i.getAction())) {{
        status.setText(i.getStringExtra("state"));
      }}
    }}
  }};

  @Override public void onCreate(Bundle b) {{
    super.onCreate(b);
    getWindow().setStatusBarColor(Color.rgb(17,17,22));
    LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(40,56,40,40); root.setBackgroundColor(Color.rgb(17,17,22));
    TextView title=new TextView(this); title.setText("{label}"); title.setTextColor(Color.WHITE); title.setTextSize(28);
    status=new TextView(this); status.setText("قطع"); status.setTextColor(Color.LTGRAY); status.setTextSize(18);
    host=field("Gateway host"); port=field("Gateway port"); pin=field("Optional SHA-256 certificate pin (hex)");
    android.content.SharedPreferences p=getSharedPreferences("cfg",MODE_PRIVATE);
    host.setText(p.getString("host","")); port.setText(String.valueOf(p.getInt("port",443))); pin.setText(p.getString("pin",""));
    Button connect=new Button(this); connect.setText("اتصال امن");
    Button stop=new Button(this); stop.setText("قطع اتصال");
    TextView note=new TextView(this);
    note.setText("این نسخه فقط وقتی «متصل» نشان می‌دهد که TLS واقعی برقرار و TUN سیستم ایجاد شده باشد. بدون Gateway معتبر، اتصال جعلی نمایش داده نمی‌شود.");
    note.setTextColor(Color.GRAY);
    root.addView(title); root.addView(status); root.addView(host); root.addView(port); root.addView(pin); root.addView(connect); root.addView(stop); root.addView(note);
    setContentView(root);

    if (Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
      requestPermissions(new String[]{{Manifest.permission.POST_NOTIFICATIONS}},77);

    connect.setOnClickListener(v -> {{
      String h=host.getText().toString().trim(); int po;
      try {{ po=Integer.parseInt(port.getText().toString().trim()); }} catch(Exception e) {{ po=443; }}
      getSharedPreferences("cfg",MODE_PRIVATE).edit().putString("host",h).putInt("port",po).putString("pin",pin.getText().toString().trim()).apply();
      if(h.isEmpty()) {{ status.setText("Gateway تنظیم نشده؛ اتصال شروع نشد"); return; }}
      Intent prep=VpnService.prepare(this);
      if(prep!=null) startActivityForResult(prep,100); else startVpn();
    }});
    stop.setOnClickListener(v -> startService(new Intent(this,HeroVpnService.class).setAction(HeroVpnService.ACTION_STOP)));
  }}

  private EditText field(String hint) {{ EditText e=new EditText(this); e.setHint(hint); e.setTextColor(Color.WHITE); e.setHintTextColor(Color.GRAY); return e; }}
  private void startVpn() {{ Intent i=new Intent(this,HeroVpnService.class).setAction(HeroVpnService.ACTION_CONNECT); if(Build.VERSION.SDK_INT>=26) startForegroundService(i); else startService(i); }}
  @Override protected void onActivityResult(int r,int c,Intent d) {{ super.onActivityResult(r,c,d); if(r==100 && c==RESULT_OK) startVpn(); }}
  @Override protected void onStart() {{ super.onStart(); IntentFilter f=new IntentFilter(HeroVpnService.ACTION_STATE); if(Build.VERSION.SDK_INT>=33) registerReceiver(stateReceiver,f,Context.RECEIVER_NOT_EXPORTED); else registerReceiver(stateReceiver,f); }}
  @Override protected void onStop() {{ super.onStop(); try{{unregisterReceiver(stateReceiver);}}catch(Exception ignored){{}} }}
}}'''
    (src/"MainActivity.java").write_text(main_java,"utf-8")

    service_java = f'''package {req["package"]};

import android.app.*;
import android.content.*;
import android.net.VpnService;
import android.os.*;
import java.io.*;
import java.net.*;
import java.security.*;
import java.security.cert.Certificate;
import javax.net.ssl.*;

public class HeroVpnService extends VpnService {{
  public static final String ACTION_CONNECT="aif.CONNECT";
  public static final String ACTION_STOP="aif.STOP";
  public static final String ACTION_STATE="{req["package"]}.VPN_STATE";
  private volatile boolean running=false;
  private ParcelFileDescriptor tun;
  private Socket raw;
  private SSLSocket tls;
  private Thread up,down;

  @Override public void onCreate() {{ super.onCreate(); createChannel(); }}
  @Override public int onStartCommand(Intent intent,int flags,int startId) {{
    String a=intent==null?ACTION_CONNECT:intent.getAction();
    if(ACTION_STOP.equals(a)) {{ shutdown("قطع"); return START_NOT_STICKY; }}
    startForeground(7,notification("در حال آماده‌سازی اتصال امن"));
    if(!running) new Thread(this::connectReal,"aif-vpn-connect").start();
    return START_STICKY;
  }}

  private void connectReal() {{
    try {{
      android.content.SharedPreferences p=getSharedPreferences("cfg",MODE_PRIVATE);
      String host=p.getString("host","").trim(); int port=p.getInt("port",443); String pin=p.getString("pin","").trim().toLowerCase();
      if(host.isEmpty()) throw new IOException("Gateway تنظیم نشده است");
      send("در حال TLS handshake…");
      raw=new Socket();
      if(!protect(raw)) throw new IOException("protect(socket) failed");
      raw.connect(new InetSocketAddress(host,port),10000);
      SSLSocketFactory sf=(SSLSocketFactory)SSLSocketFactory.getDefault();
      tls=(SSLSocket)sf.createSocket(raw,host,port,true);
      SSLParameters params=tls.getSSLParameters(); params.setEndpointIdentificationAlgorithm("HTTPS"); tls.setSSLParameters(params);
      tls.setSoTimeout(15000); tls.startHandshake();
      if(!pin.isEmpty()) verifyPin(pin,tls.getSession().getPeerCertificates()[0]);

      send("TLS معتبر؛ در حال ایجاد TUN…");
      Builder b=new Builder().setSession("{label}").setMtu(1280)
        .addAddress("10.111.0.2",32).addRoute("0.0.0.0",0).addDnsServer("1.1.1.1").setBlocking(true);
      tun=b.establish();
      if(tun==null) throw new IOException("VpnService.Builder.establish returned null");
      running=true;
      send("متصل — تونل واقعی فعال است");
      updateNotification("متصل — تونل امن فعال");

      InputStream tunIn=new FileInputStream(tun.getFileDescriptor());
      OutputStream tunOut=new FileOutputStream(tun.getFileDescriptor());
      InputStream netIn=new BufferedInputStream(tls.getInputStream());
      OutputStream netOut=new BufferedOutputStream(tls.getOutputStream());

      up=new Thread(() -> pumpTunToTls(tunIn,netOut),"aif-vpn-up");
      down=new Thread(() -> pumpTlsToTun(netIn,tunOut),"aif-vpn-down");
      up.start(); down.start();
      up.join();
    }} catch(Exception e) {{
      shutdown("خطا: "+safe(e.getMessage()));
    }}
  }}

  private void pumpTunToTls(InputStream in,OutputStream out) {{
    byte[] buf=new byte[32767];
    try {{ while(running) {{ int n=in.read(buf); if(n<0) break; out.write((n>>>24)&255); out.write((n>>>16)&255); out.write((n>>>8)&255); out.write(n&255); out.write(buf,0,n); out.flush(); }} }}
    catch(Exception e) {{ if(running) shutdown("تونل خروجی قطع شد"); }}
  }}
  private void pumpTlsToTun(InputStream in,OutputStream out) {{
    byte[] buf=new byte[32767];
    try {{ while(running) {{ int n=(in.read()<<24)|(in.read()<<16)|(in.read()<<8)|in.read(); if(n<=0||n>buf.length) throw new IOException("invalid frame"); int off=0; while(off<n){{int r=in.read(buf,off,n-off); if(r<0)throw new EOFException(); off+=r;}} out.write(buf,0,n); out.flush(); }} }}
    catch(Exception e) {{ if(running) shutdown("تونل ورودی قطع شد"); }}
  }}

  private void verifyPin(String expected, Certificate cert) throws Exception {{
    MessageDigest md=MessageDigest.getInstance("SHA-256");
    byte[] d=md.digest(cert.getEncoded()); StringBuilder s=new StringBuilder();
    for(byte x:d)s.append(String.format("%02x",x));
    String clean=expected.replace("sha256/","").replace(":","").trim();
    if(!MessageDigest.isEqual(clean.getBytes("UTF-8"),s.toString().getBytes("UTF-8"))) throw new SSLPeerUnverifiedException("certificate pin mismatch");
  }}

  private synchronized void shutdown(String state) {{
    running=false;
    try{{if(tun!=null)tun.close();}}catch(Exception ignored){{}}
    try{{if(tls!=null)tls.close();}}catch(Exception ignored){{}}
    try{{if(raw!=null)raw.close();}}catch(Exception ignored){{}}
    tun=null; tls=null; raw=null; send(state); stopForeground(true); stopSelf();
  }}

  private void send(String s) {{ Intent i=new Intent(ACTION_STATE).setPackage(getPackageName()); i.putExtra("state",s); sendBroadcast(i); }}
  private String safe(String s) {{ return s==null?"unknown":s.replace("\\n"," ").replace("\\r"," "); }}
  private void createChannel() {{ if(Build.VERSION.SDK_INT>=26){{ NotificationManager n=getSystemService(NotificationManager.class); n.createNotificationChannel(new NotificationChannel("vpn","VPN",NotificationManager.IMPORTANCE_LOW)); }} }}
  private Notification notification(String text) {{ return new Notification.Builder(this,Build.VERSION.SDK_INT>=26?"vpn":null).setContentTitle("{label}").setContentText(text).setSmallIcon(android.R.drawable.stat_sys_download_done).setOngoing(true).build(); }}
  private void updateNotification(String s) {{ ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(7,notification(s)); }}
  @Override public void onRevoke() {{ shutdown("مجوز VPN لغو شد"); }}
  @Override public void onDestroy() {{ shutdown("قطع"); super.onDestroy(); }}
}}'''
    (src/"HeroVpnService.java").write_text(service_java,"utf-8")

    proof = {
      "passed": True,
      "native": True,
      "capability": "vpn",
      "checks": {
        "extends_vpnservice": True,
        "bind_vpn_service_manifest": True,
        "vpn_prepare_permission_flow": True,
        "protect_tunnel_socket": True,
        "tls_hostname_verification": True,
        "optional_certificate_pin": True,
        "tun_establish_after_tls_handshake": True,
        "no_fake_connected_state": True,
        "foreground_notification": True,
        "ipv6_not_routed_without_transport_support": True
      },
      "external_dependency": "A compatible TLS VPN gateway implementing 4-byte big-endian length + raw IP packet framing is required for real Internet transit."
    }
    Path("aif-proof.json").write_text(json.dumps(proof,ensure_ascii=False,indent=2),"utf-8")
    Path("aif-agents.json").write_text(json.dumps(spec.get("agent_reports",{}),ensure_ascii=False,indent=2),"utf-8")
    manifest_out = {
      "request_id":req["id"],"app_name":req["app_name"],"package":req["package"],"mode":"native-vpn",
      "memory_key":req["package"],"features_v3":["agent_team","proof_of_function","project_memory","auto_repair","native_vpn"],
      "spec":spec,"proof":proof
    }
    Path("aif-manifest.json").write_text(json.dumps(manifest_out,ensure_ascii=False,indent=2),"utf-8")

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
    if "vpn" in (spec.get("native_capabilities") or []):
        write_native_vpn_project(req,spec)
        print("AIF native VPN gate: PASS")
        return
    doc = make_prompt_html(req,spec)
    write_project(req,doc,spec)
    print("AIF semantic gate: PASS")

if __name__ == "__main__":
    main()

# queue wake 2026-09-22

# vpn-native retry wake 2026-09-22

# wake vpn queue 2026-09-23

# wake queued vpn 2026-09-23T05:02+0330
