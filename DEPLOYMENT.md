# RYZORA Facebook AI Customer Service & Order Management System
## Production Deployment & Meta Integration Guide

**Brand:** RYZORA — *Smart Choice. Better Life.*  
**Website:** [https://ryzorabd.com](https://ryzorabd.com)  
**Facebook Page:** [https://www.facebook.com/ryzorabd](https://www.facebook.com/ryzorabd)  
**Location:** Bangladesh | **Currency:** BDT (৳) | **Languages:** Bangla, Banglish, English

---

### Architecture Overview

```
                      +------------------------------------------+
                      |          Meta Messenger Platform         |
                      |   (Customer sends message to ryzorabd)   |
                      +---------------------+--------------------+
                                            |
                                            | POST /webhook
                                            | (X-Hub-Signature-256)
                                            v
                      +---------------------+--------------------+
                      |     RYZORA Cloud Run / HTTPS Backend     |
                      |                                          |
                      |  1. Verify HMAC-SHA256 signature         |
                      |  2. Idempotency Check (Deduplication)    |
                      |  3. Gemini AI / Natural Language Engine  |
                      |  4. Product Catalog & Stock Verification |
                      |  5. Order Calculation & BD Phone Check   |
                      |  6. Graph API v21.0 Response Dispatch    |
                      +----------+--------------------+----------+
                                 |                    |
                                 v                    v
              +------------------+---+      +---------+----------------+
              |  Supabase PostgreSQL |      |   Android Admin Console  |
              |   (Orders, Products, |      |  (Real-time Dashboard,   |
              |   Messages, Logs)    |      |   Human Takeover, Ops)   |
              +----------------------+      +--------------------------+
```

---

### Step 1: Meta for Developers Setup

1. **Create Meta App:**
   - Go to [Meta for Developers](https://developers.facebook.com).
   - Click **My Apps** > **Create App**.
   - Select **Other** > **Business** app type.
   - Enter App Name: `RYZORA AI Messenger Agent` and your contact email.

2. **Add Messenger Product:**
   - Under Add Products to your app, locate **Messenger** and click **Set Up**.

3. **Connect Facebook Page (`ryzorabd`):**
   - In Messenger Settings, find the **Access Tokens** section.
   - Click **Add or Remove Pages** and select `RYZORA` (`https://www.facebook.com/ryzorabd`).
   - Click **Generate Token** and save this securely as `META_PAGE_ACCESS_TOKEN`.
   - Ensure the token has `pages_messaging`, `pages_show_list`, and `pages_read_engagement` permissions.

4. **Configure Webhook:**
   - In Messenger Settings > Webhooks, click **Add Callback URL**.
   - **Callback URL:** `https://your-cloud-run-domain.a.run.app/webhook`
   - **Verify Token:** `ryzora_messenger_verify_token_2026` (must match `META_VERIFY_TOKEN`).
   - Click **Verify and Save**. Meta will issue a `GET /webhook?hub.mode=subscribe&hub.challenge=...`.
   - Once verified, click **Add Subscriptions** for the `ryzorabd` Page and select:
     - `messages` (Mandatory)
     - `messaging_postbacks` (Mandatory)
     - `message_deliveries`
     - `message_reads`

5. **App Secret & Security:**
   - Go to App Settings > Basic. Copy **App Secret** and set as `META_APP_SECRET`.
   - The backend validates `X-Hub-Signature-256` using this secret to reject unauthorized HTTP requests.

---

### Step 2: Supabase Database Migration

1. Create a project at [Supabase](https://supabase.com) (Region: Singapore `ap-southeast-1` or nearest to Bangladesh).
2. Open the **SQL Editor** in the Supabase Dashboard.
3. Paste and run the entire content of `/supabase/migrations/20261025000000_init_ryzora.sql`.
4. This bootstraps:
   - `products` (seeded with initial RYZORA apparel, earbuds, smartwatch, and power bank in BDT ৳).
   - `customers` & `conversations` (with tracking for AI Auto-reply ON/OFF and Human Takeover).
   - `messages` (with `fb_mid` unique constraint to prevent duplicate processing).
   - `orders` & `order_items` (with BDT delivery calculation: Inside Dhaka ৳60 / Outside Dhaka ৳120).
   - `business_settings` (pricing, delivery terms, return policy).
   - `automation_logs` (audit trail for webhook events).
5. Obtain `SUPABASE_URL` and `SUPABASE_SERVICE_ROLE_KEY` from Project Settings > API.

---

### Step 3: Deployment to Google Cloud Run

1. **Build and push container image:**
   ```bash
   gcloud builds submit --tag gcr.io/YOUR_GCP_PROJECT/ryzora-agent:latest server/
   ```

2. **Deploy with Environment Variables:**
   ```bash
   gcloud run deploy ryzora-messenger-agent \
     --image gcr.io/YOUR_GCP_PROJECT/ryzora-agent:latest \
     --platform managed \
     --region asia-southeast1 \
     --allow-unauthenticated \
     --set-env-vars META_APP_ID="YOUR_APP_ID",\
   META_APP_SECRET="YOUR_APP_SECRET",\
   META_PAGE_ID="ryzorabd",\
   META_PAGE_ACCESS_TOKEN="YOUR_PAGE_ACCESS_TOKEN",\
   META_VERIFY_TOKEN="ryzora_messenger_verify_token_2026",\
   GEMINI_API_KEY="YOUR_GEMINI_API_KEY",\
   SUPABASE_URL="https://your-project.supabase.co",\
   SUPABASE_SERVICE_ROLE_KEY="YOUR_SERVICE_ROLE_KEY"
   ```

3. Note the HTTPS URL generated (e.g. `https://ryzora-agent-xxxx.a.run.app`) and configure it as your Webhook URL in Meta.

---

### Step 4: Verification & Smoke Test Checklist

- [x] **GET Webhook Verification:** Responds with `hub.challenge` on valid token, returns 403 on mismatch.
- [x] **Signature Verification:** Rejects tampered payloads without matching HMAC-SHA256.
- [x] **Duplicate Mid Prevention:** Duplicate Facebook messages are dropped before executing AI reasoning.
- [x] **Price & Stock Integrity:** AI answers strictly according to verified catalog prices (Earbuds ৳1990, Smart Watch ৳3250, Tee ৳790, Polo ৳1050, Backpack ৳1750, PowerBank ৳1590).
- [x] **Bangladeshi Phone Validation:** Supports 11-digit mobile numbers (013-019), +880, and Bangla numerals.
- [x] **Delivery Policy:** Correctly applies Inside Dhaka (৳60) and Outside Dhaka (৳120).
- [x] **Human Takeover:** Customer requests for "human", "agent", "manager", "রিফান্ড" or "অভিযোগ" immediately alert human admins and pause automated replies.
- [x] **Emergency Kill Switch:** Global AI auto-reply toggle disables bot replies instantly when needed.
