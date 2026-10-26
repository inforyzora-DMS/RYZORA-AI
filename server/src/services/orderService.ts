import type { Order, OrderDraft, Product } from '../types.ts';
import { catalogService } from './catalogService.ts';

export class OrderService {
  private orders: Order[] = [];

  /**
   * Validates Bangladeshi mobile phone numbers:
   * Formats: 013XXXXXXXX, 014XXXXXXXX, 015XXXXXXXX, 016XXXXXXXX, 017XXXXXXXX, 018XXXXXXXX, 019XXXXXXXX
   * Supports optional +88 or 88 prefix.
   */
  public validateBangladeshiPhone(inputPhone: string): { isValid: boolean; normalizedPhone?: string } {
    if (!inputPhone) return { isValid: false };
    // Remove all whitespace, dashes, parens
    let cleaned = inputPhone.replace(/[\s\-\(\)]/g, '');

    // Convert Bangla numerals to English
    const banglaDigits = ['০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯'];
    for (let i = 0; i < 10; i++) {
      cleaned = cleaned.replaceAll(banglaDigits[i], String(i));
    }

    if (cleaned.startsWith('+880')) {
      cleaned = cleaned.substring(3);
    } else if (cleaned.startsWith('880')) {
      cleaned = cleaned.substring(2);
    }

    const bdPhoneRegex = /^01[3-9]\d{8}$/;
    if (bdPhoneRegex.test(cleaned)) {
      return { isValid: true, normalizedPhone: cleaned };
    }
    return { isValid: false };
  }

  /**
   * Determines delivery zone & fee based on customer address in Bangladesh.
   */
  public determineDeliveryZone(address: string): { zone: 'Inside Dhaka' | 'Outside Dhaka'; fee: number } {
    const lower = address.toLowerCase();
    const isInsideDhaka =
      lower.includes('dhaka') ||
      lower.includes('ঢাকা') ||
      lower.includes('dhanmondi') ||
      lower.includes('gulshan') ||
      lower.includes('banani') ||
      lower.includes('uttara') ||
      lower.includes('mirpur') ||
      lower.includes('motijheel') ||
      lower.includes('mohammadpur') ||
      lower.includes('bashundhara') ||
      lower.includes('badda');

    if (isInsideDhaka) {
      return { zone: 'Inside Dhaka', fee: 60 };
    }
    return { zone: 'Outside Dhaka', fee: 120 };
  }

  /**
   * Generates a unique, professional RYZORA order number.
   * Format: RYZ-YYYYMMDD-XXXX
   */
  public generateOrderNumber(): string {
    const dateStr = new Date().toISOString().slice(0, 10).replace(/-/g, '');
    const randomSuffix = Math.floor(1000 + Math.random() * 9000);
    return `RYZ-${dateStr}-${randomSuffix}`;
  }

  /**
   * Formats an order summary for the customer in Bangla and English.
   */
  public formatOrderSummary(draft: OrderDraft, product: Product): string {
    const qty = draft.quantity || 1;
    const unitPrice = product.discountPriceBdt || product.priceBdt;
    const subtotal = unitPrice * qty;
    const { zone, fee } = this.determineDeliveryZone(draft.deliveryAddress || 'Dhaka');
    const total = subtotal + fee;

    return `📋 RYZORA অর্ডার সারাংশ (Order Summary):
━━━━━━━━━━━━━━━━━━
🛍️ পণ্য (Item): ${product.name}
🎨 ভ্যারিয়েন্ট (Variant): ${draft.variant || 'Standard'}
🔢 পরিমাণ (Quantity): ${qty} পিস
💵 সাবটোটাল: ৳${subtotal}
🚚 ডেলিভারি চার্জ (${zone}): ৳${fee}
💰 সর্বমোট প্রদেয় (Total): ৳${total}

👤 গ্রাহকের নাম (Name): ${draft.customerName}
📱 মোবাইল (Phone): ${draft.customerPhone}
📍 ঠিকানা (Address): ${draft.deliveryAddress}
💳 পেমেন্ট: ক্যাশ অন ডেলিভারি (Cash on Delivery)
━━━━━━━━━━━━━━━━━━
অর্ডারটি কনফার্ম করতে দয়া করে 'CONFIRM' বা 'হ্যাঁ' লিখুন।`;
  }

  /**
   * Confirms and creates the order.
   */
  public createOrder(draft: OrderDraft, product: Product): Order {
    const qty = draft.quantity || 1;
    const unitPrice = product.discountPriceBdt || product.priceBdt;
    const subtotal = unitPrice * qty;
    const { zone, fee } = this.determineDeliveryZone(draft.deliveryAddress || '');
    const total = subtotal + fee;

    const newOrder: Order = {
      id: `ord_${Date.now()}`,
      orderNumber: this.generateOrderNumber(),
      customerName: draft.customerName || 'Customer',
      customerPhone: draft.customerPhone || '',
      deliveryAddress: draft.deliveryAddress || '',
      deliveryZone: zone,
      deliveryFeeBdt: fee,
      subtotalBdt: subtotal,
      totalBdt: total,
      status: 'Pending',
      paymentMethod: 'Cash on Delivery (COD)',
      items: [
        {
          productName: product.name,
          variant: draft.variant || 'Standard',
          quantity: qty,
          unitPriceBdt: unitPrice,
          totalPriceBdt: subtotal,
        },
      ],
      createdAt: new Date().toISOString(),
    };

    this.orders.unshift(newOrder);
    return newOrder;
  }

  public getAllOrders(): Order[] {
    return this.orders;
  }

  public updateOrderStatus(orderId: string, status: Order['status']): Order | undefined {
    const order = this.orders.find((o) => o.id === orderId || o.orderNumber === orderId);
    if (order) {
      order.status = status;
      return order;
    }
    return undefined;
  }
}

export const orderService = new OrderService();
