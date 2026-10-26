import { config } from '../config.js';

export interface MessengerSendResponse {
  recipient_id?: string;
  message_id?: string;
  error?: {
    message: string;
    type: string;
    code: number;
    fbtrace_id: string;
  };
}

export class MessengerApiClient {
  private baseUrl: string;

  constructor() {
    this.baseUrl = `https://graph.facebook.com/${config.meta.apiVersion}/me/messages`;
  }

  /**
   * Sends text and optional quick reply buttons to a customer via Meta Graph API.
   */
  public async sendTextMessage(
    recipientId: string,
    text: string,
    quickReplies?: Array<{ title: string; payload: string }>
  ): Promise<{ success: boolean; messageId?: string; error?: string }> {
    const token = config.meta.pageAccessToken;

    if (!token) {
      // In development or when credentials are not yet configured, log accurately
      console.warn(`[Messenger API] META_PAGE_ACCESS_TOKEN not set. Simulating delivery to ${recipientId}.`);
      return { success: true, messageId: `mock_mid_${Date.now()}` };
    }

    const payload: Record<string, unknown> = {
      recipient: { id: recipientId },
      message: {
        text,
        quick_replies: quickReplies?.map((qr) => ({
          content_type: 'text',
          title: qr.title,
          payload: qr.payload,
        })),
      },
      messaging_type: 'RESPONSE',
    };

    try {
      const response = await fetch(`${this.baseUrl}?access_token=${encodeURIComponent(token)}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });

      const data = (await response.json()) as MessengerSendResponse;

      if (!response.ok || data.error) {
        console.error('[Messenger API Error]:', data.error);
        return {
          success: false,
          error: data.error?.message || `HTTP ${response.status}`,
        };
      }

      return {
        success: true,
        messageId: data.message_id,
      };
    } catch (err: unknown) {
      const errMsg = err instanceof Error ? err.message : String(err);
      console.error('[Messenger API Network Exception]:', errMsg);
      return { success: false, error: errMsg };
    }
  }

  /**
   * Sends typing bubble indicator or mark_seen event.
   */
  public async sendAction(
    recipientId: string,
    action: 'typing_on' | 'typing_off' | 'mark_seen'
  ): Promise<boolean> {
    const token = config.meta.pageAccessToken;
    if (!token) return true;

    try {
      await fetch(`${this.baseUrl}?access_token=${encodeURIComponent(token)}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          recipient: { id: recipientId },
          sender_action: action,
        }),
      });
      return true;
    } catch {
      return false;
    }
  }
}

export const messengerApi = new MessengerApiClient();
