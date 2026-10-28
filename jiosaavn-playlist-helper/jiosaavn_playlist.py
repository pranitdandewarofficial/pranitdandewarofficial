#!/usr/bin/env python3
"""Visible-browser JioSaavn playlist helper. Best-effort UI automation; no credential scraping."""
import argparse, csv, re, sys, time
from pathlib import Path
from urllib.parse import quote
from playwright.sync_api import sync_playwright, TimeoutError as PlaywrightTimeoutError

BASE = "https://www.jiosaavn.com"

def norm(s):
    return re.sub(r"\\s+", " ", (s or "").lower()).strip()

def parse_song(line):
    line = line.strip()
    if not line or line.startswith("#"):
        return None
    bits = re.split(r"\\s+[—–-]\\s+", line, maxsplit=1)
    return {"raw": line, "title": bits[0].strip(), "artist": bits[1].strip() if len(bits) > 1 else ""}

def visible(locator):
    try:
        return locator.count() > 0 and locator.first.is_visible()
    except Exception:
        return False

def click_first(candidates, timeout=1600):
    for loc in candidates:
        try:
            if loc.count() and loc.first.is_visible():
                loc.first.click(timeout=timeout)
                return True
        except Exception:
            continue
    return False

def find_song_row(page, title, artist):
    # Prefer a result card containing the requested title and (when given) artist.
    cards = page.locator("div[data-type='song'], article, li")
    try:
        count = min(cards.count(), 80)
    except Exception:
        count = 0
    target = norm(title)
    artist_n = norm(artist)
    for i in range(count):
        card = cards.nth(i)
        try:
            if not card.is_visible():
                continue
            text = norm(card.inner_text(timeout=500))
            if target and target in text and (not artist_n or artist_n in text):
                return card
        except Exception:
            pass
    # Fallback to any visible element whose text contains the title and then climb a few parents.
    loc = page.get_by_text(title, exact=False)
    try:
        for i in range(min(loc.count(), 15)):
            item = loc.nth(i)
            if item.is_visible():
                cur = item
                for _ in range(5):
                    try:
                        txt = norm(cur.inner_text(timeout=300))
                        if target in txt and (not artist_n or artist_n in txt) and len(txt) < 1800:
                            return cur
                        cur = cur.locator("xpath=..")
                    except Exception:
                        break
    except Exception:
        pass
    return None

def click_more_options(row):
    if row is None:
        return False
    # Use accessible labels first, then common title/aria labels.
    options = [
        row.get_by_role("button", name=re.compile(r"more|options|menu|three dot|actions", re.I)),
        row.locator("button[aria-label*='More' i]"),
        row.locator("button[title*='More' i]"),
        row.locator("button[aria-label*='options' i]"),
        row.locator("button"),
    ]
    return click_first(options)

def choose_playlist_action(page, playlist, create_if_missing=True):
    # The site may expose "Add to Playlist" in a menu or dialog.
    try:
        page.get_by_text(re.compile(r"add to playlist", re.I)).first.click(timeout=2200)
    except Exception:
        return False, "Could not identify 'Add to playlist' menu item"
    page.wait_for_timeout(500)
    # Select existing playlist by exact visible name.
    try:
        named = page.get_by_text(playlist, exact=True)
        if named.count() and named.first.is_visible():
            named.first.click(timeout=1800)
            page.wait_for_timeout(700)
            return True, "Selected existing playlist"
    except Exception:
        pass
    # Try create playlist button / option.
    if create_if_missing:
        for label in [re.compile(r"create playlist|new playlist", re.I)]:
            if click_first([page.get_by_text(label), page.get_by_role("button", name=label)]):
                page.wait_for_timeout(400)
                fields = [page.get_by_role("textbox"), page.locator("input")]
                filled = False
                for field in fields:
                    try:
                        if field.count() and field.first.is_visible():
                            field.first.fill(playlist, timeout=1000)
                            filled = True
                            break
                    except Exception:
                        pass
                if not filled:
                    return False, "Create playlist opened but name field was not found"
                if not click_first([page.get_by_role("button", name=re.compile(r"create|save|done", re.I))]):
                    return False, "Playlist name entered but create/save button was not found"
                page.wait_for_timeout(900)
                return True, "Created playlist and attempted to add track"
    return False, "Playlist chooser opened, but target playlist was not found; handle manually"

def main():
    ap = argparse.ArgumentParser(description="Build a JioSaavn playlist from a text list using visible browser UI.")
    ap.add_argument("--file", required=True, help="Text file, one song per line: Title - Artist")
    ap.add_argument("--playlist", default="PRANIT VIBES", help="Target playlist name")
    ap.add_argument("--profile", default=".jiosaavn-profile", help="Local persistent browser profile directory")
    ap.add_argument("--delay", type=float, default=2.0, help="Seconds between songs (minimum 1)")
    ap.add_argument("--limit", type=int, default=0, help="Optional maximum number of songs for a test run")
    args = ap.parse_args()
    file = Path(args.file)
    if not file.exists():
        sys.exit(f"Song list not found: {file}")
    songs = [s for s in (parse_song(x) for x in file.read_text(encoding="utf-8-sig").splitlines()) if s]
    if args.limit:
        songs = songs[:args.limit]
    if not songs:
        sys.exit("No song titles found.")
    delay = max(1.0, args.delay)
    results = []
    profile = Path(args.profile)
    print(f"Loaded {len(songs)} titles. Playlist target: {args.playlist!r}")
    print("A visible browser opens. Sign in manually; never paste passwords or OTPs into this script.")
    with sync_playwright() as p:
        context = p.chromium.launch_persistent_context(str(profile), headless=False, viewport={"width": 1400, "height": 900})
        page = context.pages[0] if context.pages else context.new_page()
        page.goto(BASE, wait_until="domcontentloaded")
        input("Log in to JioSaavn in the opened browser if needed, then press ENTER here to begin...")
        for idx, song in enumerate(songs, 1):
            query = (song["title"] + " " + song["artist"]).strip()
            print(f"[{idx}/{len(songs)}] Searching: {query}")
            status, detail, url = "manual_review", "", ""
            try:
                page.goto(BASE + "/search/" + quote(query, safe=""), wait_until="domcontentloaded", timeout=30000)
                page.wait_for_timeout(1600)
                if re.search(r"captcha|verify you are human|access denied", page.title(), re.I):
                    detail = "Verification/access screen detected; stopped for manual handling."
                    results.append({**song, "status":"stopped", "detail":detail, "url":page.url})
                    print(detail)
                    break
                row = find_song_row(page, song["title"], song["artist"])
                if row is None:
                    status, detail = "not_found", "Could not confidently identify a result card."
                elif click_more_options(row):
                    page.wait_for_timeout(350)
                    ok, detail = choose_playlist_action(page, args.playlist)
                    status = "attempted" if ok else "manual_review"
                else:
                    detail = "Could not identify the result's options button."
                url = page.url
            except PlaywrightTimeoutError:
                detail = "Page timed out; review this title manually."
                url = page.url
            except Exception as e:
                detail = f"Automation error: {type(e).__name__}: {e}"
                url = page.url
            results.append({**song, "status":status, "detail":detail, "url":url})
            print(f"    {status}: {detail}")
            if status == "manual_review":
                print("    Inspect the browser. Fix the UI manually if needed, then press ENTER to continue or type q to stop.")
                if input("> ").strip().lower() == "q":
                    break
            time.sleep(delay)
        out = Path("results.csv")
        with out.open("w", newline="", encoding="utf-8-sig") as f:
            writer = csv.DictWriter(f, fieldnames=["raw","title","artist","status","detail","url"])
            writer.writeheader()
            writer.writerows(results)
        print(f"\nSaved {len(results)} results to {out.resolve()}")
        print("IMPORTANT: 'attempted' means the UI action was attempted, not independently verified. Check the playlist in JioSaavn.")
        input("Press ENTER to close the browser...")
        context.close()

if __name__ == "__main__":
    main()
