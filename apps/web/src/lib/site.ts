// Set at build time (web-publish.yml); localhost keeps dev and test builds working.
export const siteUrl = process.env.NEXT_PUBLIC_APP_URL || "http://localhost:3000";
