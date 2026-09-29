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

// ─── Lazy Page Imports ────────────────────────────────────────────────────────

const LandingPage        = lazy(() => import('../features/common/LandingPage'));
const Login              = lazy(() => import('../features/auth/Login'));
const Register           = lazy(() => import('../features/auth/Register'));
const ForgotPassword     = lazy(() => import('../features/auth/ForgotPassword'));
const ResetPassword      = lazy(() => import('../features/auth/ResetPassword'));

const FounderDashboard      = lazy(() => import('../features/founder/FounderDashboard'));
const FounderStartupDetail  = lazy(() => import('../features/founder/FounderStartupDetail'));
const MyStartups            = lazy(() => import('../features/founder/MyStartups'));
const CreateStartup      = lazy(() => import('../features/founder/CreateStartup'));
const EditStartup        = lazy(() => import('../features/founder/EditStartup'));
const TeamManagement     = lazy(() => import('../features/founder/TeamManagement'));
const FounderInvestments = lazy(() => import('../features/founder/FounderInvestments'));
const ReceivedPayments   = lazy(() => import('../features/founder/ReceivedPayments'));

const CoFounderDashboard = lazy(() => import('../features/cofounder/CoFounderDashboard'));
const MyInvitations      = lazy(() => import('../features/founder/MyInvitations'));

const InvestorDashboard  = lazy(() => import('../features/investor/InvestorDashboard'));
const BrowseStartups     = lazy(() => import('../features/investor/BrowseStartups'));
const StartupDetail      = lazy(() => import('../features/investor/StartupDetail'));
const MyInvestments      = lazy(() => import('../features/investor/MyInvestments'));
const PaymentHistory     = lazy(() => import('../features/investor/PaymentHistory'));

const AdminDashboard     = lazy(() => import('../features/admin/AdminDashboard'));

const Notifications      = lazy(() => import('../features/common/Notifications'));
const Messages           = lazy(() => import('../features/common/Messages'));
const Chat               = lazy(() => import('../features/common/Chat'));
const Profile            = lazy(() => import('../features/common/Profile'));

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
