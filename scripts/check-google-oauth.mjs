// Provider/redirect diagnostics only. Never signs in or prints OAuth state/tokens.
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { randomBytes, createHash } from 'node:crypto';

const properties = await readFile(new URL('../gradle.properties', import.meta.url), 'utf8');
const property = name => properties.match(new RegExp(`^${name}=(.+)$`, 'm'))?.[1];
const base = property('SUPABASE_URL');
const key = property('SUPABASE_PUBLISHABLE_KEY');
assert.ok(base?.startsWith('https://') && key?.startsWith('sb_publishable_'));
const settingsResponse = await fetch(`${base}/auth/v1/settings`, { headers: { apikey: key }, redirect: 'error', signal: AbortSignal.timeout(20000) });
assert.equal(settingsResponse.status, 200);
const settings = await settingsResponse.json();
assert.equal(settings.external.google, true);
assert.equal(settings.external.email, true);
assert.equal(settings.mailer_autoconfirm, false);
console.log('PASS: Google/email enabled; email confirmation remains enabled');

for (const packageName of ['com.moasseum.app', 'com.moasseum.app.qa']) {
  const verifier = randomBytes(32).toString('base64url');
  const challenge = createHash('sha256').update(verifier).digest('base64url');
  const callback = `${packageName}://auth/callback?flow_id=${randomBytes(32).toString('base64url')}`;
  const authorize = new URL(`${base}/auth/v1/authorize`);
  authorize.search = new URLSearchParams({ provider: 'google', redirect_to: callback, code_challenge: challenge, code_challenge_method: 's256', scopes: 'openid email profile', prompt: 'select_account' });
  const response = await fetch(authorize, { redirect: 'manual', signal: AbortSignal.timeout(20000) });
  assert.ok([302, 303, 307].includes(response.status), 'Expected redirect to Google, not an OAuth setup error');
  const google = new URL(response.headers.get('location'));
  assert.equal(google.protocol, 'https:'); assert.equal(google.hostname, 'accounts.google.com');
  assert.equal(google.searchParams.get('redirect_uri'), `${base}/auth/v1/callback`);
  assert.equal(google.searchParams.get('response_type'), 'code');
  assert.equal(google.searchParams.get('prompt'), 'select_account');
  assert.ok(google.searchParams.get('state'));
  assert.match(google.searchParams.get('client_id'), /^[0-9]+-[a-z0-9]+\.apps\.googleusercontent\.com$/);
  if (process.argv[2]) assert.equal(google.searchParams.get('client_id'), process.argv[2]);
  const scopes = google.searchParams.get('scope').split(/\s+/);
  const allowed = new Set(['openid', 'email', 'profile', 'https://www.googleapis.com/auth/userinfo.email', 'https://www.googleapis.com/auth/userinfo.profile']);
  assert.ok(scopes.includes('openid')); assert.ok(scopes.every(scope => allowed.has(scope)), 'Only basic profile/email scopes are allowed');
  console.log(`PASS: ${packageName}: PKCE initiation redirects to configured Google client with only basic scopes`);
}
console.log('NOT TESTED: user consent, actual Google login, Android callback/session and installation (requires phone/user interaction).');
