# Completed verification

Historical verification recorded on 30 October 2026. The automated tests and
integration-test module described below were subsequently removed at the project
owner’s request. These results are historical and are not current run instructions.

## Java build

`mvn verify` completed successfully across all nine Maven modules. All 44 tests
passed with zero failures or errors, including nine real HTTP/JMS integration
tests. Host tests ran on Java 25 while compiling to Java 17. The Docker build
also compiled and packaged all six applications on Java 17.

Coverage includes atomic stock reservations, competing orders, duplicate
reservations/releases, overflow rejection, trusted product totals, approval and
decline flows, delayed consumers, payment mismatch rejection, independent event
publication retries, stock-release recovery, duplicate delivery/notification
events, broker outage recovery, and poison-message dead-letter quarantine.

## Containers and live APIs

`docker compose build` produced all six service images. The complete Compose
stack started with healthy broker and service checks. The live demo verified:

* Approved order: `PAYMENT_PENDING` to `PAID`, three units reserved, one delivery,
  and two customer notification records. Stock changed from 47 to 44.
* Declined order: `PAYMENT_FAILED`, reservation restored, and one failure notice.
  Stock remained 44 after completion.

Payment Service was returned to its default approval mode after the decline
scenario. The demo uses IPv4 loopback explicitly, and its HTTP client allows
extra time for the first request on a cold local JVM. Application dependency
connection/read timeouts remain bounded at two/three seconds.

The GitHub Actions workflow now checks the Java build, storefront syntax and Compose configuration on pushes and pull requests. No remote workflow run or repository push was
performed as part of this local verification.

## Demonstration artifact

The generated narrated walkthrough is 8 minutes 32 seconds, with 1280×720 H.264
video and AAC audio. A decoded frame was inspected for legibility; audio signal
checks confirmed narration is present without clipping. The video displays actual
captured API evidence and the passing test summary. It is clearly labeled as a
generated walkthrough rather than a desktop screen recording.

Generated media, local Python rendering tools, Maven output, and verification
logs are ignored by Git. Regenerate the video using the [demo guide](demo.md).
The application remains an in-memory portfolio system; operational limitations
and reset procedures are documented in the [runbook](operations.md).
