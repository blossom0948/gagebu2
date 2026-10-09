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
    for (const path of ['/v1/parse-transaction', '/v1/parse-batch-command', '/v1/analyze-spending', '/v1/ask-spending', '/v1/classify-notification']) {
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
test('verified user receives multiple validated candidates for explicit additions', async () => {
  let calls = 0;
  await mockFetch(async (url, options) => {
    calls++;
    if (String(url).startsWith(env.SUPABASE_URL)) return Response.json({ id: userId });
    const body = JSON.parse(String(options?.body));
    assert.match(body.contents[0].parts[0].text, /사용자 문장: 점심 8천원 그리고 월급 250만원/);
    const schema = body.generationConfig.responseSchema;
    assert.equal(schema.properties.action.enum.includes('UNSUPPORTED'), true);
    assert.equal(schema.properties.candidates.items.properties.type.enum.includes('INCOME'), true);
    return Response.json({ candidates: [{ content: { parts: [{ text: JSON.stringify({
      schemaVersion: 1,
      action: 'ADD',
      candidates: [
        { type: 'EXPENSE', amount: 8000, occurredDate: '2026-10-08', categoryKey: 'FOOD', merchant: '점심', memo: '', confidence: { amount: 0.9, date: 0.9, category: 0.8 }, needsConfirmation: [] },
        { type: 'INCOME', amount: 2500000, occurredDate: '2026-10-08', categoryKey: 'OTHER', merchant: '월급', memo: '', confidence: { amount: 0.9, date: 0.9, category: 0.8 }, needsConfirmation: [] },
      ],
      target: { type: 'ANY', amount: 0, categoryKey: '', merchantTokens: [], from: '', through: '' },
      edit: { amount: 0, merchant: '', categoryKey: '', type: 'NONE', occurredDate: '', memo: '' },
    }) }] } }] });
  }, async () => {
    const response = await worker.fetch(request('/v1/parse-batch-command', 'synthetic-token', {
      text: '점심 8천원 그리고 월급 250만원', today: '2026-10-08', timezone: 'Asia/Seoul',
    }), env);
    assert.equal(response.status, 200);
    const body = await response.json() as { candidates: Array<{ amount: number; type: string }> };
    assert.deepEqual(body.candidates.map(({ amount, type }) => [amount, type]), [[8000, 'EXPENSE'], [2500000, 'INCOME']]);
    assert.equal(calls, 2);
  });
});
test('batch AI returns only a narrow delete query plan and never sees transaction rows', async () => {
  const syntheticPlan = {
    schemaVersion: 1,
    action: 'DELETE',
    candidates: [],
    target: { type: 'EXPENSE', amount: 0, categoryKey: 'CAFE', merchantTokens: ['카페'], from: '2026-10-01', through: '2026-10-31' },
    edit: { amount: 0, merchant: '', categoryKey: '', type: 'NONE', occurredDate: '', memo: '' },
  };
  await mockFetch(async (url) => {
    if (String(url).startsWith(env.SUPABASE_URL)) return Response.json({ id: userId });
    assert.ok(String(url).startsWith('https://generativelanguage.googleapis.com/'));
    return Response.json({ candidates: [{ content: { parts: [{ text: JSON.stringify(syntheticPlan) }] } }] });
  }, async () => {
    const response = await worker.fetch(request('/v1/parse-batch-command', 'synthetic-token', {
      text: '이번 달 카페 지출 중 카페 거래를 골라 삭제해', today: '2026-10-08',
    }), env);
    assert.equal(response.status, 200);
    const body = await response.json() as { action: string; target: { merchantTokens: string[] }; edit: unknown };
    assert.equal(body.action, 'DELETE');
    assert.deepEqual(body.target.merchantTokens, ['카페']);
    assert.equal(body.edit, null);
  });
});
test('batch AI declines ambiguous or unsupported requests', async () => {
  await mockFetch(async (url) => {
    if (String(url).startsWith(env.SUPABASE_URL)) return Response.json({ id: userId });
    return Response.json({ candidates: [{ content: { parts: [{ text: JSON.stringify({
      schemaVersion: 1, action: 'UNSUPPORTED', candidates: [],
      target: { type: 'ANY', amount: 0, categoryKey: '', merchantTokens: [], from: '', through: '' },
      edit: { amount: 0, merchant: '', categoryKey: '', type: 'NONE', occurredDate: '', memo: '' },
    }) }] } }] });
  }, async () => {
    const response = await worker.fetch(request('/v1/parse-batch-command', 'synthetic-token', {
      text: '다음 달 예산을 바꿔줘', today: '2026-10-08',
    }), env);
    assert.equal(response.status, 422);
    assert.equal((await response.json() as { error: { code: string } }).error.code, 'UNSUPPORTED_COMMAND');
  });
});
test('batch AI rejects an unbounded edit target', async () => {
  await mockFetch(async (url) => {
    if (String(url).startsWith(env.SUPABASE_URL)) return Response.json({ id: userId });
    return Response.json({ candidates: [{ content: { parts: [{ text: JSON.stringify({
      schemaVersion: 1, action: 'DELETE', candidates: [],
      target: { type: 'EXPENSE', amount: 0, categoryKey: '', merchantTokens: [], from: '', through: '' }, edit: null,
    }) }] } }] });
  }, async () => {
    const response = await worker.fetch(request('/v1/parse-batch-command', 'synthetic-token', {
      text: '모든 지출 삭제', today: '2026-10-08',
    }), env);
    assert.equal(response.status, 502);
    assert.equal((await response.json() as { error: { code: string } }).error.code, 'AI_SCHEMA_ERROR');
  });
});
test('batch AI rejects any mutation values attached to a delete plan', async () => {
  await mockFetch(async (url) => {
    if (String(url).startsWith(env.SUPABASE_URL)) return Response.json({ id: userId });
    return Response.json({ candidates: [{ content: { parts: [{ text: JSON.stringify({
      schemaVersion: 1, action: 'DELETE', candidates: [],
      target: { type: 'ANY', amount: 0, categoryKey: 'CAFE', merchantTokens: [], from: '', through: '' },
      edit: { amount: 9000, merchant: '', categoryKey: '', type: 'NONE', occurredDate: '', memo: '' },
    }) }] } }] });
  }, async () => {
    const response = await worker.fetch(request('/v1/parse-batch-command', 'synthetic-token', {
      text: '카페 거래 삭제', today: '2026-10-08',
    }), env);
    assert.equal(response.status, 502);
    assert.equal((await response.json() as { error: { code: string } }).error.code, 'AI_SCHEMA_ERROR');
  });
});
test('batch AI returns a validated update patch without deciding which transaction to mutate', async () => {
  const updatePlan = {
    schemaVersion: 1,
    action: 'UPDATE',
    candidates: [],
    target: { type: 'ANY', amount: 0, categoryKey: 'CAFE', merchantTokens: ['스타벅스'], from: '', through: '' },
    edit: { amount: 5000, merchant: '', categoryKey: '', type: 'NONE', occurredDate: '', memo: '' },
  };
  await mockFetch(async (url, options) => {
    if (String(url).startsWith(env.SUPABASE_URL)) return Response.json({ id: userId });
    const body = JSON.parse(String(options?.body));
    const prompt = body.contents[0].parts[0].text as string;
    assert.match(prompt, /거래 원장 데이터는 제공되지 않으므로/);
    assert.ok(!prompt.includes('LOCAL_PRIVATE_ROW_MARKER'));
    return Response.json({ candidates: [{ content: { parts: [{ text: JSON.stringify(updatePlan) }] } }] });
  }, async () => {
    const response = await worker.fetch(request('/v1/parse-batch-command', 'synthetic-token', {
      text: '스타벅스 커피 금액을 5천원으로 수정해', today: '2026-10-08',
    }), env);
    assert.equal(response.status, 200);
    const body = await response.json() as { action: string; target: { merchantTokens: string[] }; edit: { amount: number } };
    assert.equal(body.action, 'UPDATE');
    assert.deepEqual(body.target.merchantTokens, ['스타벅스']);
    assert.equal(body.edit.amount, 5000);
  });
});
test('batch AI rejects invalid values and malformed candidate counts', async () => {
  await mockFetch(async (url) => {
    if (String(url).startsWith(env.SUPABASE_URL)) return Response.json({ id: userId });
    return Response.json({ candidates: [{ content: { parts: [{ text: JSON.stringify({ schemaVersion: 1, action: 'ADD', candidates: [
      { type: 'EXPENSE', amount: 1_000_000_000_001, occurredDate: '2026-10-08', categoryKey: 'FOOD', merchant: '점심' },
    ] }) }] } }] });
  }, async () => {
    const response = await worker.fetch(request('/v1/parse-batch-command', 'synthetic-token', {
      text: '점심 8천원', today: '2026-10-08',
    }), env);
    assert.equal(response.status, 502);
    assert.equal((await response.json() as { error: { code: string } }).error.code, 'AI_SCHEMA_ERROR');
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
