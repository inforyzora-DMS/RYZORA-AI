import type { AIResponseResult, IntentType, OrderDraft } from '../types.ts';
import { catalogService } from './catalogService.ts';
import { orderService } from './orderService.ts';

/**
 * RYZORA AI Customer Service & Dialogue Agent
 * Handles Bangla, Banglish, and English intents accurately.
 */
export class AIReplyAgent {
  private customerContexts: Map<string, { orderDraft?: OrderDraft; lastIntent?: IntentType }> = new Map();

  /**
   * Generates a conversational reply based on user message and verified store facts.
   */
  public async processMessage(
    senderPsid: string,
    messageText: string,
    geminiApiKey?: string
  ): Promise<AIResponseResult> {
    const rawText = messageText.trim();
    const lowerText = rawText.toLowerCase();

    // 1. Prompt Injection Defense
    if (
      lowerText.includes('ignore previous instructions') ||
      lowerText.includes('system prompt') ||
      lowerText.includes('you are now') ||
      lowerText.includes('jailbreak') ||
      lowerText.includes('free product')
    ) {
      return {
        replyText: 'সম্মানিত গ্রাহক, আমি RYZORA-র সহায়ক এআই। আমাদের পণ্য, মূল্য বা অর্ডার সংক্রান্ত তথ্যের জন্য আপনাকে স্বাগতম। কীভাবে সাহায্য করতে পারি?',
        detectedIntent: 'GREETING',
        requiresHumanHandoff: false,
      };
    }

    // 2. Human Escalation Detection
    const escalationKeywords = [
      'human',
      'agent',
      'operator',
      'manager',
      'manush',
      'person',
      'কথা বলব',
      'মানুষ',
      'প্রতিনিধি',
      'অভিযোগ',
      'complaint',
      'scam',
      'fraud',
      'refund',
      'রিফান্ড',
    ];
    if (escalationKeywords.some((kw) => lowerText.includes(kw))) {
      return {
        replyText:
          'সম্মানিত গ্রাহক, আপনার অনুরোধটি আমাদের সিনিয়র কাস্টমার সাপোর্ট টিমের কাছে পাঠানো হয়েছে। খুব শীঘ্রই একজন প্রতিনিধি আপনার সাথে যুক্ত হবেন। ধন্যবাদ! (A human support executive will contact you shortly).',
        detectedIntent: 'HUMAN_HANDOFF',
        requiresHumanHandoff: true,
      };
    }

    // Get current context for customer
    const context = this.customerContexts.get(senderPsid) || {};

    // 3. Order Confirmation Check
    if (context.orderDraft && context.orderDraft.stage === 'AWAITING_CONFIRMATION') {
      const confirmWords = ['confirm', 'yes', 'ha', 'ok', 'কনফার্ম', 'হ্যাঁ', 'ঠিক আছে', 'পাঠিয়ে দিন'];
      const cancelWords = ['cancel', 'no', 'na', 'বাতিল', 'না'];

      if (confirmWords.some((w) => lowerText.includes(w))) {
        const product = catalogService.getProductById(context.orderDraft.productId || '');
        if (product) {
          const newOrder = orderService.createOrder(context.orderDraft, product);
          this.customerContexts.delete(senderPsid);

          return {
            replyText: `🎉 অভিনন্দন! আপনার RYZORA অর্ডারটি সফলভাবে সম্পন্ন হয়েছে।

📦 অর্ডার নম্বর: ${newOrder.orderNumber}
🛍️ পণ্য: ${product.name}
💰 মোট প্রদেয়: ৳${newOrder.totalBdt} (ক্যাশ অন ডেলিভারি)
🚚 ডেলিভারি: ${newOrder.deliveryZone === 'Inside Dhaka' ? '১-২ দিনের মধ্যে' : '২-৪ দিনের মধ্যে'}

আমাদের প্রতিনিধি ডেলিভারির পূর্বে ফোন করবেন। RYZORA-র সাথে থাকার জন্য ধন্যবাদ! ✨`,
            detectedIntent: 'ORDER_CONFIRM',
            requiresHumanHandoff: false,
            confirmedOrder: newOrder,
          };
        }
      } else if (cancelWords.some((w) => lowerText.includes(w))) {
        this.customerContexts.delete(senderPsid);
        return {
          replyText: 'আপনার অর্ডার ড্রাফটটি বাতিল করা হয়েছে। অন্য কোনো পণ্য সম্পর্কে জানতে চাইলে বলুন!',
          detectedIntent: 'ORDER_CANCEL',
          requiresHumanHandoff: false,
        };
      }
    }

    // 4. Order Details Input Check (Phone number or Address provided)
    const phoneCheck = orderService.validateBangladeshiPhone(rawText);
    if (phoneCheck.isValid && context.orderDraft && context.orderDraft.stage === 'AWAITING_DETAILS') {
      context.orderDraft.customerPhone = phoneCheck.normalizedPhone;
      if (!context.orderDraft.deliveryAddress) {
        this.customerContexts.set(senderPsid, context);
        return {
          replyText: 'ধন্যবাদ! এবার অনুগ্রহ করে আপনার সম্পূর্ণ ডেলিভারি ঠিকানা (জেলা, থানা/এলাকা সহ) লিখুন।',
          detectedIntent: 'ORDER_PROVIDE_DETAILS',
          requiresHumanHandoff: false,
          orderDraft: context.orderDraft,
        };
      }
    }

    // 5. Delivery Inquiry Intent
    if (
      lowerText.includes('delivery') ||
      lowerText.includes('shipping') ||
      lowerText.includes('ডেলিভারি') ||
      lowerText.includes('চার্জ') ||
      lowerText.includes('কতো দিন')
    ) {
      return {
        replyText: `🚚 RYZORA ডেলিভারি পলিসি:
• ঢাকার ভেতরে: ৳৬০ (১-২ কার্যদিবস)
• ঢাকার বাইরে: ৳১২০ (২-৪ কার্যদিবস)
• পেমেন্ট: ক্যাশ অন ডেলিভারি (পণ্য দেখে মূল্য পরিশোধ)
• ৳৩০০০ টাকার বেশি অর্ডারে ফ্রি ডেলিভারি!

কোন পণ্যটি আপনার পছন্দ হয়েছে? জানাবেন কি?`,
        detectedIntent: 'DELIVERY_INQUIRY',
        requiresHumanHandoff: false,
      };
    }

    // 6. Return Policy Intent
    if (
      lowerText.includes('return') ||
      lowerText.includes('warranty') ||
      lowerText.includes('exchange') ||
      lowerText.includes('রিটার্ন') ||
      lowerText.includes('ওয়ারেন্টি')
    ) {
      return {
        replyText: `🔄 RYZORA রিটার্ন ও এক্সচেঞ্জ পলিসি:
পণ্য হাতে পাওয়ার পর কোনো সমস্যা বা সাইজ এক্সচেঞ্জের জন্য ৭ দিনের সহজ রিটার্ন সুবিধা রয়েছে। ত্রুটিপূর্ণ পণ্য পাওয়া গেলে বিনা মূল্যে ৭২ ঘণ্টার মধ্যে পরিবর্তন করে দেওয়া হয়।`,
        detectedIntent: 'RETURN_POLICY',
        requiresHumanHandoff: false,
      };
    }

    // 7. Product Matching & Price / Info Inquiry
    const matchedProduct = catalogService.findProductByQuery(rawText);

    if (matchedProduct) {
      // Check if user wants to order this product
      const orderTriggers = ['order', 'buy', 'kinte', 'চাই', 'নিতে', 'কিনব', 'পাঠান'];
      const wantsToOrder = orderTriggers.some((t) => lowerText.includes(t));

      if (wantsToOrder) {
        const draft: OrderDraft = {
          productId: matchedProduct.id,
          productName: matchedProduct.name,
          unitPriceBdt: matchedProduct.discountPriceBdt || matchedProduct.priceBdt,
          quantity: 1,
          stage: 'AWAITING_DETAILS',
        };
        this.customerContexts.set(senderPsid, { orderDraft: draft, lastIntent: 'ORDER_START' });

        return {
          replyText: `অর্ডার প্রসেস শুরু করছি! 
🛍️ পণ্য: ${matchedProduct.name}
💰 মূল্য: ৳${draft.unitPriceBdt}

অনুগ্রহ করে আপনার:
১. নাম
২. মোবাইল নম্বর
৩. সম্পূর্ণ ঠিকানা
লিখে পাঠিয়ে দিন।`,
          detectedIntent: 'ORDER_START',
          requiresHumanHandoff: false,
          orderDraft: draft,
        };
      }

      // Price / Stock / Variant Inquiry for the product
      const priceText = matchedProduct.discountPriceBdt
        ? `৳${matchedProduct.discountPriceBdt} (রেগুলার মূল্য ৳${matchedProduct.priceBdt})`
        : `৳${matchedProduct.priceBdt}`;

      const sizes = matchedProduct.availableSizes.join(', ');
      const colors = matchedProduct.availableColors.join(', ');
      const stockStatus = matchedProduct.stockQuantity > 0 ? `স্টকে রয়েছে (${matchedProduct.stockQuantity} পিস)` : 'স্টক শেষ';

      return {
        replyText: `✨ ${matchedProduct.name}
💵 অফার মূল্য: ${priceText}
📦 সাইজ: ${sizes}
🎨 কালার: ${colors}
📊 প্রাপ্যতা: ${stockStatus}

অর্ডার করতে চাইলে 'Order' লিখুন অথবা আপনার সাইজ ও ঠিকানা দিন।`,
        detectedIntent: 'PRICE_INQUIRY',
        requiresHumanHandoff: false,
        quickReplies: [
          { title: '🛒 Order Now', payload: `ORDER_${matchedProduct.sku}` },
          { title: '🚚 Delivery Fee', payload: 'DELIVERY_INFO' },
          { title: '📞 Human Agent', payload: 'CALL_AGENT' },
        ],
      };
    }

    // 8. General Greetings
    const greetingWords = ['hi', 'hello', 'hey', 'salam', 'সালাম', 'কেমন আছেন'];
    if (greetingWords.some((w) => lowerText.startsWith(w) || lowerText === w)) {
      return {
        replyText:
          'আসসালামু আলাইকুম! RYZORA-তে আপনাকে স্বাগতম। "Smart Choice. Better Life." আমাদের প্রিমিয়াম গ্যাজেট ও লাইফস্টাইল পণ্য সম্পর্কে জানতে বা অর্ডার করতে কী তথ্য প্রয়োজন জানান!',
        detectedIntent: 'GREETING',
        requiresHumanHandoff: false,
        quickReplies: [
          { title: '🛍️ All Products', payload: 'VIEW_PRODUCTS' },
          { title: '🚚 Delivery Policy', payload: 'DELIVERY_INFO' },
          { title: '📞 Speak to Human', payload: 'CALL_AGENT' },
        ],
      };
    }

    // 9. Default Fallback
    return {
      replyText: `ধন্যবাদ আপনার মেসেজের জন্য! RYZORA-র প্রিমিয়াম টি-শার্ট, পোলো, ইয়ারবাডস, স্মার্ট ওয়াচ বা ব্যাকপ্যাকের দাম ও স্টক জানতে পণ্যের নাম লিখুন, অথবা সরাসরি প্রতিনিধিকে কল করতে পারেন।`,
      detectedIntent: 'GENERAL_FAQ',
      requiresHumanHandoff: false,
      quickReplies: [
        { title: '🎧 Earbuds Price', payload: 'PRICE_EARBUDS' },
        { title: '⌚ Smartwatch', payload: 'PRICE_SMARTWATCH' },
        { title: '👕 T-Shirts', payload: 'PRICE_TSHIRTS' },
      ],
    };
  }
}

export const aiReplyAgent = new AIReplyAgent();
