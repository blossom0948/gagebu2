interface Env {
  GEMINI_API_KEY: string;
  GEMINI_MODEL?: string;
  REQUIRE_AUTH?: string;
  SUPABASE_URL?: string;
  SUPABASE_PUBLISHABLE_KEY?: string;
  ALLOWED_ORIGIN?: string;
}

const CATEGORY_KEYS = ["FOOD", "TRANSPORT", "SHOPPING", "LIVING", "HEALTH", "LEISURE", "OTHER"] as const;
const MAX_INPUT_LENGTH = 500;
const PUBLIC_REQUESTS_PER_MINUTE = 30;

const publicRequestBuckets = new Map<string, { startedAt: number; count: number }>();

type CategoryKey = (typeof CATEGORY_KEYS)[number];

type ParseRequest = {
  schemaVersion?: number;
  text?: string;
  today?: string;
  timezone?: string;
};

type GeminiResponse = {
  candidates?: Array<{ content?: { parts?: Array<{ text?: string }> } }>;
};

const corsHeaders = (request: Request, env: Env): Record<string, string> => ({
  "Access-Control-Allow-Origin": env.ALLOWED_ORIGIN || request.headers.get("Origin") || "*",
  "Access-Control-Allow-Headers": "Authorization, Content-Type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Cache-Control": "no-store",
});

const json = (request: Request, env: Env, body: unknown, status = 200): Response =>
  new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders(request, env), "Content-Type": "application/json; charset=utf-8" },
  });

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: corsHeaders(request, env) });
    }

    const url = new URL(request.url);
    if (url.pathname === "/health" && request.method === "GET") {
      return json(request, env, { ok: true, service: "moasseum-ai-worker", schemaVersion: 1 });
    }
    if (url.pathname !== "/v1/parse-transaction" || request.method !== "POST") {
      return json(request, env, { error: { code: "NOT_FOUND", message: "지원하지 않는 경로입니다." } }, 404);
    }

    if (!env.GEMINI_API_KEY) {
      return json(request, env, { error: { code: "AI_NOT_CONFIGURED", message: "AI 서버 secret이 설정되지 않았습니다." } }, 503);
    }

    if (env.REQUIRE_AUTH === "false" && !allowPublicRequest(request)) {
      return json(request, env, { error: { code: "RATE_LIMITED", message: "잠시 후 다시 시도해 주세요." } }, 429);
    }

    if (env.REQUIRE_AUTH !== "false") {
      const authorized = await verifySupabaseUser(request, env);
      if (!authorized) {
        return json(request, env, { error: { code: "UNAUTHORIZED", message: "로그인이 필요한 요청입니다." } }, 401);
      }
    }

    let input: ParseRequest;
    try {
      input = await request.json<ParseRequest>();
    } catch {
      return json(request, env, { error: { code: "INVALID_JSON", message: "요청 형식이 올바르지 않습니다." } }, 400);
    }

    const text = input.text?.trim() || "";
    if (!text || text.length > MAX_INPUT_LENGTH) {
      return json(request, env, { error: { code: "INVALID_INPUT", message: "기록 문장은 1~500자로 입력해 주세요." } }, 400);
    }

    const today = isIsoDate(input.today) ? input.today : new Date().toISOString().slice(0, 10);
    const model = env.GEMINI_MODEL || "gemini-3.5-flash-lite";
    const prompt = [
      "당신은 한국어 가계부 거래 구조화기입니다.",
      "사용자 문장을 해석해 JSON schema에 맞는 거래 후보 하나만 반환하세요.",
      "금액·날짜를 추측하지 말고, 모호하면 needsConfirmation에 필드명을 넣으세요.",
      `오늘 날짜: ${today}`,
      `사용자 시간대: ${input.timezone || "Asia/Seoul"}`,
      `허용 카테고리: ${CATEGORY_KEYS.join(", ")}`,
      `사용자 문장: ${text}`,
    ].join("\n");

    const geminiResponse = await fetch(
      `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(model)}:generateContent`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json", "x-goog-api-key": env.GEMINI_API_KEY },
        body: JSON.stringify({
          contents: [{ parts: [{ text: prompt }] }],
          generationConfig: {
            temperature: 0.1,
            responseMimeType: "application/json",
            responseSchema: responseSchema(),
          },
        }),
      },
    );

    if (!geminiResponse.ok) {
      return json(request, env, { error: { code: "AI_UPSTREAM_ERROR", message: "AI 서버가 잠시 응답하지 않습니다." } }, 502);
    }

    const upstream = (await geminiResponse.json()) as GeminiResponse;
    const rawText = upstream.candidates?.[0]?.content?.parts?.[0]?.text;
    if (!rawText) {
      return json(request, env, { error: { code: "AI_EMPTY_RESPONSE", message: "AI가 거래 후보를 만들지 못했습니다." } }, 502);
    }

    try {
      const candidate = validateCandidate(JSON.parse(stripCodeFence(rawText)), today);
      return json(request, env, candidate);
    } catch (error) {
      console.error("AI candidate validation failed", error instanceof Error ? error.message : "unknown error");
      return json(request, env, { error: { code: "AI_SCHEMA_ERROR", message: "AI 응답을 거래 후보로 검증하지 못했습니다." } }, 502);
    }
  },
} satisfies ExportedHandler<Env>;

function responseSchema() {
  return {
    type: "OBJECT",
    properties: {
      schemaVersion: { type: "INTEGER" },
      type: { type: "STRING", enum: ["EXPENSE", "INCOME"] },
      amount: { type: "INTEGER" },
      currency: { type: "STRING" },
      occurredDate: { type: "STRING" },
      categoryKey: { type: "STRING", enum: CATEGORY_KEYS },
      merchant: { type: "STRING" },
      memo: { type: "STRING" },
      confidence: {
        type: "OBJECT",
        properties: {
          amount: { type: "NUMBER" },
          date: { type: "NUMBER" },
          category: { type: "NUMBER" },
        },
        required: ["amount", "date", "category"],
      },
      needsConfirmation: { type: "ARRAY", items: { type: "STRING" } },
    },
    required: ["schemaVersion", "type", "amount", "currency", "occurredDate", "categoryKey", "merchant", "memo", "confidence", "needsConfirmation"],
  };
}

function validateCandidate(value: unknown, fallbackDate: string) {
  if (!value || typeof value !== "object") throw new Error("not object");
  const candidate = value as Record<string, unknown>;
  const amount = typeof candidate.amount === "number" && Number.isInteger(candidate.amount) ? candidate.amount : 0;
  const rawMerchant = typeof candidate.merchant === "string" ? candidate.merchant.trim().slice(0, 80) : "";
  const merchant = rawMerchant || "알 수 없음";
  const occurredDate = typeof candidate.occurredDate === "string" && isIsoDate(candidate.occurredDate) ? candidate.occurredDate : fallbackDate;
  const categoryKey = CATEGORY_KEYS.includes(candidate.categoryKey as CategoryKey) ? candidate.categoryKey : "OTHER";
  if (amount <= 0) throw new Error("invalid candidate");
  const needsConfirmation = Array.isArray(candidate.needsConfirmation)
    ? candidate.needsConfirmation.filter((item): item is string => typeof item === "string").slice(0, 5)
    : [];
  if (!rawMerchant && !needsConfirmation.includes("merchant")) needsConfirmation.push("merchant");
  return {
    schemaVersion: 1,
    type: candidate.type === "INCOME" ? "INCOME" : "EXPENSE",
    amount,
    currency: "KRW",
    occurredDate,
    categoryKey,
    merchant,
    memo: typeof candidate.memo === "string" ? candidate.memo.trim().slice(0, 160) : "",
    confidence: candidate.confidence || { amount: 0.7, date: 0.6, category: 0.6 },
    needsConfirmation,
  };
}

function isIsoDate(value: unknown): value is string {
  return typeof value === "string" && /^\d{4}-\d{2}-\d{2}$/.test(value);
}

function stripCodeFence(value: string): string {
  return value.trim().replace(/^```(?:json)?\s*/i, "").replace(/\s*```$/, "");
}

function allowPublicRequest(request: Request): boolean {
  const now = Date.now();
  const clientKey = request.headers.get("CF-Connecting-IP") || "unknown";
  const existing = publicRequestBuckets.get(clientKey);
  if (!existing || now - existing.startedAt >= 60_000) {
    publicRequestBuckets.set(clientKey, { startedAt: now, count: 1 });
    return true;
  }
  if (existing.count >= PUBLIC_REQUESTS_PER_MINUTE) return false;
  existing.count += 1;
  return true;
}

async function verifySupabaseUser(request: Request, env: Env): Promise<boolean> {
  const authorization = request.headers.get("Authorization");
  if (!authorization?.startsWith("Bearer ") || !env.SUPABASE_URL || !env.SUPABASE_PUBLISHABLE_KEY) return false;
  const response = await fetch(`${env.SUPABASE_URL.replace(/\/$/, "")}/auth/v1/user`, {
    headers: { Authorization: authorization, apikey: env.SUPABASE_PUBLISHABLE_KEY },
  });
  return response.ok;
}
