import type { MetadataRoute } from "next";
import { siteUrl } from "@/lib/site";

// Only public pages; table QR links stay private.
export default function sitemap(): MetadataRoute.Sitemap {
  return ["/", "/legal", "/legal/mitra"].map((path) => ({ url: new URL(path, siteUrl).toString() }));
}
