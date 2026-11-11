"""Render a narrated demonstration from captured live API evidence.

Windows: python -m pip install -r scripts/demo-video-requirements.txt
  ./scripts/demo.ps1 -EvidencePath demo/generated/evidence.json
  python scripts/render-demo.py prepare
  ./scripts/narrate-demo.ps1 -ScenesPath demo/generated/scenes.json
  python scripts/render-demo.py render

The output is a generated narrated walkthrough, not a recording of desktop activity.
"""
import argparse
import json
import subprocess
import sys
import textwrap
import wave
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / '.demo-tools'))
OUT = ROOT / 'demo/generated'


def prepare():
    from PIL import Image, ImageDraw, ImageFont

    evidence = json.loads((OUT / 'evidence.json').read_text(encoding='utf-8-sig'))
    if evidence['completed']['status'] != 'PAID' or len(evidence['notifications']) != 2:
        raise SystemExit('An approved, complete demo evidence file is required.')
    oid = evidence['created']['id']
    before = evidence['stockBefore']['availableQuantity']
    after = evidence['stockAfter']['availableQuantity']
    code = lambda value: json.dumps(value, indent=2, ensure_ascii=False)
    scenes = [
        ('ShopLink', 'A complete order integration walkthrough',
         'Six independent Java services\nREST + JSON + ActiveMQ/JMS\nVerified live API results\nGenerated narration and captured responses',
         'Welcome to ShopLink, an online store integration system built as a systems integration portfolio project. This walkthrough follows a real order through the running services, using captured API responses rather than invented sample results. We will look at product validation, stock reservation, asynchronous payment, delivery, and notifications. We will also explain concurrency, retry behavior, the jewellery storefront, and the limits of the in memory design.'),
        ('01 / Service boundaries', 'Each service owns a clear responsibility',
         'Product  :8080     Catalog and trusted prices\nOrder    :8081     Customer orders and workflow\nInventory :8082    Atomic stock reservations\nPayment  :8083     Simulated payment processing\nDelivery :8084     One delivery per paid order\nNotification :8085 Payment and delivery notices',
         'ShopLink has six services. Product owns the sample catalog and trusted prices. Order accepts customer requests and tracks workflow state. Inventory owns available stock and reservations. Payment simulates approval or decline. Delivery creates a delivery record for a paid order. Notification records payment and delivery notices. These boundaries allow each service to communicate through a small API or event contract instead of directly accessing another service\u2019s data structures.'),
        ('02 / Start the stack', 'One command starts the complete local system',
         'docker compose up --build -d --wait\n\ndocker compose ps\n\n./scripts/demo.ps1\n\nHealth: /actuator/health on ports 8080-8085',
         'Docker Compose builds and starts all six applications and the Active MQ broker. The applications have health checks, and Order waits for Product and Inventory to be ready. Messaging services wait for the broker to accept connections. Each published port binds to the local machine. The demo script submits an order, waits for the asynchronous results, and verifies that the stock change matches the expected payment outcome.'),
        ('03 / Submit an order', 'POST http://localhost:8081/orders', code(evidence['request']),
         'The customer supplies a customer identifier and a list of product identifiers with positive quantities. In this request, the customer orders three canvas tote bags. There is no client supplied price. Order asks Product Service for the current product details and calculates the amount from those trusted prices. It also checks currency consistency and combines repeated product lines without allowing integer overflow before requesting an inventory reservation.'),
        ('04 / Accept and queue', 'The returned ID is used to poll progress',
         code({'id': oid, 'initialStatus': evidence['created']['status'], 'capturedAt': evidence['capturedAt']}),
         'Order returns a created response with a generated identifier and payment pending status. Inventory has already reserved the requested units. Order places the order created event in a process local publication buffer and attempts to send it to Active MQ. A failed publication stays pending and is retried automatically. The HTTP response does not wait for payment, delivery, and notifications to complete. The customer can use this order identifier to query progress.'),
        ('05 / Payment completes', 'GET /orders/{id}',
         code({'id': oid, 'status': evidence['completed']['status'], 'items': evidence['completed']['items']}),
         'Payment consumes the order created event and creates one simulated payment for the order. It replies on the same transacted JMS session, so message consumption and the payment reply commit together. Order checks that the returned amount and currency match its saved order before changing the state to paid. A repeated or conflicting terminal payment result cannot change a completed order. This is the actual saved response from the demonstration run.'),
        ('06 / Stock stays consistent', 'Inventory is the single owner of stock',
         f'Product: P1002\n\nAvailable before: {before}\nReserved quantity: 3\nAvailable after: {after}\n\nEvery basket is checked and deducted atomically.',
         'Inventory protects its stock map, reservation map, and released identifiers with one monitor. A reservation checks every product before changing any quantities. Competing orders cannot both claim the same remaining units, and a rejected basket does not partially reduce stock. In this captured run, three units were deducted. Repeating the same reservation does not deduct twice, and repeating a release cannot restore stock twice. These guarantees apply within one inventory process.'),
        ('07 / Delivery is created', 'GET /deliveries/orders/{id}', code(evidence['delivery']),
         'For an approved payment, Order queues an order paid event. Delivery consumes that event and creates one record for the order. Its concurrent map uses an atomic operation so repeated requests reuse the same delivery identifier. Delivery sends a delivery created reply in the listener transaction. This response is the real delivery record returned for the demonstration order. A declined payment does not request a delivery.'),
        ('08 / Customer updates', 'GET /notifications/{id}',
         code([{'type': item['type'], 'message': item['message']} for item in evidence['notifications']]),
         'Notification receives the order status update and the delivery created event on separate queues. It records one notice per order and notice type. Here we can see the payment success and delivery creation notices for the same order. The two queues are independent, so notification arrival order is not guaranteed. These are simulated customer updates stored in memory. No real email or SMS provider is configured or contacted.'),
        ('09 / Recover from failures', 'Retries preserve progress while processes stay running',
         'Publication failure -> process-local retry buffer\nDecline -> STOCK_RELEASE_PENDING -> PAYMENT_FAILED\nListener failure -> three bounded redeliveries\nPermanent bad message -> ActiveMQ.DLQ\n\nDuplicate events do not repeat business effects.',
         'The failure paths are part of the implementation. Failed event sends retry every two seconds. A declined payment first marks stock release pending, and only a successful release allows payment failed. An independent scheduler can finish the release after message retries are exhausted. Listener failures roll back consumption, with three redeliveries and exponential delay. Persistent poison messages then move to the dead letter queue so they cannot block the queue forever.'),
        ('10 / Jewellery storefront', 'The Heirloom Verse — http://localhost:3000',
         '40 designs across four categories\nWomen, men and unisex collections\nTwo-ring and three-ring sets\nIndividual ring sizes and shared stock\nSimulated checkout and order tracking',
         'The Heirloom Verse gives these integrations a customer-facing jewellery store. The collection includes forty designs across rings, necklaces, earrings, and bracelets, with collections for women, men, and unisex styles. Matching sets contain two or three rings, each with an independently selected size. Checkout expands the complete set into its individual stock items and reserves them together in one atomic inventory operation. The website then follows the simulated payment and delivery status.'),
        ('11 / Scope and next steps', 'Portfolio implementation, with explicit reliability limits',
         'In-memory orders, stock, payments, and retry buffers\nBroker messages persist in a Docker volume\nRun one instance per service\n\nProduction extensions:\nDatabase transactions + durable outbox\nAPI idempotency + authentication + real providers',
         'This completes the ShopLink demonstration. The project deliberately uses in memory application state. A restart loses orders, stock reservations, duplicate protection, and pending publications, even though the broker can retain messages. A timeout during an inventory request can also have an ambiguous outcome. Production use would require persistent state, a transactional outbox, request idempotency, authentication, and real payment and notification providers. The repository includes Docker startup, continuous integration, an operations runbook, and reproducible demo tools.'),
    ]
    fonts = Path('C:/Windows/Fonts')
    title_font = ImageFont.truetype(str(fonts / 'segoeuib.ttf'), 56)
    subtitle_font = ImageFont.truetype(str(fonts / 'segoeui.ttf'), 28)
    body_font = ImageFont.truetype(str(fonts / 'consola.ttf'), 25)
    footer_font = ImageFont.truetype(str(fonts / 'segoeui.ttf'), 20)
    output = []
    for index, (title, subtitle, body, narration) in enumerate(scenes):
        image = Image.new('RGB', (1600, 900), '#101b2b')
        draw = ImageDraw.Draw(image)
        draw.rectangle((0, 0, 1600, 12), fill='#32d4b0')
        draw.text((80, 62), title, font=title_font, fill='#f3f7fc')
        draw.text((82, 144), subtitle, font=subtitle_font, fill='#a8bdd7')
        draw.rounded_rectangle((70, 214, 1530, 806), radius=22, fill='#192c43')
        lines = []
        for line in body.splitlines():
            lines.extend(textwrap.wrap(line, width=90, replace_whitespace=False, drop_whitespace=False) or [''])
        # Keep all response fields readable; compact long responses instead of clipping.
        size = min(25, int(535 / max(len(lines), 1) / 1.25))
        font = ImageFont.truetype(str(fonts / 'consola.ttf'), size)
        draw.multiline_text((104, 240), '\n'.join(lines), font=font, fill='#e0edfa', spacing=7)
        draw.text((80, 843), 'SHOPLINK  /  Verified API evidence + generated narration', font=footer_font, fill='#8aa2be')
        draw.text((1430, 843), f'{index + 1:02d} / {len(scenes):02d}', font=footer_font, fill='#32d4b0')
        scene_id = f'{index:02d}'
        image.save(OUT / f'{scene_id}.png')
        output.append({'id': scene_id, 'title': title, 'narration': narration})
    (OUT / 'scenes.json').write_text(json.dumps(output, indent=2, ensure_ascii=False), encoding='utf-8')
    print(f'Prepared {len(output)} scenes from captured live order {oid}.')


def render():
    import imageio_ffmpeg

    ffmpeg = imageio_ffmpeg.get_ffmpeg_exe()
    scenes = json.loads((OUT / 'scenes.json').read_text(encoding='utf-8'))
    total = 0
    subtitles = []
    def stamp(seconds):
        millis = round(seconds * 1000)
        return f'{millis // 3600000:02}:{millis // 60000 % 60:02}:{millis // 1000 % 60:02},{millis % 1000:03}'
    for index, scene in enumerate(scenes):
        scene_id = scene['id']
        with wave.open(str(OUT / f'{scene_id}.wav')) as audio:
            duration = max(25, audio.getnframes() / audio.getframerate() + 2)
        words = scene['narration'].split()
        chunks = [' '.join(words[start:start + 17]) for start in range(0, len(words), 17)]
        for part, chunk in enumerate(chunks):
            start = total + (duration - 2) * part / len(chunks)
            end = total + (duration - 2) * (part + 1) / len(chunks)
            subtitles.append(f'{len(subtitles) + 1}\n{stamp(start)} --> {stamp(end)}\n{chunk}\n')
        subprocess.run([ffmpeg, '-y', '-loglevel', 'error', '-loop', '1', '-framerate', '10',
                        '-i', str(OUT / f'{scene_id}.png'), '-i', str(OUT / f'{scene_id}.wav'),
                        '-vf', 'scale=1280:720', '-c:v', 'libx264', '-preset', 'ultrafast', '-crf', '23',
                        '-tune', 'stillimage', '-pix_fmt', 'yuv420p', '-c:a', 'aac', '-b:a', '96k',
                        '-af', 'apad', '-t', str(duration), '-threads', '2',
                        str(OUT / f'{scene_id}.mp4')], check=True)
        total += duration
        print(f'Rendered {index + 1}/{len(scenes)}: {scene["title"]}', flush=True)
    if not 300 <= total <= 600:
        raise SystemExit(f'Duration {total:.1f}s is outside the 5-10 minute requirement.')
    (OUT / 'concat.txt').write_text(''.join(f"file '{scene['id']}.mp4'\n" for scene in scenes), encoding='utf-8')
    (OUT / 'ShopLink-demo.srt').write_text('\n'.join(subtitles), encoding='utf-8')
    subprocess.run([ffmpeg, '-y', '-loglevel', 'error', '-f', 'concat', '-safe', '0', '-i', str(OUT / 'concat.txt'),
                    '-c', 'copy', '-movflags', '+faststart', str(OUT / 'ShopLink-demo.mp4')], check=True)
    (OUT / 'video-metadata.json').write_text(json.dumps({'durationSeconds': total, 'scenes': len(scenes),
                    'kind': 'Generated narrated walkthrough using captured live API evidence'}, indent=2), encoding='utf-8')
    print(f'Created {OUT / "ShopLink-demo.mp4"} ({total / 60:.2f} minutes).')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('command', choices=['prepare', 'render'])
    args = parser.parse_args()
    prepare() if args.command == 'prepare' else render()
