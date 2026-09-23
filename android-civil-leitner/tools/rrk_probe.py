from pathlib import Path
from urllib.request import Request, urlopen
from urllib.error import URLError, HTTPError
import hashlib, json, re, ssl, time

OUT = Path(__file__).resolve().parents[1] / 'rrk-probe'
OUT.mkdir(parents=True, exist_ok=True)

urls = [
    'https://rrk.ir/',
    'https://www.rrk.ir/',
    'http://rrk.ir/',
    'http://www.rrk.ir/',
]
ctx = ssl.create_default_context()
results = []
html = None
final_url = None
for u in urls:
    try:
        req = Request(u, headers={'User-Agent':'Mozilla/5.0 CivilLawVerifier/1.0'})
        with urlopen(req, timeout=25, context=ctx) as r:
            body = r.read(5_000_000)
            record = {
                'requestedUrl': u,
                'finalUrl': r.geturl(),
                'status': getattr(r, 'status', None),
                'contentType': r.headers.get('content-type'),
                'bytes': len(body),
                'sha256': hashlib.sha256(body).hexdigest(),
            }
            results.append(record)
            if b'<html' in body.lower() or b'<!doctype' in body.lower():
                html = body
                final_url = r.geturl()
                (OUT/'root.html').write_bytes(body)
                break
    except Exception as e:
        results.append({'requestedUrl':u,'error':repr(e)})

links=[]
if html:
    text=html.decode('utf-8','ignore')
    for href in re.findall(r'''href=["']([^"']+)["']''', text, flags=re.I):
        if href not in links:
            links.append(href)
    (OUT/'root-links.json').write_text(json.dumps(links[:500],ensure_ascii=False,indent=2),encoding='utf-8')

report={
    'probeEpoch': int(time.time()),
    'results': results,
    'htmlCaptured': html is not None,
    'finalUrl': final_url,
    'linkCount': len(links),
    'civilCodeSearchReady': bool(html),
}
(OUT/'rrk-probe-report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(report,ensure_ascii=False,indent=2))
if not html:
    raise SystemExit('RRK_LIVE_PROBE_FAILED: no reachable HTML entry point')
