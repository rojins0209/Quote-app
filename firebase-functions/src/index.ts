import * as admin from "firebase-admin";
import { onRequest } from "firebase-functions/v2/https";

admin.initializeApp();

const db = admin.firestore();

const ownerChatId = process.env.OWNER_TELEGRAM_CHAT_ID;
const telegramBotToken = process.env.TELEGRAM_BOT_TOKEN;
const dateRegex = /^\d{4}-\d{2}-\d{2}$/;
const monthRegex = /^\d{4}-\d{2}$/;
const maxQuoteLength = 600;

type TelegramUpdate = {
  message?: {
    chat: { id: number };
    text?: string;
  };
};

type QuotePayload = {
  date: string;
  quoteText: string;
  accentLine: string;
  author: string;
  force: boolean;
};

async function sendTelegramReply(chatId: number, text: string): Promise<void> {
  if (!telegramBotToken) return;

  const response = await fetch(`https://api.telegram.org/bot${telegramBotToken}/sendMessage`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ chat_id: chatId, text })
  });

  if (!response.ok) {
    throw new Error(`Failed sending Telegram message (${response.status})`);
  }
}

function parseAddPayload(rawText: string): QuotePayload | null {
  const force = rawText.startsWith("/add --force");
  const cleaned = force ? rawText.replace("/add --force", "/add") : rawText;

  const payload = cleaned.replace("/add", "").trim();
  const [date, quoteText, accentLine, author] = payload.split("|").map((item) => item.trim());

  if (!date || !quoteText || !dateRegex.test(date)) return null;
  if (quoteText.length > maxQuoteLength) return null;

  return {
    date,
    quoteText,
    accentLine: accentLine || "every day matters",
    author: author || "",
    force
  };
}

function parseDateCommand(rawText: string, command: string): string | null {
  const date = rawText.replace(command, "").trim();
  if (!dateRegex.test(date)) return null;
  return date;
}

function parseMonthForList(rawText: string): string | null {
  const val = rawText.replace("/list", "").trim();
  if (!val) return null;
  return monthRegex.test(val) ? val : null;
}

async function upsertQuote(parsed: QuotePayload, source: "telegram" | "telegram-update") {
  await db.collection("quotes").doc(parsed.date).set({
    text: parsed.quoteText,
    accentLine: parsed.accentLine,
    author: parsed.author,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    source
  }, { merge: true });
}

function getHelpText(): string {
  return [
    "Commands:",
    "/add YYYY-MM-DD | quote text | accent(optional) | author(optional)",
    "/add --force YYYY-MM-DD | quote text | accent(optional) | author(optional)",
    "/update YYYY-MM-DD | quote text | accent(optional) | author(optional)",
    "/preview YYYY-MM-DD",
    "/list [YYYY-MM]",
    "/stats",
    "/delete YYYY-MM-DD",
    "/help"
  ].join("\n");
}

export const telegramWebhook = onRequest(async (req, res) => {
  const update = req.body as TelegramUpdate;
  const message = update.message;

  if (!ownerChatId || !telegramBotToken) {
    res.status(500).send("Missing OWNER_TELEGRAM_CHAT_ID or TELEGRAM_BOT_TOKEN env vars");
    return;
  }

  if (!message?.text) {
    res.status(200).send("ignored");
    return;
  }

  if (String(message.chat.id) !== ownerChatId) {
    res.status(403).send("forbidden");
    return;
  }

  const text = message.text.trim();

  try {
    if (text.startsWith("/add")) {
      const parsed = parseAddPayload(text);
      if (!parsed) {
        await sendTelegramReply(message.chat.id, "Invalid format/length. Use /add YYYY-MM-DD | quote text | accent(optional) | author(optional). Quote max length: 600");
        res.status(200).send("invalid-add");
        return;
      }

      const existing = await db.collection("quotes").doc(parsed.date).get();
      if (existing.exists && !parsed.force) {
        await sendTelegramReply(message.chat.id, `Quote already exists for ${parsed.date}. Use /update or /add --force.`);
        res.status(200).send("duplicate-add");
        return;
      }

      await upsertQuote(parsed, "telegram");
      await sendTelegramReply(message.chat.id, parsed.force ? `Force-saved quote for ${parsed.date}` : `Saved quote for ${parsed.date}`);
      res.status(200).send("saved");
      return;
    }

    if (text.startsWith("/update")) {
      const parsed = parseAddPayload(text.replace("/update", "/add"));
      if (!parsed) {
        await sendTelegramReply(message.chat.id, "Invalid format/length. Use /update YYYY-MM-DD | quote text | accent(optional) | author(optional). Quote max length: 600");
        res.status(200).send("invalid-update");
        return;
      }

      const existing = await db.collection("quotes").doc(parsed.date).get();
      if (!existing.exists) {
        await sendTelegramReply(message.chat.id, `No quote exists for ${parsed.date}. Use /add to create it.`);
        res.status(200).send("missing-update-target");
        return;
      }

      await upsertQuote(parsed, "telegram-update");
      await sendTelegramReply(message.chat.id, `Updated quote for ${parsed.date}`);
      res.status(200).send("updated");
      return;
    }

    if (text.startsWith("/preview")) {
      const date = parseDateCommand(text, "/preview");
      if (!date) {
        await sendTelegramReply(message.chat.id, "Invalid format. Use /preview YYYY-MM-DD");
        res.status(200).send("invalid-preview");
        return;
      }

      const doc = await db.collection("quotes").doc(date).get();
      if (!doc.exists) {
        await sendTelegramReply(message.chat.id, `No quote scheduled for ${date}`);
        res.status(200).send("missing-preview-target");
        return;
      }

      const body = doc.get("text") as string | undefined;
      const accent = doc.get("accentLine") as string | undefined;
      const author = doc.get("author") as string | undefined;
      await sendTelegramReply(
        message.chat.id,
        [
          `Preview ${date}`,
          body ?? "",
          accent ? `Accent: ${accent}` : "",
          author ? `Author: ${author}` : ""
        ].filter(Boolean).join("\n")
      );
      res.status(200).send("previewed");
      return;
    }

    if (text.startsWith("/delete")) {
      const date = parseDateCommand(text, "/delete");
      if (!date) {
        await sendTelegramReply(message.chat.id, "Invalid format. Use /delete YYYY-MM-DD");
        res.status(200).send("invalid-delete");
        return;
      }

      await db.collection("quotes").doc(date).delete();
      await sendTelegramReply(message.chat.id, `Deleted ${date}`);
      res.status(200).send("deleted");
      return;
    }

    if (text.startsWith("/list")) {
      const month = parseMonthForList(text);
      const start = month ? `${month}-01` : new Date().toISOString().slice(0, 10);
      const end = month ? `${month}-31` : "9999-12-31";

      const snap = await db
        .collection("quotes")
        .where(admin.firestore.FieldPath.documentId(), ">=", start)
        .where(admin.firestore.FieldPath.documentId(), "<=", end)
        .orderBy(admin.firestore.FieldPath.documentId())
        .limit(31)
        .get();

      const lines = snap.docs.map((doc) => {
        const quote = (doc.get("text") as string | undefined)?.slice(0, 80) ?? "";
        return `${doc.id}: ${quote}`;
      });

      await sendTelegramReply(message.chat.id, lines.length ? lines.join("\n") : "No matching quotes");
      res.status(200).send("listed");
      return;
    }

    if (text.startsWith("/stats")) {
      const totalSnap = await db.collection("quotes").count().get();
      const total = totalSnap.data().count;
      const thisMonth = new Date().toISOString().slice(0, 7);
      const monthSnap = await db
        .collection("quotes")
        .where(admin.firestore.FieldPath.documentId(), ">=", `${thisMonth}-01`)
        .where(admin.firestore.FieldPath.documentId(), "<=", `${thisMonth}-31`)
        .count()
        .get();
      const monthCount = monthSnap.data().count;
      await sendTelegramReply(message.chat.id, `Stats\nTotal quotes: ${total}\n${thisMonth}: ${monthCount}`);
      res.status(200).send("stats");
      return;
    }

    if (text.startsWith("/help")) {
      await sendTelegramReply(message.chat.id, getHelpText());
      res.status(200).send("help");
      return;
    }

    await sendTelegramReply(message.chat.id, getHelpText());
    res.status(200).send("unknown-command");
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : "Unknown error";
    await sendTelegramReply(message.chat.id, `Error: ${errorMessage}`);
    res.status(500).send("error");
  }
});
