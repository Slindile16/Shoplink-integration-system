# 5–10 minute demonstration

The project includes a repeatable live demo script and a generator for a narrated
walkthrough using captured API responses. The generator labels the video as a
generated walkthrough; it does not record unrelated desktop activity.

The completed local artifact is `demo/generated/ShopLink-demo.mp4` (8:32, 720p,
H.264 video with AAC narration). `ShopLink-demo.srt` contains companion subtitles.
The existing video is a historical artifact and includes the former test summary.
New walkthroughs use captured approved-order responses without test reports.

## Generate the narrated walkthrough on Windows

After `mvn clean package` succeeds and the Docker stack is healthy:

```powershell
python -m pip install --target .demo-tools -r scripts/demo-video-requirements.txt
./scripts/demo.ps1 -EvidencePath demo/generated/evidence.json
python scripts/render-demo.py prepare
./scripts/narrate-demo.ps1 -ScenesPath demo/generated/scenes.json
python scripts/render-demo.py render
```

The result is `demo/generated/ShopLink-demo.mp4`, with a companion subtitle file.
Generated media and local Python tools are ignored by Git. The renderer checks
that the video is between five and ten minutes and uses Windows system fonts and
local Windows speech synthesis. Python and video dependencies are optional; they
are not needed to build or run the Java system.

## Record a personal presentation

Before recording, start a clean Docker stack using the [runbook](operations.md),
open this repository in an editor, and open the broker console. Avoid exposing
personal windows, tokens, or unrelated terminals in the recording.

| Time | Show | Explain |
|---|---|---|
| 0:00–1:00 | README and architecture diagram | Six independent services, synchronous validation/reservation, asynchronous payment/delivery/notifications |
| 1:00–2:00 | `docker compose ps` and health endpoints | One-command startup, ports, service ownership |
| 2:00–3:30 | `./scripts/demo.ps1` | Calculated prices, PAYMENT_PENDING then PAID, stock deduction, one delivery and two notices |
| 3:30–5:00 | Stop payment, submit an order, start payment | Queue buffering and eventual processing |
| 5:00–6:00 | Clean decline scenario from runbook | Failed payment restores stock and creates no delivery |
| 6:00–7:30 | Reservation code and stock responses | Atomic baskets, independent ring sizes, duplicate reservations and releases |
| 7:30–9:00 | Retry buffer and operations notes | Publication recovery, bounded redelivery, DLQ, honest in-memory limitations |

Run `mvn clean package` before recording and demonstrate the live order flow.

For a personal presentation, record the screen and your narration using your
preferred recorder, export an MP4, and link it from the README.
