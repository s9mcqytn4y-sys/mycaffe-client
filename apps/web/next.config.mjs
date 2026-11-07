/** @type {import('next').NextConfig} */
const backendInternalUrl = (process.env.BACKEND_INTERNAL_URL || "http://127.0.0.1:8080").replace(/\/+$/, "");

const nextConfig = {
  reactStrictMode: true,
  poweredByHeader: false,
  async rewrites() {
    return [
      {
        source: "/api/:path*",
        destination: `${backendInternalUrl}/api/:path*`
      },
      {
        source: "/health/:path*",
        destination: `${backendInternalUrl}/health/:path*`
      }
    ];
  }
};

export default nextConfig;

