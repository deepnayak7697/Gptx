import express from 'express';
import cors from 'cors';
import rateLimit from 'express-rate-limit';
import dotenv from 'dotenv';
import axios from 'axios';
import multer from 'multer';

dotenv.config();

const app = express();
const PORT = process.env.PORT || 8787;
const OPENAI_API_KEY = process.env.OPENAI_API_KEY;
const APP_KEY = process.env.APP_KEY;

if (!OPENAI_API_KEY || !APP_KEY) {
  console.error("❌ FATAL: Missing API keys in .env");
  process.exit(1);
}

const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 10 * 1024 * 1024 }
});

const limiter = rateLimit({ windowMs: 60000, max: 100 });
app.use(limiter);
app.use(cors());
app.use(express.json({ limit: '50mb' }));

const auth = (req, res, next) => {
  if (req.header('X-App-Key') !== APP_KEY) return res.status(401).json({ error: 'Unauthorized' });
  next();
};

app.get('/health', (req, res) => res.json({ ok: true, version: '2.0.0', features: ['chat', 'vision', 'files'] }));

app.post('/v1/chat', auth, async (req, res) => {
  const { messages, model = 'gpt-4o-mini', temperature = 0.7, max_tokens = 4096 } = req.body;
  if (!messages?.length) return res.status(400).json({ error: 'messages required' });

  res.writeHead(200, { 'Content-Type': 'text/event-stream', 'Cache-Control': 'no-cache', 'Connection': 'keep-alive' });

  try {
    const response = await axios.post('https://api.openai.com/v1/chat/completions',
      { model, messages, temperature, max_tokens, stream: true },
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

app.post('/v1/upload', auth, upload.single('file'), (req, res) => {
  if (!req.file) return res.status(400).json({ error: 'No file' });
  const base64 = req.file.buffer.toString('base64');
  res.json({ success: true, data: `data:${req.file.mimetype};base64,${base64}`, size: req.file.size });
});

app.post('/v1/vision', auth, async (req, res) => {
  const { imageUrl, prompt = 'Describe this image.' } = req.body;
  if (!imageUrl) return res.status(400).json({ error: 'imageUrl required' });
  try {
    const resp = await axios.post('https://api.openai.com/v1/chat/completions', {
      model: 'gpt-4o-mini',
      messages: [{ role: 'user', content: [{ type: 'text', text: prompt }, { type: 'image_url', image_url: { url: imageUrl } }] }],
      max_tokens: 1000
    }, { headers: { 'Authorization': `Bearer ${OPENAI_API_KEY}` } });
    res.json({ success: true, result: resp.data.choices[0].message.content });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.listen(PORT, '0.0.0.0', () => console.log(`🚀 GPTX v2.0 → http://0.0.0.0:${PORT}`));
