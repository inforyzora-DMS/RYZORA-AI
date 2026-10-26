import crypto from 'node:crypto';

/**
 * Validates Meta Webhook HMAC-SHA256 signature.
 * Header format: sha256=abcdef123456...
 */
export function verifyMetaSignature(
  rawBody: Buffer | string,
  signatureHeader: string | undefined,
  appSecret: string
): boolean {
  if (!signatureHeader || !appSecret) {
    return false;
  }

  const [prefix, expectedHash] = signatureHeader.split('=');
  if (prefix !== 'sha256' || !expectedHash) {
    return false;
  }

  const hmac = crypto.createHmac('sha256', appSecret);
  hmac.update(rawBody);
  const actualHash = hmac.digest('hex');

  const expectedBuffer = Buffer.from(expectedHash, 'hex');
  const actualBuffer = Buffer.from(actualHash, 'hex');

  if (expectedBuffer.length !== actualBuffer.length) {
    return false;
  }

  return crypto.timingSafeEqual(expectedBuffer, actualBuffer);
}
