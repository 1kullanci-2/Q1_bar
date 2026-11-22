// Medya Paneli kurulum sayacı (Cloudflare Worker + KV, bağlama adı: KV)
// POST /install {"id": "<uuid>"}  -> her yeni kimlik için toplamı 1 artırır (aynı kimlik iki kez sayılmaz)
// GET  /count                     -> {"total": N}
const H = { "content-type": "application/json", "access-control-allow-origin": "*" };

export default {
  async fetch(req, env) {
    const url = new URL(req.url);

    if (url.pathname === "/install" && req.method === "POST") {
      let id;
      try { id = (await req.json()).id; } catch (e) {}
      if (typeof id !== "string" || !/^[0-9a-f-]{36}$/.test(id)) {
        return new Response('{"error":"bad id"}', { status: 400, headers: H });
      }
      if (!(await env.KV.get("id:" + id))) {
        await env.KV.put("id:" + id, "1");
        const total = parseInt((await env.KV.get("total")) || "0", 10) + 1;
        await env.KV.put("total", String(total));
      }
      return new Response('{"ok":true}', { headers: H });
    }

    if (url.pathname === "/count") {
      const total = parseInt((await env.KV.get("total")) || "0", 10);
      return new Response(JSON.stringify({ total }), { headers: H });
    }

    return new Response("Medya Paneli sayaç", { status: 404 });
  },
};
