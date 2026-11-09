import { test, expect } from "@playwright/test";

test("renews an expired staff token once and retries the KDS requests", async ({ page }) => {
  let refreshCalls = 0;

  await page.addInitScript(() => {
    if (!sessionStorage.getItem("seeded")) {
      sessionStorage.setItem("seeded", "1");
      localStorage.setItem("mycaffe_store_token", "expired-token");
      localStorage.setItem("mycaffe_store_slug", "central-cafe");
    }
  });

  await page.route("**/api/v1/auth/staff/refresh", async (route) => {
    refreshCalls += 1;
    expect(route.request().headers()["x-csrf-protection"]).toBe("1");
    await route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({ access_token: "fresh-token" }) });
  });

  await page.route("**/api/v1/pos/ws-ticket", (route) => route.fulfill({ status: 503, body: "{}" }));

  await page.route("**/api/v1/kds/tickets?station=*", async (route) => {
    if (route.request().headers()["authorization"] !== "Bearer fresh-token") {
      await route.fulfill({ status: 401, contentType: "application/json", body: JSON.stringify({ error: "expired" }) });
      return;
    }
    const station = new URL(route.request().url()).searchParams.get("station");
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        tickets: station === "bar"
          ? [{ id: "t1", queue_number: "A-001", station: "bar", status: "queued", created_at: "2026-11-09T10:00:00Z", lines: [{ name: "Caffe Latte", quantity: 1 }] }]
          : []
      })
    });
  });

  await page.goto("/kds");

  await expect(page.getByText("Caffe Latte")).toBeVisible();
  expect(refreshCalls).toBe(1);
  expect(await page.evaluate(() => localStorage.getItem("mycaffe_store_token"))).toBe("fresh-token");
});
