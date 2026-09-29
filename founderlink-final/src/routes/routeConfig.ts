// Centralized route path constants
export const ROUTES = {
  HOME: '/',
  LOGIN: '/login',
  REGISTER: '/register',
  UNAUTHORIZED: '/unauthorized',
  // Founder
  FOUNDER_DASHBOARD: '/founder/dashboard',
  FOUNDER_STARTUPS: '/founder/startups',
  FOUNDER_CREATE_STARTUP: '/founder/startups/create',
  FOUNDER_EDIT_STARTUP: '/founder/startups/:id/edit',
  FOUNDER_TEAM: '/founder/team/:startupId',
  FOUNDER_INVESTMENTS: '/founder/investments',
  FOUNDER_PAYMENTS: '/founder/payments',
  // Investor
  INVESTOR_DASHBOARD: '/investor/dashboard',
  INVESTOR_BROWSE: '/investor/startups',
  INVESTOR_STARTUP_DETAIL: '/investor/startups/:id',
  INVESTOR_INVESTMENTS: '/investor/investments',
  INVESTOR_PAYMENTS: '/investor/payments',
  // Co-Founder
  COFOUNDER_DASHBOARD: '/cofounder/dashboard',
  COFOUNDER_BROWSE: '/cofounder/startups',
  COFOUNDER_STARTUP_DETAIL: '/cofounder/startups/:id',
  FOUNDER_INVITATIONS: '/founder/invitations',
  // Admin
  ADMIN_DASHBOARD: '/admin/dashboard',
  // Common
  NOTIFICATIONS: '/notifications',
  MESSAGES: '/messages',
  CHAT: '/messages/:conversationId',
  PROFILE: '/profile',
} as const;

export type RoutePath = typeof ROUTES[keyof typeof ROUTES];
