import React, { lazy, Suspense } from 'react';
import { createBrowserRouter, useRouteError, Link } from 'react-router-dom';
import { AlertTriangle, RefreshCw, Home, ArrowLeft } from 'lucide-react';
import ProtectedRoute from '../shared/components/ProtectedRoute';

// ─── Shared UI ────────────────────────────────────────────────────────────────

const PageLoader: React.FC = () => (
  <div
    style={{
      minHeight: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      background: 'var(--bg)',
    }}
  >
    <style>{`
      @keyframes fl-pulse {
        0%, 100% { opacity: 1; transform: scale(1); }
        50% { opacity: 0.4; transform: scale(0.92); }
      }
    `}</style>
    <div
      style={{
        width: 48, height: 48,
        borderRadius: 14,
        background: 'var(--brand)',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        color: '#fff',
        fontSize: 16, fontWeight: 700,
        fontFamily: "'Syne', system-ui, sans-serif",
        boxShadow: '0 4px 20px rgba(5,150,105,0.35)',
        animation: 'fl-pulse 1.4s ease-in-out infinite',
      }}
    >
      FL
    </div>
  </div>
);

const ErrorPage: React.FC = () => {
  const error = useRouteError() as Error | undefined;
  return (
    <div
      style={{
        minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center',
        background: 'var(--bg)', padding: '1.5rem',
      }}
    >
      <div style={{ textAlign: 'center', maxWidth: 420 }}>
        <div
          style={{
            width: 56, height: 56, borderRadius: 16,
            background: 'var(--red-bg)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            margin: '0 auto 20px',
          }}
        >
          <AlertTriangle size={28} style={{ color: 'var(--red)' }} />
        </div>
        <h1
          style={{
            fontFamily: "'Syne', system-ui, sans-serif",
            fontSize: 22, fontWeight: 700,
            color: 'var(--text-primary)', marginBottom: 8,
          }}
        >
          Something went wrong
        </h1>
        <p style={{ color: 'var(--text-muted)', fontSize: 14, marginBottom: 24, lineHeight: 1.6 }}>
          {error?.message || 'An unexpected error occurred. Please try again.'}
        </p>
        <button
          onClick={() => window.location.reload()}
          className="btn-primary"
          style={{ gap: 8 }}
        >
          <RefreshCw size={14} /> Reload page
        </button>
      </div>
    </div>
  );
};

const NotFound: React.FC = () => (
  <div
    style={{
      minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center',
      background: 'var(--bg)', padding: '1.5rem',
    }}
  >
    <div style={{ textAlign: 'center', maxWidth: 420 }}>
      <h1
        style={{
          fontFamily: "'Syne', system-ui, sans-serif",
          fontSize: 96, fontWeight: 700,
          color: 'var(--text-primary)', lineHeight: 1, marginBottom: 12,
        }}
      >
        404
      </h1>
      <p style={{ color: 'var(--text-secondary)', fontSize: 18, fontWeight: 500, marginBottom: 8 }}>
        This page doesn't exist.
      </p>
      <p style={{ color: 'var(--text-muted)', fontSize: 14, marginBottom: 28 }}>
        The URL you requested could not be found on this server.
      </p>
      <Link to="/" className="btn-primary" style={{ gap: 8, textDecoration: 'none' }}>
        <Home size={14} /> Go home
      </Link>
    </div>
  </div>
);

const Unauthorized: React.FC = () => (
  <div
    style={{
      minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center',
      background: 'var(--bg)', padding: '1.5rem',
    }}
  >
    <div style={{ textAlign: 'center', maxWidth: 420 }}>
      <h1
        style={{
          fontFamily: "'Syne', system-ui, sans-serif",
          fontSize: 96, fontWeight: 700,
          color: 'var(--text-primary)', lineHeight: 1, marginBottom: 12,
        }}
      >
        403
      </h1>
      <p style={{ color: 'var(--text-secondary)', fontSize: 18, fontWeight: 500, marginBottom: 8 }}>
        You don't have access to this page.
      </p>
      <p style={{ color: 'var(--text-muted)', fontSize: 14, marginBottom: 28 }}>
        Your current role doesn't have permission to view this content.
      </p>
      <Link to="/founder/dashboard" className="btn-primary" style={{ gap: 8, textDecoration: 'none' }}>
        <ArrowLeft size={14} /> Go to dashboard
      </Link>
    </div>
  </div>
);

// ─── Lazy Page Imports with Auto-Retry on Deployment ──────────────────────────

function lazyWithRetry<T extends React.ComponentType<any>>(
  componentImport: () => Promise<{ default: T }>
) {
  return lazy(async () => {
    const pageHasBeenForceRefreshed = JSON.parse(
      window.sessionStorage.getItem('page-has-been-force-refreshed') || 'false'
    );

    try {
      const component = await componentImport();
      window.sessionStorage.setItem('page-has-been-force-refreshed', 'false');
      return component;
    } catch (error: any) {
      if (!pageHasBeenForceRefreshed) {
        window.sessionStorage.setItem('page-has-been-force-refreshed', 'true');
        window.location.reload();
        return new Promise<{ default: T }>(() => {});
      }
      throw error;
    }
  });
}

const LandingPage        = lazyWithRetry(() => import('../features/common/LandingPage'));
const Login              = lazyWithRetry(() => import('../features/auth/Login'));
const Register           = lazyWithRetry(() => import('../features/auth/Register'));
const ForgotPassword     = lazyWithRetry(() => import('../features/auth/ForgotPassword'));
const ResetPassword      = lazyWithRetry(() => import('../features/auth/ResetPassword'));

const FounderDashboard      = lazyWithRetry(() => import('../features/founder/FounderDashboard'));
const FounderStartupDetail  = lazyWithRetry(() => import('../features/founder/FounderStartupDetail'));
const MyStartups            = lazyWithRetry(() => import('../features/founder/MyStartups'));
const CreateStartup      = lazyWithRetry(() => import('../features/founder/CreateStartup'));
const EditStartup        = lazyWithRetry(() => import('../features/founder/EditStartup'));
const TeamManagement     = lazyWithRetry(() => import('../features/founder/TeamManagement'));
const FounderInvestments = lazyWithRetry(() => import('../features/founder/FounderInvestments'));
const ReceivedPayments   = lazyWithRetry(() => import('../features/founder/ReceivedPayments'));

const CoFounderDashboard = lazyWithRetry(() => import('../features/cofounder/CoFounderDashboard'));
const MyInvitations      = lazyWithRetry(() => import('../features/founder/MyInvitations'));

const InvestorDashboard  = lazyWithRetry(() => import('../features/investor/InvestorDashboard'));
const BrowseStartups     = lazyWithRetry(() => import('../features/investor/BrowseStartups'));
const StartupDetail      = lazyWithRetry(() => import('../features/investor/StartupDetail'));
const MyInvestments      = lazyWithRetry(() => import('../features/investor/MyInvestments'));
const PaymentHistory     = lazyWithRetry(() => import('../features/investor/PaymentHistory'));

const AdminDashboard     = lazyWithRetry(() => import('../features/admin/AdminDashboard'));

const Notifications      = lazyWithRetry(() => import('../features/common/Notifications'));
const Messages           = lazyWithRetry(() => import('../features/common/Messages'));
const Chat               = lazyWithRetry(() => import('../features/common/Chat'));
const Profile            = lazyWithRetry(() => import('../features/common/Profile'));

const s = (Component: React.LazyExoticComponent<React.ComponentType<any>>) => (
  <Suspense fallback={<PageLoader />}>
    <Component />
  </Suspense>
);

// ─── Router ───────────────────────────────────────────────────────────────────

const router = createBrowserRouter([
  { path: '/',              element: s(LandingPage) },
  { path: '/login',         element: s(Login) },
  { path: '/register',      element: s(Register) },
  { path: '/forgot-password', element: s(ForgotPassword) },
  { path: '/reset-password',  element: s(ResetPassword) },
  { path: '/unauthorized',  element: <Unauthorized /> },
  { path: '*',              element: <NotFound /> },

  {
    element: <ProtectedRoute allowedRoles={['ROLE_FOUNDER']} />,
    errorElement: <ErrorPage />,
    children: [
      { path: '/founder/dashboard',         element: s(FounderDashboard) },
      { path: '/founder/startups',          element: s(MyStartups) },
      { path: '/founder/startups/create',   element: s(CreateStartup) },
      { path: '/founder/startups/:id',      element: s(FounderStartupDetail) },
      { path: '/founder/startups/:id/edit', element: s(EditStartup) },
      { path: '/founder/team/:startupId',   element: s(TeamManagement) },
      { path: '/founder/investments',       element: s(FounderInvestments) },
      { path: '/founder/payments',          element: s(ReceivedPayments) },
    ],
  },

  {
    element: <ProtectedRoute allowedRoles={['ROLE_COFOUNDER']} />,
    errorElement: <ErrorPage />,
    children: [
      { path: '/cofounder/dashboard',       element: s(CoFounderDashboard) },
      { path: '/cofounder/startups',        element: s(BrowseStartups) },
      { path: '/cofounder/startups/:id',    element: s(StartupDetail) },
      { path: '/founder/invitations',       element: s(MyInvitations) },
    ],
  },

  {
    element: <ProtectedRoute allowedRoles={['ROLE_INVESTOR']} />,
    errorElement: <ErrorPage />,
    children: [
      { path: '/investor/dashboard',        element: s(InvestorDashboard) },
      { path: '/investor/startups',         element: s(BrowseStartups) },
      { path: '/investor/startups/:id',     element: s(StartupDetail) },
      { path: '/investor/investments',      element: s(MyInvestments) },
      { path: '/investor/payments',         element: s(PaymentHistory) },
    ],
  },

  {
    element: <ProtectedRoute allowedRoles={['ROLE_ADMIN']} />,
    errorElement: <ErrorPage />,
    children: [
      { path: '/admin/dashboard',           element: s(AdminDashboard) },
    ],
  },

  {
    element: <ProtectedRoute />,
    errorElement: <ErrorPage />,
    children: [
      { path: '/notifications',             element: s(Notifications) },
      { path: '/messages',                  element: s(Messages) },
      { path: '/messages/:conversationId',  element: s(Chat) },
      { path: '/profile',                   element: s(Profile) },
    ],
  },
]);

export default router;
