import { Router, Request, Response } from 'express';
import { catalogService } from '../services/catalogService.js';
import { orderService } from '../services/orderService.js';
import { aiReplyAgent } from '../services/aiReplyAgent.js';
import { recentWebhookLogs } from './webhook.js';
import { config } from '../config.js';

export const apiRouter = Router();

let isGlobalAiAutoReplyEnabled = true;

/**
 * GET /api/status - Connection and System health status
 */
apiRouter.get('/status', (_req: Request, res: Response) => {
  res.json({
    status: 'ONLINE',
    brand: 'RYZORA',
    tagline: 'Smart Choice. Better Life.',
    facebookPage: {
      pageId: config.meta.pageId,
      connected: Boolean(config.meta.pageAccessToken),
      webhookSubscribed: true,
      apiVersion: config.meta.apiVersion,
    },
    aiAgent: {
      enabled: isGlobalAiAutoReplyEnabled,
      model: config.gemini.model,
      geminiKeyConfigured: Boolean(config.gemini.apiKey),
    },
    database: {
      connected: Boolean(config.supabase.url),
      type: 'Supabase PostgreSQL',
    },
    timestamp: new Date().toISOString(),
  });
});

/**
 * POST /api/ai/toggle - Toggle AI auto reply globally
 */
apiRouter.post('/ai/toggle', (req: Request, res: Response) => {
  const { enabled } = req.body;
  if (typeof enabled === 'boolean') {
    isGlobalAiAutoReplyEnabled = enabled;
  } else {
    isGlobalAiAutoReplyEnabled = !isGlobalAiAutoReplyEnabled;
  }
  res.json({ enabled: isGlobalAiAutoReplyEnabled });
});

/**
 * GET /api/products - Get all products
 */
apiRouter.get('/products', (_req: Request, res: Response) => {
  res.json(catalogService.getAllActiveProducts());
});

/**
 * GET /api/orders - Get all orders
 */
apiRouter.get('/orders', (_req: Request, res: Response) => {
  res.json(orderService.getAllOrders());
});

/**
 * PATCH /api/orders/:id/status - Update order status
 */
apiRouter.patch('/orders/:id/status', (req: Request, res: Response) => {
  const { id } = req.params;
  const { status } = req.body;
  const updated = orderService.updateOrderStatus(id, status);
  if (!updated) {
    res.status(404).json({ error: 'Order not found' });
    return;
  }
  res.json(updated);
});

/**
 * GET /api/webhook/logs - Recent webhook audit logs
 */
apiRouter.get('/webhook/logs', (_req: Request, res: Response) => {
  res.json(recentWebhookLogs);
});

/**
 * POST /api/simulate - Interactive Messenger testing endpoint
 */
apiRouter.post('/simulate', async (req: Request, res: Response) => {
  const { senderPsid = 'test_user_dhaka_01', messageText = 'Price of earbuds' } = req.body;

  const result = await aiReplyAgent.processMessage(senderPsid, messageText, config.gemini.apiKey);

  res.json({
    senderPsid,
    input: messageText,
    result,
  });
});
