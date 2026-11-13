# The Heirloom Verse

A responsive luxury jewellery storefront for the ShopLink integration portfolio.
The approved palette is deep burgundy, warm pearl, dusty rose and champagne gold.

## Run

From the repository root:

```powershell
docker compose up --build -d --wait --wait-timeout 300
```

Open **http://localhost:3000**. The original six Java services remain available on
ports 8080–8085. The gateway uses Node 24 in Docker and requires Node 22.13+ when
run directly. It has no npm dependencies:

```powershell
cd storefront
npm start
```

Direct execution supports browsing without Java services. Integrated checkout
requires Product, Inventory, Order, Payment and Delivery to be healthy. Configure
service addresses using `PRODUCT_SERVICE_URL`, `INVENTORY_SERVICE_URL`,
`ORDER_SERVICE_URL`, `PAYMENT_SERVICE_URL` and `DELIVERY_SERVICE_URL`.

## First release

- Responsive homepage, four categories and 117 fictional designs.
- Women, Men and Unisex collections, plus material, gemstone, style, size and price filters.
- Search, category filters and price/name sorting.
- Product details, size/length options and live stock checks.
- Browser-persisted shopping bag, quantity edits and removal.
- Validated simulated checkout with backend pricing and variant stock.
- Order tracking with payment, stock-release and delivery states.
- Story, care, size guide and demo delivery information.
- SQLite checkout journal with session ownership and request references.
- Original AI-generated imagery; see [image prompts](image-prompts.md).
- Eight ring styles: solitaire, halo, three-stone, eternity bands, signet, sculptural, nature-inspired and gemstone.
- Nineteen new individual ring designs, including six solitaire cuts across the collection.
- [Ring imagery and prompts](image-prompts-rings.md).
- A dedicated Single necklaces view, with six supplied butterfly, heart, sapphire, moon/star and infinity designs shown first.
- [Supplied necklace image sources](user-necklace-images.md).
- Ten necklace styles, with twenty new main designs and four individually available pieces from the two matching pairs.
- Ten additional matching necklace pairs: split hearts, lock/key, mountain/wave, black/white flowers, twin stars, butterfly wings, two-tone circles, leaf/branch, contrasting gemstones and connected shapes.
- The ten new designs are sold as complete pairs. Their twenty component records are inventory-only and never appear as product cards or standalone product pages. Each component has its own chain length and stock.
- [Matching necklace imagery and prompts](image-prompts-matching-necklaces.md).
- Gold sun-and-moon and silver black-and-white yin-yang pairs with independent chain lengths.
- Two- and three-layer necklaces share one clasp; the selected length is the inner layer.
- [Necklace imagery and prompts](image-prompts-necklaces.md).
- Four matching ring sets: bridal duo, couples’ duo, wedding trio and stacking trio.
- Six matching bracelet pairs across three distinct designs, each with gold or silver details.
- Designs include Shared Verse beads, Unity yin-yang cords and Fleur Chain flower stations.
- Each Fleur Chain bracelet has four flowers with chain connecting the gaps.
- Bracelet pairs suit couples, friends, siblings and any meaningful connection.
- Gold/silver finish links preserve independently chosen black/white bracelet lengths.
- A separate size for each ring, clear set contents and a total set price.
- Shared component stock: buying a set reserves its individual ring variants atomically.
- One distinct image per design; [expanded image prompts](image-prompts-expanded.md).

The same `dist/catalogue.json` seeds Product and Inventory via the common module.
There are 605 physical jewellery variants, initially 8 units each. Original `P1001`–`P1003`
products remain for the existing demos. `HV01-N` and `HV01-J` are independently
stocked ring sizes.

Ring sets have no independent or duplicated stock. For example, the Forever
Wedding Trio contains the Promise Solitaire, Contour Band and Legacy Band. Sizes
`N|P|V` expand to `HV01-N`, `HV15-P` and `HV14-V` in a single backend order. The
gateway combines overlapping single-ring and set quantities before reservation.
Set prices are the sum of their component prices. The checkout journal keeps
the selected set name and individual sizes for the order receipt.

The Shared Verse pairs use black onyx and white howlite beads with either 18ct
yellow-gold or sterling-silver spacer details. Gold pairs cost R12,400 and silver
pairs R7,800 in the fictional catalogue. Each bracelet has its own 15cm, 17cm,
19cm, 21cm or 23cm size and stock reference. Pairs reserve both bracelets together;
individual colours are also available separately. See [bracelet image notes](image-prompts-bracelets.md).

The Unity design uses a yin-yang medallion on black/white cord. The Fleur Chain
design uses four black/white enamel flowers per bracelet with fine metal chain
between stations. The product page has visual design choices alongside gold and
silver choices; changing either preserves both selected wrist sizes. See
[distinct design image notes](image-prompts-bracelet-designs.md).

## Demo checkout

Use fictional details: `Demo Customer`, `demo@example.com`, `12 Example Street`,
`Johannesburg`, `2000`, `Gauteng`. No card fields are present. The gateway forwards
only a generated customer ID, variant IDs and quantities to Order. Payment results
use the existing simulation. Tracking requires the checkout's browser session.

The SQLite journal at `data/checkout.sqlite` (Docker volume `storefront-data`)
stores fictional delivery details and request references. It is ignored by Git
and excluded from hosting. The session cookie lasts seven days; clearing it
prevents access to previous demo orders in that browser.

## Hosted preview

Private preview: https://heirloom-verse-portfolio.adorablethistle2.chatgpt.site
The matching-bracelet update was published successfully on 11 November 2026.

The private Sites version is a **static storefront preview**. Browsing, filters,
product options and the shopping bag work. Checkout is explicitly disabled because
a hosted page cannot reach Java services on your computer. Integrated checkout is
available in the local Docker deployment.

## Durability and retries

The checkout journal is durable. Java services still hold business state in
memory, so restarting those services resets orders and stock. The journal does
not make the underlying system durable. Accounts, administration, durable Java
repositories and hosted backend deployment are the next milestone.

Accepted checkout references recover existing orders after a gateway restart.
Timeouts or server failures during submission are marked unknown; the gateway
refuses to submit the same request again. This prevents blind duplicate orders
but cannot recover an unknown backend ID automatically because the current Order
API has no idempotency-key lookup. Inspect the local journal and Order logs before
starting a new checkout in that situation.

## Build checks

```powershell
npm run check
```

From the repository root, `mvn clean package` builds the Java services. Automated
test suites and smoke-check scripts have been removed from the project.
The local Docker deployment provides the integrated shopping experience; the
private hosted preview is static and cannot connect to the local Java services.

## Personal necklace additions

Four personalised 18ct yellow gold concepts: The Initial Verse (R9,800, A-Z), The Birthstone Story (R14,900, birth month), The Written Promise Bar (R12,800, message) and The Forever Disc (R11,900, initials/name/date). Engravings accept up to 24 characters. Each has five chain lengths and an illustrative example image. Personalisation is preserved in bag and checkout snapshots; stock is shared per design and chain length. See [images and generation prompts](image-prompts-personal-necklaces.md).

### Birthstone month previews

The month selector displays both month and gemstone. Gallery, gemstone details, bag images, checkout summaries and order snapshots use the selected month. The collection offers one accepted stone per month: January garnet, February amethyst, March aquamarine, April diamond, May emerald, June alexandrite, July ruby, August peridot, September sapphire, October pink tourmaline, November citrine and December tanzanite. June, October and December have other accepted birthstones; these are this collection’s choices. All options retain the fictional R14,900 catalogue price and share inventory by chain length.

Birthstone sources: [American Gem Society](https://www.americangemsociety.org/birthstones/) and [GIA](https://www.gia.edu/birthstones).

## Bracelet stacks

Five complete sets are available under Bracelets → Bracelet stacks: Golden Chapter (five bangles, R24,500), Pearl & Promise (three pieces, R20,600), Midnight (three pieces, R15,300), Radiance (three pieces, R40,800) and Mixed Verse (three bangles, R12,800). Photos show the full stack worn on one wrist. Every pictured bracelet is included; customers choose 16cm, 18cm or 20cm for each piece. Hidden component SKUs provide individual stock reservations and sum to the displayed set price. See [image prompts](image-prompts-bracelet-stacks.md).

## Earring additions

Seven designs include Golden Chapter Ear Stack (R11,600, three components for one ear), Celestial Verse Set (R9,300, three components for one ear), Liquid Petal Drops (R14,900 per pair), Ivy Light Climbers (R12,900 per pair), Birthstone Story Studs (R7,900 per pair), Initial Verse Huggies (R9,900 per pair) and Pearl Halo Jackets (R11,800 per complete pair). Each listing explains its included pieces and piercing requirements. Birth month, independent left/right letters and pearl-jacket styling selections have illustrative live previews and are retained in bag and checkout snapshots. Pair stock is shared across personalisation choices; ear stacks reserve every component. Jacket styling always includes both studs and both removable jackets. See [image prompts](image-prompts-earrings.md).

### Ear placement and medium hoops

Celestial Verse uses an anatomical ear-stand photo; Ivy Light Climbers use mirrored ear stands to show upward placement. Stands are display props and are not included. Initial Verse Huggies now have petite 12mm hoops with 5mm letter charms, including smaller letters in live previews. Woven Circle Hoops (R8,900) and Everyday Circle Hoops (R7,900) are matching pairs of round medium 25mm hoops, with twisted and smooth finishes respectively. See [refinement image prompts](image-prompts-earring-refinements.md).
