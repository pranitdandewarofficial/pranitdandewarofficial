# PRANIT VIBES · JioSaavn Playlist Assistant

A local, user-operated browser automation helper for building a JioSaavn playlist from a plain-text song list. It uses a visible Chromium browser and the normal JioSaavn website UI; you sign in yourself. It does not ask for or store your password, OTP, cookies, or session tokens.

## Important limitations
- JioSaavn does not publish a public, supported API for writing songs to a user's playlist. This helper therefore uses the website UI, which can change and may require manual confirmation.
- This is a best-effort automation, not a guarantee that every song can be added. Run a small test list first and inspect the playlist.
- Do not use it to bypass login, CAPTCHA, subscription checks, rate limits, or other access controls. If the site presents a verification screen, stop and handle it manually.
- Premium is not required just to create/manage playlists; Pro benefits relate to listening/download features. See Jio's help page.
- Desktop Chromium is supported (Linux, Windows, macOS). This is not a mobile-only Android app.

## Install (Ubuntu / Windows / macOS)
1. Install Python 3.10+.
2. In this folder run:
   ```bash
   python -m pip install -r requirements.txt
   python -m playwright install chromium
   ```
3. Start it:
   ```bash
   python jiosaavn_playlist.py --file songs.txt --playlist "PRANIT VIBES"
   ```
4. A visible browser opens. Sign in to JioSaavn manually if needed. Keep the browser window open.
5. The helper opens each song's search results and attempts to use visible playlist controls. It pauses between songs and records results in `results.csv`. If a UI step cannot be identified, it pauses instead of pretending the song was added.

## Song list format
One song per line. Artist is optional; recommended format is `Song Title - Artist`.
Lines starting with `#` are ignored. Example:
```text
Jalaluddin - Gravity
3:59 AM - DIVINE
Husn - Anuv Jain
```

## Safety and reliability
- Start with 2–3 songs and verify the playlist on JioSaavn.
- Don't run multiple copies at once.
- A failed/ambiguous match is marked for manual review.
- Browser profile data is saved locally in `.jiosaavn-profile/` so login persists on your own machine. Do not share that folder; it can contain a logged-in session. Delete it from your own device to sign out.
