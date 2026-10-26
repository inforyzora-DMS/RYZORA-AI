-- ====================================================================
-- RYZORA Facebook AI Customer Service & Order Management System
-- Schema Migration: 20261025000000_init_ryzora.sql
-- Brand: RYZORA ("Smart Choice. Better Life.")
-- Website: https://ryzorabd.com | Facebook: https://www.facebook.com/ryzorabd
-- Location: Bangladesh | Currency: BDT (৳)
-- ====================================================================

-- 1. Enable necessary extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 2. Admin Users Table
CREATE TABLE IF NOT EXISTS admin_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    role VARCHAR(50) DEFAULT 'admin' CHECK (role IN ('super_admin', 'admin', 'agent')),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 3. Connected Facebook Pages Table
CREATE TABLE IF NOT EXISTS connected_pages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    page_id VARCHAR(100) UNIQUE NOT NULL,
    page_name VARCHAR(255) NOT NULL,
    page_access_token_encrypted TEXT,
    app_id VARCHAR(100),
    webhook_subscribed BOOLEAN DEFAULT FALSE,
    is_auto_reply_enabled BOOLEAN DEFAULT TRUE,
    takeover_cooldown_minutes INT DEFAULT 30,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 4. Products Table (Source of truth for stock, prices in BDT ৳, and variants)
CREATE TABLE IF NOT EXISTS products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sku VARCHAR(60) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    bangla_name VARCHAR(255),
    description TEXT,
    category VARCHAR(100) NOT NULL,
    price_bdt NUMERIC(10, 2) NOT NULL CHECK (price_bdt >= 0),
    discount_price_bdt NUMERIC(10, 2) CHECK (discount_price_bdt >= 0),
    images TEXT[] DEFAULT '{}',
    available_sizes TEXT[] DEFAULT '{}',
    available_colors TEXT[] DEFAULT '{}',
    stock_quantity INT DEFAULT 0 CHECK (stock_quantity >= 0),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 5. Customers Table (Facebook Page-Scoped ID & Contact details)
CREATE TABLE IF NOT EXISTS customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    fb_psid VARCHAR(100) UNIQUE NOT NULL,
    full_name VARCHAR(255),
    phone VARCHAR(20),
    delivery_address TEXT,
    city VARCHAR(100) DEFAULT 'Dhaka',
    total_orders INT DEFAULT 0,
    preferred_language VARCHAR(10) DEFAULT 'bn' CHECK (preferred_language IN ('bn', 'en', 'banglish')),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 6. Conversations Table (Manages AI State, Human Takeover, and Context)
CREATE TABLE IF NOT EXISTS conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID REFERENCES customers(id) ON DELETE CASCADE,
    page_id VARCHAR(100) NOT NULL,
    channel VARCHAR(50) DEFAULT 'messenger',
    is_ai_active BOOLEAN DEFAULT TRUE,
    human_takeover_at TIMESTAMPTZ,
    human_takeover_by UUID REFERENCES admin_users(id),
    current_intent VARCHAR(100) DEFAULT 'GENERAL_INQUIRY',
    pending_order_draft JSONB,
    last_message_at TIMESTAMPTZ DEFAULT NOW(),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 7. Messages Table (Persistent log of incoming & outgoing messages)
CREATE TABLE IF NOT EXISTS messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID REFERENCES conversations(id) ON DELETE CASCADE,
    fb_mid VARCHAR(255) UNIQUE, -- Meta Message ID for idempotency
    sender_type VARCHAR(20) NOT NULL CHECK (sender_type IN ('customer', 'ai_agent', 'human_admin', 'system')),
    content TEXT NOT NULL,
    detected_intent VARCHAR(100),
    confidence_score NUMERIC(4, 3),
    raw_payload JSONB,
    sent_at TIMESTAMPTZ DEFAULT NOW()
);

-- 8. Orders Table (E-commerce Order fulfillment in Bangladesh)
CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number VARCHAR(50) UNIQUE NOT NULL,
    customer_id UUID REFERENCES customers(id),
    conversation_id UUID REFERENCES conversations(id),
    customer_name VARCHAR(255) NOT NULL,
    customer_phone VARCHAR(20) NOT NULL,
    delivery_address TEXT NOT NULL,
    delivery_zone VARCHAR(50) DEFAULT 'Inside Dhaka' CHECK (delivery_zone IN ('Inside Dhaka', 'Outside Dhaka')),
    delivery_fee_bdt NUMERIC(10, 2) DEFAULT 60.00,
    subtotal_bdt NUMERIC(10, 2) NOT NULL CHECK (subtotal_bdt >= 0),
    total_bdt NUMERIC(10, 2) NOT NULL CHECK (total_bdt >= 0),
    status VARCHAR(50) DEFAULT 'Pending' CHECK (status IN ('Pending', 'Confirmed', 'Processing', 'Shipped', 'Delivered', 'Cancelled')),
    payment_method VARCHAR(50) DEFAULT 'Cash on Delivery (COD)' CHECK (payment_method IN ('Cash on Delivery (COD)', 'bKash', 'Nagad', 'Prepaid Card')),
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 9. Order Items Table
CREATE TABLE IF NOT EXISTS order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID REFERENCES products(id),
    product_name VARCHAR(255) NOT NULL,
    variant_details VARCHAR(100), -- Size, Color
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price_bdt NUMERIC(10, 2) NOT NULL CHECK (unit_price_bdt >= 0),
    total_price_bdt NUMERIC(10, 2) NOT NULL CHECK (total_price_bdt >= 0),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 10. Business Settings & AI Personality
CREATE TABLE IF NOT EXISTS business_settings (
    key VARCHAR(100) PRIMARY KEY,
    value JSONB NOT NULL,
    description TEXT,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 11. Automation & Webhook Logs (Audit trail for Meta calls and error handling)
CREATE TABLE IF NOT EXISTS automation_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('success', 'failure', 'warning', 'skipped')),
    sender_id VARCHAR(100),
    details JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Indexes for high-throughput messaging & order lookups
CREATE INDEX IF NOT EXISTS idx_customers_fb_psid ON customers(fb_psid);
CREATE INDEX IF NOT EXISTS idx_conversations_customer_id ON conversations(customer_id);
CREATE INDEX IF NOT EXISTS idx_messages_conversation_id ON messages(conversation_id);
CREATE INDEX IF NOT EXISTS idx_messages_fb_mid ON messages(fb_mid);
CREATE INDEX IF NOT EXISTS idx_orders_customer_id ON orders(customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_order_number ON orders(order_number);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_products_sku ON products(sku);
CREATE INDEX IF NOT EXISTS idx_products_active ON products(is_active);
CREATE INDEX IF NOT EXISTS idx_automation_logs_created_at ON automation_logs(created_at DESC);

-- Enable Row Level Security (RLS)
ALTER TABLE admin_users ENABLE ROW LEVEL SECURITY;
ALTER TABLE connected_pages ENABLE ROW LEVEL SECURITY;
ALTER TABLE products ENABLE ROW LEVEL SECURITY;
ALTER TABLE customers ENABLE ROW LEVEL SECURITY;
ALTER TABLE conversations ENABLE ROW LEVEL SECURITY;
ALTER TABLE messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE order_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE automation_logs ENABLE ROW LEVEL SECURITY;

-- Service Role policies (Allows backend server full access via service role key)
CREATE POLICY "Service role full access on admin_users" ON admin_users FOR ALL USING (auth.role() = 'service_role');
CREATE POLICY "Service role full access on connected_pages" ON connected_pages FOR ALL USING (auth.role() = 'service_role');
CREATE POLICY "Service role full access on products" ON products FOR ALL USING (auth.role() = 'service_role');
CREATE POLICY "Service role full access on customers" ON customers FOR ALL USING (auth.role() = 'service_role');
CREATE POLICY "Service role full access on conversations" ON conversations FOR ALL USING (auth.role() = 'service_role');
CREATE POLICY "Service role full access on messages" ON messages FOR ALL USING (auth.role() = 'service_role');
CREATE POLICY "Service role full access on orders" ON orders FOR ALL USING (auth.role() = 'service_role');
CREATE POLICY "Service role full access on order_items" ON order_items FOR ALL USING (auth.role() = 'service_role');
CREATE POLICY "Service role full access on business_settings" ON business_settings FOR ALL USING (auth.role() = 'service_role');
CREATE POLICY "Service role full access on automation_logs" ON automation_logs FOR ALL USING (auth.role() = 'service_role');

-- Public / Authenticated read policies for safe frontend access
CREATE POLICY "Public read active products" ON products FOR SELECT USING (is_active = TRUE);

-- Initial Seed Data: Verified RYZORA Bangladesh Catalog (Prices in BDT ৳)
INSERT INTO products (sku, name, bangla_name, description, category, price_bdt, discount_price_bdt, available_sizes, available_colors, stock_quantity, is_active)
VALUES
('RYZ-PRO-01', 'RYZORA Premium Pro Wireless Earbuds', 'রাইজোরা প্রিমিয়াম প্রো ওয়্যারলেস এয়ারবাডস', 'Active Noise Cancellation, 36h Playtime, Deep Bass, IPX5 Waterproof.', 'Electronics', 2450.00, 1990.00, ARRAY['Standard'], ARRAY['Matte Black', 'Pearl White'], 45, TRUE),
('RYZ-SW-02', 'RYZORA Apex AMOLED Smart Watch', 'রাইজোরা অ্যাপেক্স অ্যামোলেড স্মার্ট ওয়াচ', '1.43 inch AMOLED Display, Bluetooth Calling, 100+ Sports Modes, Health Tracking.', 'Electronics', 3800.00, 3250.00, ARRAY['Free Size'], ARRAY['Midnight Black', 'Metallic Silver'], 30, TRUE),
('RYZ-TEE-03', 'RYZORA Heavyweight Cotton Graphic Tee', 'রাইজোরা হেভিওয়েট কটন টি-শার্ট', '220 GSM 100% Combed Compact Cotton, Pre-shrunk, Signature High-Density Print.', 'Fashion', 950.00, 790.00, ARRAY['M', 'L', 'XL', 'XXL'], ARRAY['Jet Black', 'Off White', 'Sage Green'], 120, TRUE),
('RYZ-POLO-04', 'RYZORA Classic Pique Knit Polo', 'রাইজোরা ক্লাসিক পোলো শার্ট', 'Premium Mercerized Cotton Pique, Structured Collar, Regular Fit.', 'Fashion', 1250.00, 1050.00, ARRAY['M', 'L', 'XL'], ARRAY['Navy Blue', 'Maroon', 'Olive Green'], 85, TRUE),
('RYZ-BAG-05', 'RYZORA Urban Commuter Waterproof Backpack', 'রাইজোরা আরবান ওয়াটারপ্রুফ ব্যাকপ্যাক', 'Fits up to 15.6 inch laptop, Hidden Anti-theft Pocket, Ergonomic Shoulder Straps.', 'Accessories', 2150.00, 1750.00, ARRAY['20L'], ARRAY['Obsidian Black', 'Steel Grey'], 25, TRUE),
('RYZ-PB-06', 'RYZORA PowerCore 20000mAh 22.5W Fast Power Bank', 'রাইজোরা পাওয়ারকোর ২০০০০ এমএএইচ পাওয়ার ব্যাংক', 'PD 3.0 & QC 4.0 Fast Charging, Digital Battery Indicator, Dual Type-C Output.', 'Electronics', 1950.00, 1590.00, ARRAY['Standard'], ARRAY['Space Grey'], 60, TRUE)
ON CONFLICT (sku) DO NOTHING;

-- Initial Seed Data: Business Settings & Delivery Policies in Bangladesh
INSERT INTO business_settings (key, value, description)
VALUES
('business_profile', '{
    "brand_name": "RYZORA",
    "tagline": "Smart Choice. Better Life.",
    "website": "https://ryzorabd.com",
    "facebook_page": "https://www.facebook.com/ryzorabd",
    "location": "Bangladesh",
    "hotline": "+880 1700-000000",
    "email": "info.ryzora@gmail.com"
}'::jsonb, 'General business profile information'),

('delivery_policy', '{
    "inside_dhaka_fee_bdt": 60,
    "outside_dhaka_fee_bdt": 120,
    "inside_dhaka_delivery_days": "1-2 days",
    "outside_dhaka_delivery_days": "2-4 days",
    "cash_on_delivery": true,
    "free_delivery_threshold_bdt": 3000
}'::jsonb, 'Bangladesh shipping rates and timeline'),

('return_policy', '{
    "duration_days": 7,
    "conditions": "Unused, original tags intact, and invoice provided. Damaged or wrong products are replaced without extra charge within 72 hours."
}'::jsonb, 'Return and refund conditions'),

('ai_agent_config', '{
    "default_language": "auto_detect",
    "supported_languages": ["bn", "banglish", "en"],
    "persona": "Polite, helpful Bangladeshi sales and support specialist for RYZORA.",
    "is_global_auto_reply_enabled": true,
    "escalate_on_keywords": ["refund", "damaged", "scam", "complaint", "fraud", "manager", "human", "কথা বলতে চাই", "অভিযোগ", "রিফান্ড"],
    "order_confirmation_required": true
}'::jsonb, 'AI Agent behavioral hyperparameters and rules')
ON CONFLICT (key) DO NOTHING;

-- Seed Connected Meta Page
INSERT INTO connected_pages (page_id, page_name, app_id, webhook_subscribed, is_auto_reply_enabled)
VALUES ('1268010728837960', 'RYZORA (@ryzorabd)', '1268010728837960', TRUE, TRUE)
ON CONFLICT (page_id) DO NOTHING;
