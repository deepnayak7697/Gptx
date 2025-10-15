import express from 'express';
import cors from 'cors';
import rateLimit from 'express-rate-limit';
import dotenv from 'dotenv';
import axios from 'axios';

dotenv.config();

const app = express();
const PORT = process.env.PORT || 8787;
const OPENAI_API_KEY = process.env.OPENAI_API_KEY;
const APP_KEY = process.env.APP_KEY;

if (!OPENAI_API_KEY || !APP_KEY) {
  console.error("FATAL: OPENAI_API_KEY and APP_KEY must be set in .env file");
  process.exit(1);
}

const limiter = rateLimit({ windowMs: 60000, max: 60, standardHeaders: true, legacyHeaders: false });
app.use(limiter);
app.use(cors());
app.use(express.json());

app.get('/health', (req, res) => res.json({ ok: true }));

app.post('/v1/chat', async (req, res) => {
  const receivedAppKey = req.header('X-App-Key');
  if (receivedAppKey !== APP_KEY) return res.status(401).json({ error: 'Unauthorized' });
  
  const { messages, model = 'gpt-4o-mini' } = req.body;
  if (!messages?.length) return res.status(400).json({ error: 'messages required' });

  res.writeHead(200, { 'Content-Type': 'text/event-stream', 'Cache-Control': 'no-cache', 'Connection': 'keep-alive' });

  try {
    const response = await axios.post('https://api.openai.com/v1/chat/completions',
      { model, messages, stream: true },
      { responseType: 'stream', headers: { 'Authorization': `Bearer ${OPENAI_API_KEY}` } }
    );
    response.data.on('data', chunk => {
      chunk.toString().split('\n').filter(l => l.trim()).forEach(line => {
        if (line.includes('[DONE]')) { res.write('data: [DONE]\n\n'); res.end(); }
        else if (line.startsWith('data: ')) res.write(`${line}\n\n`);
      });
    });
    response.data.on('error', () => res.end());
  } catch (err) {
    res.write(`event: error\ndata: ${JSON.stringify({ error: err.message })}\n\n`);
    res.end();
  }
});

app.listen(PORT, '0.0.0.0', () => console.log(`🚀 GPTX Backend running on http://0.0.0.0:${PORT}`));
