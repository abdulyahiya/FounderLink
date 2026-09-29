import React from 'react';
import Navbar from '../shared/components/Navbar';
import Sidebar from '../shared/components/Sidebar';

interface MainLayoutProps {
  children: React.ReactNode;
}

const NAVBAR_H = 53;
const SIDEBAR_W = 224;

const MainLayout: React.FC<MainLayoutProps> = ({ children }) => (
  <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh', background: 'var(--bg)' }}>
    {/* Navbar — sticky top */}
    <Navbar />
    {/* Body row */}
    <div style={{ display: 'flex', flex: 1, paddingTop: 0 }}>
      {/* Fixed sidebar */}
      <aside
        style={{
          position: 'fixed',
          top: NAVBAR_H,
          left: 0,
          bottom: 0,
          width: SIDEBAR_W,
          overflowY: 'auto',
          zIndex: 40,
          background: 'var(--surface)',
          borderRight: '1px solid var(--border)',
        }}
      >
        <Sidebar />
      </aside>
      {/* Scrollable content */}
      <main
        style={{
          marginLeft: SIDEBAR_W,
          flex: 1,
          padding: '32px',
          overflowY: 'auto',
          minWidth: 0,
        }}
      >
        {children}
      </main>
    </div>
  </div>
);

export default MainLayout;
