import type { MetadataRoute } from "next";

export default function robots(): MetadataRoute.Robots {
  const baseUrl = process.env.NEXT_PUBLIC_APP_URL || "https://abbdullaziz.online";

  return {
    rules: {
      userAgent: "*",
      allow: ["/", "/onboarding", "/legal"],
      disallow: [
        "/order/*",
        "/store/*",
        "/kds/*",
        "/queue/*",
        "/platform/*",
        "/api/*",
      ],
    },
    sitemap: `${baseUrl}/sitemap.xml`,
  };
}

