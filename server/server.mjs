import http from "node:http";

const PORT = Number(process.env.PORT || 8787);
const RESPONSES_URL = "https://api.openai.com/v1/responses";
const MODEL = "gpt-5-mini";

const server = http.createServer(async (request, response) => {
  setCorsHeaders(response);

  if (request.method === "OPTIONS") {
    response.writeHead(204);
    response.end();
    return;
  }

  if (request.method === "GET" && request.url === "/health") {
    sendJson(response, 200, { ok: true, service: "Duddy Português AI Coach" });
    return;
  }

  if (request.method === "POST" && request.url === "/coach") {
    await handleCoachRequest(request, response);
    return;
  }

  sendJson(response, 404, { error: "Not found" });
});

server.listen(PORT, "0.0.0.0", () => {
  console.log(`Duddy Português AI server listening on http://0.0.0.0:${PORT}`);
});

async function handleCoachRequest(request, response) {
  const apiKey = process.env.OPENAI_API_KEY;
  if (!apiKey) {
    sendJson(response, 500, {
      error: "OPENAI_API_KEY is not set on the server."
    });
    return;
  }

  try {
    const body = await readJson(request);
    const prompt = buildCoachPrompt(body);

    const openAiResponse = await fetch(RESPONSES_URL, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${apiKey}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        model: MODEL,
        store: false,
        max_output_tokens: 220,
        instructions:
          "You are Duddy Português, a warm Brazilian Portuguese tutor for an English-speaking learner. Use proven language-learning practice: active recall, immediate corrective feedback, production practice, and one next repetition. Do not invent acoustic details; judge only the provided speech transcript.",
        input: prompt
      })
    });

    const resultText = await openAiResponse.text();
    if (!openAiResponse.ok) {
      sendJson(response, openAiResponse.status, {
        error: extractOpenAiError(resultText)
      });
      return;
    }

    sendJson(response, 200, {
      feedback: extractOutputText(resultText)
    });
  } catch (error) {
    sendJson(response, 400, {
      error: error instanceof Error ? error.message : "Invalid coach request."
    });
  }
}

function buildCoachPrompt(body) {
  const targetPhrase = requireString(body, "targetPhrase");
  const english = requireString(body, "english");
  const pronunciationGuide = requireString(body, "pronunciationGuide");
  const category = requireString(body, "category");
  const spokenText = requireString(body, "spokenText");

  return `
Target phrase: ${targetPhrase}
English meaning: ${english}
Pronunciation guide: ${pronunciationGuide}
Category: ${category}
User speech transcript: ${spokenText}

Give concise Portuguese tutor feedback. Mention whether the transcript is close to the target phrase, one specific pronunciation or word-order note, and one short encouragement. Keep it under 80 words.

Format:
Score: 0-100
Fix: one specific correction
Model: the target phrase
Next rep: one short instruction for the learner to say it again
`.trim();
}

function requireString(body, key) {
  const value = body?.[key];
  if (typeof value !== "string" || value.trim().length === 0) {
    throw new Error(`Missing required field: ${key}`);
  }
  return value.trim();
}

function extractOutputText(resultText) {
  const result = JSON.parse(resultText);
  if (typeof result.output_text === "string" && result.output_text.trim()) {
    return result.output_text.trim();
  }

  const text = [];
  for (const outputItem of result.output || []) {
    for (const contentItem of outputItem.content || []) {
      if (typeof contentItem.text === "string" && contentItem.text.trim()) {
        text.push(contentItem.text.trim());
      }
    }
  }

  return text.join("\n\n") || "No feedback text was returned. Try again in a moment.";
}

function extractOpenAiError(resultText) {
  try {
    const parsed = JSON.parse(resultText);
    return parsed?.error?.message || resultText.slice(0, 240);
  } catch {
    return resultText.slice(0, 240) || "OpenAI request failed.";
  }
}

function readJson(request) {
  return new Promise((resolve, reject) => {
    let body = "";

    request.on("data", (chunk) => {
      body += chunk;
      if (body.length > 32_000) {
        request.destroy();
        reject(new Error("Request body is too large."));
      }
    });

    request.on("end", () => {
      try {
        resolve(JSON.parse(body || "{}"));
      } catch {
        reject(new Error("Request body must be valid JSON."));
      }
    });

    request.on("error", reject);
  });
}

function sendJson(response, statusCode, payload) {
  response.writeHead(statusCode, { "Content-Type": "application/json" });
  response.end(JSON.stringify(payload));
}

function setCorsHeaders(response) {
  response.setHeader("Access-Control-Allow-Origin", "*");
  response.setHeader("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
  response.setHeader("Access-Control-Allow-Headers", "Content-Type");
}
