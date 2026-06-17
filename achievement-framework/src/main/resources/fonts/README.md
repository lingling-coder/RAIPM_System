# Dashboard PDF Export Fonts

This directory should contain the `NotoSansSC-Regular.ttf` font file for PDF export with Chinese character support.

## Font Installation

1. Download `NotoSansSC-Regular.ttf` from Google Fonts:
   - https://fonts.google.com/noto/fonts/Noto+Sans+SC
   - Or from the Noto repository: https://github.com/notofonts/noto-cjk/releases

2. Place the `.ttf` file in this directory.

3. The `DashboardPdfService` will detect the font file automatically. If the font is not found, it falls back to the built-in `STSong-Light` font via the `font-asian` iText 7 add-on.

## Fallback

Without the `.ttf` font file, PDF export will use:
- **Primary fallback:** `STSong-Light` (via iText 7 font-asian library)
- This font includes CJK character support but may have limited glyph coverage compared to Noto Sans SC.

> The font-asian dependency is declared in `achievement-framework/pom.xml` and is bundled with the application.
> No additional download is required for the fallback to work.
