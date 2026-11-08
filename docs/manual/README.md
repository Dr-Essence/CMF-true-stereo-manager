# CMF Stereo manual (early beta)

The PDF instruction manual. It is in early beta: still being written, and some screenshots are
still placeholders. The **Attach manual** workflow (run by hand) uploads it to the latest
release.

- `CMF-Stereo-Manual.pdf`: the manual.
- `app_content.py`: its text, chapter by chapter (HTML sections).
- `build_manual.py`: builds the PDF with Playwright's Chromium, in two passes so the contents
  page shows each section's page.
- `shots/`: the screenshots; `shots/map.json` matches each to its place and caption. A place
  without one prints a dashed box naming the screenshot it needs.

Build:

    NDOT_FONT=/path/to/Ndot77JPExtended.ttf python3 build_manual.py app_content.py ../banner.png CMF-Stereo-Manual.pdf shots

Needs Playwright (Node) and poppler-utils (pdfinfo, pdftotext). Ndot 77 JP Extended is
Nothing's font and is not stored here; take it from the phone.
