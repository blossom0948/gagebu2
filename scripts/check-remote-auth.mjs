// Uses only an explicitly supplied, temporary synthetic QA account. No admin keys.
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const credentialPath = process.argv[2];
if (!credentialPath) throw new Error('Pass the path to ephemeral QA credentials (never a real user account).');
const credentials = JSON.parse(await readFile(credentialPath, 'utf8'));
assert.ok(credentials.email.endsWith('@example.invalid'), 'Use a synthetic QA account only.');
const properties = await readFile(new URL('../gradle.properties', import.meta.url), 'utf8');
const property = (name) => properties.match(new RegExp(`^${name}=(.+)$`, 'm'))?.[1];
const base = property('SUPABASE_URL');
const key = property('SUPABASE_PUBLISHABLE_KEY');
assert.ok(base?.startsWith('https://') && key?.startsWith('sb_publishable_'));

async function request(path, body, token, method = 'POST') {
  const response = await fetch(`${base}/auth/v1/${path}`, {
    method, headers: { apikey: key, 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    ...(body ? { body: JSON.stringify(body) } : {}), signal: AbortSignal.timeout(20000), redirect: 'error',
  });
  const text = await response.text();
  return { status: response.status, body: text ? JSON.parse(text) : {} };
}

let activeToken;
try {
  const settings = await request('settings', null, null, 'GET');
  assert.equal(settings.status, 200); assert.equal(settings.body.external.email, true);
  assert.equal(settings.body.mailer_autoconfirm, false);
  console.log('PASS: email login enabled, email confirmation remains required');
  const bad = await request('token?grant_type=password', { email: credentials.email, password: 'not-the-qa-password' });
  assert.equal(bad.status, 400); assert.equal(bad.body.error_code, 'invalid_credentials');
  console.log('PASS: incorrect password rejected');
  const signed = await request('token?grant_type=password', credentials);
  assert.equal(signed.status, 200, 'Synthetic account login must succeed');
  assert.equal(signed.body.user.email, credentials.email);
  assert.ok(signed.body.access_token && signed.body.refresh_token);
  activeToken = signed.body.access_token;
  const user = await request('user', null, activeToken, 'GET');
  assert.equal(user.status, 200); assert.equal(user.body.id, signed.body.user.id);
  console.log('PASS: real password login and server-verified identity');
  const refreshed = await request('token?grant_type=refresh_token', { refresh_token: signed.body.refresh_token });
  assert.equal(refreshed.status, 200); assert.equal(refreshed.body.user.id, user.body.id);
  activeToken = refreshed.body.access_token;
  console.log('PASS: real session refresh retains identity');
  if (process.argv.includes('--worker')) {
    const worker = 'https://moasseum-ai-worker.blossom0948.workers.dev';
    const health = await (await fetch(`${worker}/health`, { signal: AbortSignal.timeout(10000) })).json();
    assert.equal(health.authRequired, true);
    const aggregate = { month: '2026-10', expenseTotal: 1000, incomeTotal: 0, budgetAmount: 10000, previousExpenseTotal: 0, categories: [{ categoryKey: 'OTHER', total: 1000, count: 1 }] };
    const checks = [
      ['/v1/parse-transaction', { text: 'QA검사상점에서 1000원 지출', today: '2026-10-05', timezone: 'Asia/Seoul' }],
      ['/v1/analyze-spending', aggregate],
      ['/v1/ask-spending', { ...aggregate, question: '이번 달 지출은 얼마인가요?' }],
      ['/v1/classify-notification', { title: 'QA테스트카드', text: 'QA테스트카드 승인 1,000원 QA검사상점 일시불' }],
    ];
    for (const [path, body] of checks) {
      for (const token of [null, 'synthetic-invalid-token']) {
        const denied = await fetch(`${worker}${path}`, { method: 'POST', headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) }, body: JSON.stringify(body), signal: AbortSignal.timeout(15000) });
        assert.equal(denied.status, 401, `${path} must reject missing/forged tokens`);
      }
      const accepted = await fetch(`${worker}${path}`, { method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${activeToken}` }, body: JSON.stringify(body), signal: AbortSignal.timeout(35000) });
      assert.equal(accepted.status, 200, `${path} authenticated request must succeed`);
      const result = await accepted.json(); assert.equal(result.schemaVersion, 1);
      if (path.endsWith('parse-transaction')) assert.equal(result.amount, 1000);
      console.log(`PASS: ${path}: no-token/forged token 401, real token 200`);
    }
  }
  const loggedOut = await request('logout?scope=local', {}, activeToken);
  assert.equal(loggedOut.status, 204); activeToken = null;
  const revoked = await request('token?grant_type=refresh_token', { refresh_token: refreshed.body.refresh_token });
  assert.equal(revoked.status, 400);
  console.log('PASS: logout revokes the refresh token');
  const unauthenticated = await request('user', null, null, 'GET');
  assert.equal(unauthenticated.status, 401);
  const forged = await request('token?grant_type=pkce', { auth_code: 'synthetic-invalid-code', code_verifier: 'a'.repeat(43) });
  assert.ok([400, 403, 404].includes(forged.status));
  console.log('PASS: no-token identity request and forged PKCE exchange rejected');
} finally {
  if (activeToken) await request('logout?scope=local', {}, activeToken).catch(() => {});
}
