/**
 * RYZORA Facebook Messenger AI Auto-Reply & Order Management Types
 */

export interface MetaWebhookEvent {
  object: 'page';
  entry: MetaWebhookEntry[];
}

export interface MetaWebhookEntry {
  id: string; // Page ID
  time: number;
  messaging: MetaMessagingEvent[];
}

export interface MetaMessagingEvent {
  sender: { id: string }; // Customer PSID
  recipient: { id: string }; // Page ID
  timestamp: number;
  message?: {
    mid: string; // Message ID
    text?: string;
    quick_reply?: {
      payload: string;
    };
    attachments?: Array<{
      type: string;
      payload: { url?: string };
    }>;
  };
  postback?: {
    mid?: string;
    title: string;
    payload: string;
  };
  delivery?: {
    mids: string[];
    watermark: number;
  };
  read?: {
    watermark: number;
  };
}

export type IntentType =
  | 'PRICE_INQUIRY'
  | 'PRODUCT_INFO'
  | 'STOCK_AVAILABILITY'
  | 'DELIVERY_INQUIRY'
  | 'RETURN_POLICY'
  | 'ORDER_START'
  | 'ORDER_PROVIDE_DETAILS'
  | 'ORDER_CONFIRM'
  | 'ORDER_CANCEL'
  | 'HUMAN_HANDOFF'
  | 'GREETING'
  | 'GENERAL_FAQ'
  | 'COMPLAINT'
  | 'UNKNOWN';

export interface ProductVariant {
  size?: string;
  color?: string;
}

export interface Product {
  id: string;
  sku: string;
  name: string;
  banglaName?: string;
  description: string;
  category: string;
  priceBdt: number;
  discountPriceBdt?: number;
  availableSizes: string[];
  availableColors: string[];
  stockQuantity: number;
  isActive: boolean;
}

export interface OrderDraft {
  productId?: string;
  productName?: string;
  variant?: string;
  quantity?: number;
  unitPriceBdt?: number;
  deliveryZone?: 'Inside Dhaka' | 'Outside Dhaka';
  customerName?: string;
  customerPhone?: string;
  deliveryAddress?: string;
  stage: 'SELECTING_PRODUCT' | 'AWAITING_DETAILS' | 'AWAITING_CONFIRMATION' | 'CONFIRMED';
}

export interface Order {
  id: string;
  orderNumber: string;
  customerName: string;
  customerPhone: string;
  deliveryAddress: string;
  deliveryZone: 'Inside Dhaka' | 'Outside Dhaka';
  deliveryFeeBdt: number;
  subtotalBdt: number;
  totalBdt: number;
  status: 'Pending' | 'Confirmed' | 'Processing' | 'Shipped' | 'Delivered' | 'Cancelled';
  paymentMethod: string;
  items: Array<{
    productName: string;
    variant: string;
    quantity: number;
    unitPriceBdt: number;
    totalPriceBdt: number;
  }>;
  createdAt: string;
}

export interface AIResponseResult {
  replyText: string;
  detectedIntent: IntentType;
  requiresHumanHandoff: boolean;
  orderDraft?: OrderDraft;
  confirmedOrder?: Order;
  quickReplies?: Array<{ title: string; payload: string }>;
}
