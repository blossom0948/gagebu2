import { test } from 'node:test';
import assert from 'node:assert/strict';
import worker from '../src/index.ts';

const env = { GEMINI_API_KEY: 'synthetic-not-a-real-key', REQUIRE_AUTH: 'true', SUPABASE_URL: 'https://qa.supabase.invalid', SUPABASE_PUBLISHABLE_KEY: 'sb_publishable_synthetic' };
const userId = '72a43f9d-00e0-44db-af34-4cf143314bb7';
const request = (path = '/v1/parse-transaction', bearer?: string, body: unknown = {}) => new Request(`https://qa.worker.invalid${path}`, { method: 'POST', headers: { 'Content-Type': 'application/json', ...(bearer ? { Authorization: `Bearer ${bearer}` } : {}) }, body: JSON.stringify(body) });
async function mockFetch(handler: typeof fetch, run: () => Promise<void>) {
  const original = globalThis.fetch;
  globalThis.fetch = handler;
  try { await run(); } finally { globalThis.fetch = original; }
}

test('all AI routes reject missing tokens without contacting upstream', async () => {
  await mockFetch(async () => { throw new Error('No upstream call permitted'); }, async () => {
    for (const path of ['/v1/parse-transaction', '/v1/analyze-spending', '/v1/ask-spending', '/v1/classify-notification']) {
      const response = await worker.fetch(request(path), env);
      assert.equal(response.status, 401);
    }
  });
});
test('invalid token is checked by Supabase and never reaches Gemini', async () => {
  await mockFetch(async (url, options) => {
    assert.equal(String(url), `${env.SUPABASE_URL}/auth/v1/user`);
    assert.equal(options?.redirect, 'manual');
    assert.equal(new Headers(options?.headers).get('Authorization'), 'Bearer invalid');
    return Response.json({ error: 'synthetic' }, { status: 401 });
  }, async () => { assert.equal((await worker.fetch(request(undefined, 'invalid'), env)).status, 401); });
});
test('Supabase outage yields safe 503 rather than exception or authorization', async () => {
  await mockFetch(async () => { throw new Error('SYNTHETIC_SECRET must not be echoed'); }, async () => {
    const response = await worker.fetch(request(undefined, 'synthetic-token'), env);
    assert.equal(response.status, 503); assert.ok(!(await response.text()).includes('SYNTHETIC_SECRET'));
  });
});
test('successful HTTP without valid user identity does not authorize', async () => {
  await mockFetch(async () => Response.json({}), async () => { assert.equal((await worker.fetch(request(undefined, 'synthetic-token'), env)).status, 401); });
});
test('auth redirects are never followed and do not authorize', async () => {
  await mockFetch(async (_url, options) => {
    assert.equal(options?.redirect, 'manual');
    return new Response(null, { status: 302, headers: { Location: 'https://different.invalid' } });
  }, async () => { assert.equal((await worker.fetch(request(undefined, 'synthetic-token'), env)).status, 503); });
});
test('verified user reaches input validation, not an unauthenticated bypass', async () => {
  await mockFetch(async () => Response.json({ id: userId }), async () => { assert.equal((await worker.fetch(request(undefined, 'synthetic-token'), env)).status, 400); });
});
test('verified user can receive a validated synthetic Gemini candidate', async () => {
  let calls = 0;
  await mockFetch(async (url) => {
    calls++;
    if (String(url).startsWith(env.SUPABASE_URL)) return Response.json({ id: userId });
    assert.ok(String(url).startsWith('https://generativelanguage.googleapis.com/'));
    return Response.json({ candidates: [{ content: { parts: [{ text: JSON.stringify({ type: 'EXPENSE', amount: 1000, merchant: 'QA', occurredDate: '2026-10-05', categoryKey: 'OTHER' }) }] } }] });
  }, async () => {
    const response = await worker.fetch(request(undefined, 'synthetic-token', { text: 'QA 1000원', today: '2026-10-05' }), env);
    assert.equal(response.status, 200); assert.equal((await response.json()).amount, 1000); assert.equal(calls, 2);
  });
});
test('authenticated traffic is also rate limited', async () => {
  await mockFetch(async () => Response.json({ id: '82a43f9d-00e0-44db-af34-4cf143314bb7' }), async () => {
    for (let index = 0; index < 30; index++) assert.equal((await worker.fetch(request(undefined, 'synthetic-token'), env)).status, 400);
    assert.equal((await worker.fetch(request(undefined, 'synthetic-token'), env)).status, 429);
  });
});
test('health is public and clearly reports authentication requirement', async () => {
  const response = await worker.fetch(new Request('https://qa.worker.invalid/health'), env);
  assert.equal(response.status, 200); assert.equal((await response.json()).authRequired, true);
});
