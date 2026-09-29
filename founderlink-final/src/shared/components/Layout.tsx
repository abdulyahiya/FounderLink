import React from 'react';
import { useEffect, useState } from 'react';
import { Menu, X } from 'lucide-react';
import Navbar from './Navbar';
import Sidebar from './Sidebar';

interface LayoutProps {
  children: React.ReactNode;
}

const Layout: React.FC<LayoutProps> = ({ children }) => (
  <ResponsiveLayout>{children}</ResponsiveLayout>
);

const DESKTOP_BREAKPOINT = 1024;

const ResponsiveLayout: React.FC<LayoutProps> = ({ children }) => {
  const [mobileNavOpen, setMobileNavOpen] = useState(false);
  const [isDesktop, setIsDesktop] = useState(() =>
    typeof window !== 'undefined' ? window.innerWidth >= DESKTOP_BREAKPOINT : true
  );

  useEffect(() => {
    const handleResize = () => {
      const desktop = window.innerWidth >= DESKTOP_BREAKPOINT;
      setIsDesktop(desktop);
      if (desktop) setMobileNavOpen(false);
    };

    handleResize();
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  return (
    <div className="min-h-screen flex flex-col" style={{ background: 'var(--bg)' }}>
      <Navbar />
      <div className="flex flex-1 min-h-0" style={{ position: 'relative' }}>
        {isDesktop ? (
          <aside
            style={{
              width: 240,
              borderRight: '1px solid var(--border)',
              background: 'var(--surface)',
              overflowY: 'auto',
              flexShrink: 0,
            }}
          >
            <Sidebar />
          </aside>
        ) : (
          <>
            <button
              type="button"
              aria-label={mobileNavOpen ? 'Close navigation menu' : 'Open navigation menu'}
              onClick={() => setMobileNavOpen((open) => !open)}
              style={{
                position: 'fixed',
                right: 16,
                bottom: 16,
                width: 48,
                height: 48,
                borderRadius: 14,
                border: '1px solid var(--border)',
                background: 'var(--surface)',
                color: 'var(--text-primary)',
                boxShadow: 'var(--shadow-hover)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                zIndex: 60,
              }}
            >
              {mobileNavOpen ? <X size={20} /> : <Menu size={20} />}
            </button>

            {mobileNavOpen && (
              <div
                onClick={() => setMobileNavOpen(false)}
                style={{
                  position: 'fixed',
                  inset: 0,
                  background: 'rgba(10, 10, 10, 0.35)',
                  backdropFilter: 'blur(4px)',
                  zIndex: 55,
                }}
              >
                <aside
                  onClick={(event) => event.stopPropagation()}
                  style={{
                    width: 'min(84vw, 320px)',
                    height: '100%',
                    background: 'var(--surface)',
                    borderRight: '1px solid var(--border)',
                    boxShadow: 'var(--shadow-hover)',
                    overflowY: 'auto',
                  }}
                >
                  <Sidebar onNavigate={() => setMobileNavOpen(false)} />
                </aside>
              </div>
            )}
          </>
        )}

        <main
          className="flex-1 overflow-auto"
          style={{
            minWidth: 0,
            padding: isDesktop ? '32px' : '20px 16px 88px',
          }}
        >
          {children}
        </main>
      </div>
    </div>
  );
};

export default Layout;
