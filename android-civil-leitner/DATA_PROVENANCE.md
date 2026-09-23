# Civil Code data provenance

The Android seed contains exactly the 1,335 main-numbered provisions of the Iranian Civil Code (`1..1335`).

## Current consolidated official source

- National Laws and Regulations System (Presidential Legal Affairs): `https://qavanin.ir/Law/TreeText/?IDS=12021850837713548188`
- Retrieved through the official printable/tree page on 2026-09-18 (Tehran date).
- Each article is extracted from its own official `SecTex` block; notes immediately attached to the provision are included in the same record.

## Official Gazette provenance

- Original Civil Code: Official Gazette registration 1877, print 2, period 9, volume 1, page 210.
- Direct live retrieval from `rrk.ir` was attempted from both the web retrieval environment and GitHub Actions but the site did not respond. The build therefore does **not** claim a live RRK page fetch. The consolidated text is marked `VERIFIED_OFFICIAL`, not `VERIFIED_RRK`.

## Validation gates

- Exactly 1,335 main records.
- Exact sequential numbering from 1 through 1,335, without gaps or duplicate main numbers.
- No blank official text.
- Spot checks for current texts of articles 1, 2, 190, 946, 1041 and 1335.
- Repealed provisions remain in the numbered dataset and are visibly marked as repealed.
- Four `مکرر` provisions are preserved separately in `civil_supplemental.json`: 218مکرر, 881مکرر, 1313مکرر, 1328مکرر.
- SHA-256 hashes of the retrieved source and generated seed are recorded in `civil_source_manifest.json`.
