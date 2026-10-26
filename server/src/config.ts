/**
 * RYZORA Backend Configuration
 */

export interface AppConfig {
  port: number;
  meta: {
    appId: string;
    appSecret: string;
    verifyToken: string;
    pageAccessToken: string;
    pageId: string;
    apiVersion: string;
  };
  gemini: {
    apiKey: string;
    model: string;
  };
  supabase: {
    url: string;
    serviceRoleKey: string;
  };
  business: {
    name: string;
    tagline: string;
    website: string;
    insideDhakaFee: number;
    outsideDhakaFee: number;
    freeDeliveryThreshold: number;
  };
}

export const config: AppConfig = {
  port: parseInt(process.env.PORT || '8080', 10),
  meta: {
    appId: process.env.META_APP_ID || '1268010728837960',
    appSecret: process.env.META_APP_SECRET || '',
    verifyToken: process.env.META_VERIFY_TOKEN || 'ryzora_messenger_verify_token_2026',
    pageAccessToken: process.env.META_PAGE_ACCESS_TOKEN || '',
    pageId: process.env.META_PAGE_ID || '1268010728837960',
    apiVersion: 'v21.0',
  },
  gemini: {
    apiKey: process.env.GEMINI_API_KEY || '',
    model: process.env.GEMINI_MODEL || 'gemini-1.5-flash',
  },
  supabase: {
    url: process.env.SUPABASE_URL || '',
    serviceRoleKey: process.env.SUPABASE_SERVICE_ROLE_KEY || '',
  },
  business: {
    name: 'RYZORA',
    tagline: 'Smart Choice. Better Life.',
    website: 'https://ryzorabd.com',
    insideDhakaFee: 60,
    outsideDhakaFee: 120,
    freeDeliveryThreshold: 3000,
  },
};
