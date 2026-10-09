import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import ts from 'typescript';
import { Miniflare, convertV4MiniflareOptions } from 'miniflare';

test('actual Workerd runtime supports authenticated fetch without forwarding redirects', async () => {
  const source = await readFile(new URL('../src/index.ts', import.meta.url), 'utf8');
  const script = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2022 } }).outputText;
  let authStatus = 200, modelCalls = 0;
  const mf = new Miniflare(convertV4MiniflareOptions({
    modules: true, script, compatibilityDate: '2026-09-15', compatibilityFlags: ['nodejs_compat'],
    bindings: { GEMINI_API_KEY: 'synthetic', REQUIRE_AUTH: 'true', SUPABASE_URL: 'https://qa.supabase.invalid', SUPABASE_PUBLISHABLE_KEY: 'sb_publishable_synthetic' },
    outboundService: async (request) => {
      if (request.url.startsWith('https://qa.supabase.invalid')) {
        if (authStatus === 302) return new Response(null, { status: 302, headers: { Location: 'https://must-not-receive-token.invalid' } });
        return Response.json({ id: '72a43f9d-00e0-44db-af34-4cf143314bb7' }, { status: authStatus });
      }
      assert.ok(request.url.startsWith('https://generativelanguage.googleapis.com/'));
      modelCalls++;
      if (request.body && new URL(request.url).searchParams.get('key')) {
        // API keys are sent only as headers; this branch intentionally does not inspect secrets.
      }
      const requestBody = await request.clone().json().catch(() => ({}));
      if (new URL(request.url).pathname.includes('generateContent') &&
          requestBody.generationConfig?.responseSchema?.properties?.action) {
        return Response.json({ candidates: [{ content: { parts: [{ text: JSON.stringify({ schemaVersion: 1, action: 'ADD', candidates: [
          { type: 'EXPENSE', amount: 1200, occurredDate: '2026-10-05', categoryKey: 'FOOD', merchant: 'QA 점심', memo: '', confidence: { amount: 0.9, date: 0.8, category: 0.7 }, needsConfirmation: [] },
        ], target: { type: 'ANY', amount: 0, categoryKey: '', merchantTokens: [], from: '', through: '' },
        edit: { amount: 0, merchant: '', categoryKey: '', type: 'NONE', occurredDate: '', memo: '' } }) }] } }] });
      }
      return Response.json({ candidates: [{ content: { parts: [{ text: JSON.stringify({ amount: 1000, merchant: 'QA', type: 'EXPENSE' }) }] } }] });
    },
  }));
  const options = (token) => ({ method: 'POST', headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: 'Bearer synthetic' } : {}) }, body: JSON.stringify({ text: 'QA 1000원', today: '2026-10-05' }) });
  try {
    assert.equal((await mf.dispatchFetch('https://qa.worker.invalid/v1/parse-transaction', options(false))).status, 401);
    const accepted = await mf.dispatchFetch('https://qa.worker.invalid/v1/parse-transaction', options(true));
    assert.equal(accepted.status, 200); assert.equal((await accepted.json()).amount, 1000); assert.equal(modelCalls, 1);
    const batch = await mf.dispatchFetch('https://qa.worker.invalid/v1/parse-batch-command', {
      ...options(true), body: JSON.stringify({ text: '점심 1200원', today: '2026-10-05', timezone: 'Asia/Seoul' }),
    });
    assert.equal(batch.status, 200); assert.equal((await batch.json()).candidates[0].amount, 1200); assert.equal(modelCalls, 2);
    authStatus = 403;
    assert.equal((await mf.dispatchFetch('https://qa.worker.invalid/v1/parse-transaction', options(true))).status, 401);
    authStatus = 302;
    assert.equal((await mf.dispatchFetch('https://qa.worker.invalid/v1/parse-transaction', options(true))).status, 503);
    assert.equal(modelCalls, 2);
  } finally { await mf.dispose(); }
});
