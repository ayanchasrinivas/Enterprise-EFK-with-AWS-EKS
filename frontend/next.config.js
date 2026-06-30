/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  swcMinify: true,
  env: {
    NEXT_PUBLIC_API_BASE_URL: process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080',
    NEXT_PUBLIC_INCIDENT_API: process.env.NEXT_PUBLIC_INCIDENT_API || 'http://localhost:8080/api/v1/incidents',
    NEXT_PUBLIC_ONCALL_API: process.env.NEXT_PUBLIC_ONCALL_API || 'http://localhost:8081/api/v1',
    NEXT_PUBLIC_NOTIFICATION_API: process.env.NEXT_PUBLIC_NOTIFICATION_API || 'http://localhost:8082/api/v1/notifications',
    NEXT_PUBLIC_POSTMORTEM_API: process.env.NEXT_PUBLIC_POSTMORTEM_API || 'http://localhost:8083/api/v1/postmortems',
  },
  headers: async () => [
    {
      source: '/:path*',
      headers: [
        { key: 'X-Content-Type-Options', value: 'nosniff' },
        { key: 'X-Frame-Options', value: 'SAMEORIGIN' },
        { key: 'X-XSS-Protection', value: '1; mode=block' },
        { key: 'Referrer-Policy', value: 'strict-origin-when-cross-origin' },
      ],
    },
  ],
};

module.exports = nextConfig;
