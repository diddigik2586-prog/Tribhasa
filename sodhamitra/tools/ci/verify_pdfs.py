#!/usr/bin/env python3
"""Checks PDFs produced by the app on a real device: A4 size and orientation, and on EVERY page the exact two footer
lines and a "Page i of N" line. Usage: verify_pdfs.py <dir> [--portrait name ...] [--landscape name ...]"""
import sys, os, re, unicodedata
from pypdf import PdfReader

FOOT1 = "Śodhamitra — Research Planner"
FOOT2 = "Created by © Dr. Diddigi Vamsi Krishna"

def norm(s):
    s = unicodedata.normalize("NFC", s)
    return re.sub(r"\s+", " ", s).strip()

def check(path, landscape):
    r = PdfReader(path)
    n = len(r.pages)
    problems = []
    for i, page in enumerate(r.pages, 1):
        w, h = float(page.mediabox.width), float(page.mediabox.height)
        exp = (842, 595) if landscape else (595, 842)
        if abs(w - exp[0]) > 2 or abs(h - exp[1]) > 2:
            problems.append(f"page {i}: size {w:.0f}x{h:.0f}, expected {exp[0]}x{exp[1]}")
        text = norm(page.extract_text() or "")
        # the capital Ś is the one letter some text extractors cannot map back; every other character must be exact
        if norm(FOOT1) not in text and not re.search(r".odhamitra — Research Planner", text): problems.append(f"page {i}: footer line 1 missing")
        if norm(FOOT2) not in text: problems.append(f"page {i}: footer line 2 missing")
        if not re.search(rf"Page {i} of {n}\b", text): problems.append(f"page {i}: 'Page {i} of {n}' missing")
    return n, problems

if __name__ == "__main__":
    d = sys.argv[1]
    ok = True
    for name in sorted(os.listdir(d)):
        if not name.endswith(".pdf"): continue
        landscape = "landscape" in name
        n, problems = check(os.path.join(d, name), landscape)
        print(f"{name}: {n} pages, {'A4 landscape' if landscape else 'A4 portrait'}: " + ("OK" if not problems else "PROBLEMS"))
        for p in problems[:20]: print("   ", p)
        ok &= not problems and n >= 2
    sys.exit(0 if ok else 1)
