# Shringaar Stitches — website

Spring Boot 3 (Java 21) catalogue + e-commerce site with WhatsApp enquiries, built on the
**Mehrab** UI theme (maroon and zari gold on ivory, arch-framed photos, running-stitch dividers).

## Run it

```bash
mvn spring-boot:run
```

Open http://localhost:8080. Staff admin: http://localhost:8080/admin (default `admin` / `change-me`).

Local data is stored in `./data/` (H2 database + uploaded photos). An empty database is seeded with
8 sample products — **the prices are placeholders**; edit them in the admin.

## Before going live — set these

| Env variable | What |
|---|---|
| `STORE_WHATSAPP_NUMBER` | Business WhatsApp number, digits only with country code, e.g. `919876543210` |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | Admin login (**change the password**) |
| `STORE_PHONE`, `STORE_EMAIL`, `STORE_ADDRESS`, `STORE_CITY`, `STORE_INSTAGRAM_URL` | Shown in the footer and booking page |
| `SPRING_PROFILES_ACTIVE=prod` + `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` | PostgreSQL in production |

Hero photo: drop a portrait image at `src/main/resources/static/images/hero.jpg`.

## What's included

**Storefront**
- Home, collections (category filter + search), product page with size/quantity
- Bag (session cart), checkout (Cash on delivery / Pay at studio / UPI link on WhatsApp), order confirmation
- Book an appointment: garment, day, time slot (taken slots disabled, no double booking), and how to meet:
  **studio fitting**, **video call** (on WhatsApp) or **phone call** consultation

**WhatsApp integration** — no WhatsApp Business API account needed. Every WhatsApp button goes to
`/enquire/...`, which logs the enquiry and redirects to `https://wa.me/<number>?text=<pre-filled message>`:

| Where | Pre-filled message |
|---|---|
| Floating button (every page) | General greeting; on a product page it becomes that product's enquiry |
| Product → *Enquire on WhatsApp* | Product name, design code, price, chosen size & quantity, link |
| Bag → *Order on WhatsApp* | Every item with size, qty, line totals and the total |
| Order confirmation | Order reference, items, total, payment method |
| Appointment confirmation | Garment, date/time, appointment type |

**Admin** (`/admin`): products (add/edit, photo upload, hide/publish, featured), categories, orders
(status, one-click WhatsApp to the customer), appointments (one-click reminder, open chat to
video call), and a log of every WhatsApp enquiry.

## Automatic WhatsApp reminders

When a customer books, she can tick *"Send my confirmation and appointment reminders on WhatsApp"*
(ticked by default). She then gets, from your business number:

1. a **confirmation** as soon as she books,
2. a **reminder 24 hours before** (skipped if she booked less than a day ahead),
3. a **reminder 1 hour before**.

Sending messages without anyone pressing Send needs the **WhatsApp Business Cloud API** from Meta.
Until it is set up, nothing is sent automatically. Instead, **Admin → Appointments** starts with a
**Reminders due** list: a day-before reminder for each of tomorrow's appointments and a 1-hour
reminder for each of today's (highlighted *Send now* within 90 minutes of the start). **Send
reminder** opens WhatsApp with the message typed out and marks it *Sent ✓* (with Undo). The
dashboard shows how many are still to send. The same marks are used by the automatic sender, so
switching the API on later never repeats a reminder already sent by hand.

### Setting it up

1. Create a Meta Business account and a WhatsApp Business app at https://developers.facebook.com,
   and add your business phone number. Note the **Phone number ID** and create a **permanent
   access token** (System User token with `whatsapp_business_messaging`).
   A number registered on the Cloud API can't be used in the normal WhatsApp app at the same time,
   so many businesses use a separate number for it.
2. In WhatsApp Manager, create two **Utility** templates in English (`en`) with four body variables.
   Suggested text:

   `appointment_confirmation`
   > Hi {{1}}, your {{2}} with Shringaar Stitches is booked for {{3}}. {{4}} Reply here if you need to change the time.

   `appointment_reminder`
   > Hi {{1}}, a reminder of your {{2}} with Shringaar Stitches on {{3}}. {{4}} Reply here if you need to change the time.

   The site fills them as: {{1}} name · {{2}} e.g. "video call consultation for your bridal lehenga"
   · {{3}} e.g. "Fri 13 Nov 2026 at 3:00 pm" · {{4}} how to join (e.g. "We will video call you on
   WhatsApp at +91 …" or the studio address).
3. Once Meta approves the templates, set:

   | Env variable | Value |
   |---|---|
   | `WHATSAPP_API_ENABLED` | `true` |
   | `WHATSAPP_API_TOKEN` | the access token |
   | `WHATSAPP_PHONE_NUMBER_ID` | the phone number ID |
   | `WHATSAPP_CONFIRMATION_TEMPLATE` / `WHATSAPP_REMINDER_TEMPLATE` | only if you named the templates differently |
   | `STORE_TIMEZONE` | defaults to `Asia/Kolkata` |

The site checks for due reminders every 5 minutes. The result of each message (sent, or the
error from WhatsApp) is shown in the WhatsApp column on `/admin/appointments`. Meta charges per
business-initiated conversation; see Meta's WhatsApp pricing page.

**JSON API** (read-only): `/api/categories`, `/api/products?category=&q=`, `/api/products/{slug}`.

## Code map

```
src/main/java/com/shringaar/store/
  config/      StoreProperties, security, sample data
  domain/      Product, Category, CustomerOrder, OrderItem, Appointment, Enquiry
  service/     Cart, CatalogService, OrderService, BookingService, WhatsAppService, Formats (₹1,20,000),
               AppointmentNotifier + CloudApiWhatsAppMessenger (automatic reminders)
  web/         storefront controllers, EnquiryController (WhatsApp redirects)
  web/admin/   admin
  api/         JSON API
src/main/resources/templates/   Thymeleaf pages; fragments/layout.html holds header, footer, WhatsApp button
src/main/resources/static/css/site.css   Mehrab theme tokens and components
```

## Next steps worth considering
- Online payments: add Razorpay/Cashfree as another `PaymentMethod` (needs merchant keys)
- Order notifications to the customer through the same WhatsApp Business API
- Move photo uploads to S3/Cloudinary when hosting on multiple servers
- Flyway migrations instead of `ddl-auto=update` once the schema settles
