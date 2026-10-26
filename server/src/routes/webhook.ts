import { Router, Request, Response } from 'express';
import { config } from '../config.js';
import { verifyMetaSignature } from '../services/signatureVerifier.js';
import { deduplicationManager } from '../services/deduplication.js';
import { aiReplyAgent } from '../services/aiReplyAgent.js';
import { messengerApi } from '../services/messengerApi.js';
import { MetaWebhookEvent } from '../types.js';

export const webhookRouter = Router();

// In-memory automation activity logs
export interface WebhookLogItem {
  id: string;
  timestamp: string;
  type: string;
  senderId?: string;
  mid?: string;
  status: 'SUCCESS' | 'DUPLICATE' | 'ERROR' | 'UNSUPPORTED' | 'VERIFIED';
  details: string;
}

export const recentWebhookLogs: WebhookLogItem[] = [];

function addLog(log: Omit<WebhookLogItem, 'id' | 'timestamp'>) {
  recentWebhookLogs.unshift({
    id: `log_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`,
    timestamp: new Date().toISOString(),
    ...log,
  });
  if (recentWebhookLogs.length > 100) recentWebhookLogs.pop();
}

/**
 * GET /webhook - Meta Webhook Verification Endpoint
 * Meta sends hub.mode, hub.verify_token, and hub.challenge
 */
webhookRouter.get('/', (req: Request, res: Response): void => {
  const mode = req.query['hub.mode'];
  const token = req.query['hub.verify_token'];
  const challenge = req.query['hub.challenge'];

  if (mode === 'subscribe' && token === config.meta.verifyToken) {
    console.log('[Meta Webhook] Verification successful with token match.');
    addLog({
      type: 'VERIFICATION',
      status: 'VERIFIED',
      details: 'Meta challenge verified and accepted.',
    });
    res.status(200).send(challenge);
  } else {
    console.warn('[Meta Webhook] Verification failed. Invalid or mismatched token.');
    addLog({
      type: 'VERIFICATION',
      status: 'ERROR',
      details: `Token mismatch. Received: ${token}, Expected: ${config.meta.verifyToken}`,
    });
    res.sendStatus(403);
  }
});

/**
 * POST /webhook - Meta Messenger Event Receiver
 */
webhookRouter.post('/', async (req: Request, res: Response): Promise<void> => {
  const signature = req.headers['x-hub-signature-256'] as string | undefined;

  // Verify HMAC-SHA256 signature if appSecret is configured
  if (config.meta.appSecret) {
    const rawBody = (req as unknown as { rawBody?: Buffer }).rawBody || JSON.stringify(req.body);
    const isValid = verifyMetaSignature(rawBody, signature, config.meta.appSecret);

    if (!isValid) {
      console.error('[Meta Webhook] Invalid HMAC-SHA256 signature header.');
      addLog({
        type: 'SIGNATURE_CHECK',
        status: 'ERROR',
        details: 'Invalid X-Hub-Signature-256. Webhook payload rejected.',
      });
      res.sendStatus(401);
      return;
    }
  }

  const body = req.body as MetaWebhookEvent;

  if (body.object !== 'page') {
    res.sendStatus(404);
    return;
  }

  // Meta expects an immediate 200 OK
  res.status(200).send('EVENT_RECEIVED');

  // Process entries asynchronously
  for (const entry of body.entry || []) {
    for (const webhookEvent of entry.messaging || []) {
      const senderPsid = webhookEvent.sender?.id;
      const mid = webhookEvent.message?.mid;
      const messageText = webhookEvent.message?.text || webhookEvent.message?.quick_reply?.payload;

      // Handle unsupported event types gracefully
      if (webhookEvent.delivery || webhookEvent.read) {
        addLog({
          type: webhookEvent.delivery ? 'DELIVERY_RECEIPT' : 'READ_RECEIPT',
          senderId: senderPsid,
          status: 'UNSUPPORTED',
          details: 'Receipt acknowledged without action.',
        });
        continue;
      }

      if (!messageText || !senderPsid) {
        addLog({
          type: 'NON_TEXT_EVENT',
          senderId: senderPsid,
          status: 'UNSUPPORTED',
          details: 'Attachment or unsupported interaction type.',
        });
        continue;
      }

      // Deduplication check
      if (mid && deduplicationManager.isDuplicate(mid)) {
        console.log(`[Meta Webhook] Duplicate message mid=${mid} skipped.`);
        addLog({
          type: 'DUPLICATE_MID',
          senderId: senderPsid,
          mid,
          status: 'DUPLICATE',
          details: `Skipped idempotent duplicate MID ${mid}`,
        });
        continue;
      }

      try {
        // Send typing indicator
        await messengerApi.sendAction(senderPsid, 'typing_on');

        // Process message with AI agent
        const aiResult = await aiReplyAgent.processMessage(senderPsid, messageText, config.gemini.apiKey);

        // Turn off typing indicator
        await messengerApi.sendAction(senderPsid, 'typing_off');

        // Send reply back to customer
        await messengerApi.sendTextMessage(senderPsid, aiResult.replyText, aiResult.quickReplies);

        addLog({
          type: 'AI_REPLY_SENT',
          senderId: senderPsid,
          mid,
          status: 'SUCCESS',
          details: `Intent: ${aiResult.detectedIntent} | Escalation: ${aiResult.requiresHumanHandoff}`,
        });
      } catch (err: unknown) {
        const errorMsg = err instanceof Error ? err.message : String(err);
        console.error('[Meta Webhook Processing Error]:', errorMsg);
        addLog({
          type: 'PROCESSING_ERROR',
          senderId: senderPsid,
          mid,
          status: 'ERROR',
          details: errorMsg,
        });
      }
    }
  }
});
