import React from 'react';
import { Link, useLocation } from 'react-router-dom';

export default function Layout({ user, onLogout, children }) {
  const [isNavOpen, setIsNavOpen] = React.useState(false);
  const location = useLocation();
  const hideNav = location.pathname === '/login';

  return (
    <div className="app-shell">
      {!hideNav && (
        <nav className="navbar navbar-expand-lg sticky-top">
          <div className="container-fluid px-4">
            <Link className="navbar-brand d-flex align-items-center gap-2" to="/diagramas">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="text-primary-500">
                <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" />
              </svg>
              Casos UML
            </Link>

            <button
              className="navbar-toggler border-0"
              type="button"
              onClick={() => setIsNavOpen(!isNavOpen)}
              aria-controls="navMain"
              aria-expanded={isNavOpen}
              aria-label="Toggle navigation"
            >
              <span className="navbar-toggler-icon" style={{ filter: 'invert(1)' }} />
            </button>

            <div className={`collapse navbar-collapse ${isNavOpen ? 'show' : ''}`} id="navMain">
              <ul className="navbar-nav me-auto mb-2 mb-lg-0 ms-lg-4">
                <li className="nav-item">
                  <Link
                    className={`nav-link ${location.pathname.startsWith('/diagramas') ? 'active' : ''}`}
                    to="/diagramas"
                    onClick={() => setIsNavOpen(false)}
                  >
                    Mis Diagramas
                  </Link>
                </li>
              </ul>

              {user && (
                <div className="d-flex flex-lg-row flex-column align-items-lg-center gap-3 mt-3 mt-lg-0">
                  <div className="d-flex flex-column align-items-lg-end text-lg-end">
                    <span className="text-sm font-medium" style={{ color: 'var(--text-primary)' }}>
                      {user.nombre_usuario}
                    </span>
                    <span className="text-xs text-uppercase" style={{ color: 'var(--text-secondary)', fontSize: '0.75rem' }}>
                      {user.nombre_rol}
                    </span>
                  </div>

                  <button
                    className="btn btn-outline-danger btn-sm d-flex align-items-center gap-2"
                    type="button"
                    onClick={onLogout}
                    title="Cerrar sesión"
                  >
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
                      <polyline points="16 17 21 12 16 7" />
                      <line x1="21" y1="12" x2="9" y2="12" />
                    </svg>
                    <span className="d-lg-none">Cerrar Sesión</span>
                  </button>
                </div>
              )}
            </div>
          </div>
        </nav>
      )}
      <main className="flex-grow-1 w-100">
        <div className="container-fluid px-4 py-4">
          {children}
        </div>
      </main>
    </div>
  );
}
