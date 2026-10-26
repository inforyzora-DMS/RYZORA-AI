import express, { Request, Response } from 'express';
import { config } from './config.js';
import { webhookRouter } from './routes/webhook.js';
import { apiRouter } from './routes/api.js';

const app = express();

// Parse JSON while preserving raw body Buffer for HMAC-SHA256 signature verification
app.use(
  express.json({
    verify: (req: Request & { rawBody?: Buffer }, _res: Response, buf: Buffer) => {
      req.rawBody = buf;
    },
  })
);

app.use('/webhook', webhookRouter);
app.use('/api', apiRouter);

app.get('/health', (_req: Request, res: Response) => {
  res.json({
    status: 'healthy',
    service: 'RYZORA Facebook Messenger AI Agent',
    version: '1.0.0',
    time: new Date().toISOString(),
  });
});

if (process.env.NODE_ENV !== 'test') {
  app.listen(config.port, () => {
    console.log(`[RYZORA] Server is running on port ${config.port}`);
    console.log(`[RYZORA] Webhook URL: http://localhost:${config.port}/webhook`);
  });
}

export default app;
