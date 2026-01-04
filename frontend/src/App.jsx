import React, { useEffect, useState } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import Layout from './components/Layout.jsx';
import LoginPage from './pages/LoginPage.jsx';
import DiagramsPage from './pages/DiagramsPage.jsx';
import DiagramEditorPage from './pages/DiagramEditorPage.jsx';
import MediaManagerPage from './pages/MediaManagerPage.jsx';
import NotFoundPage from './pages/NotFoundPage.jsx';
import { logout } from './services/auth.js';

const STORAGE_KEY = 'session_user';

function PrivateRoute({ user, children }) {
  if (!user) {
    return <Navigate to="/login" replace />;
  }
  return children;
}

export default function App() {
  const [user, setUser] = useState(null);

  useEffect(() => {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored) {
      try {
        setUser(JSON.parse(stored));
      } catch (err) {
        localStorage.removeItem(STORAGE_KEY);
      }
    }
  }, []);

  const handleLogin = (userInfo) => {
    setUser(userInfo);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(userInfo));
  };

  const handleLogout = async () => {
    try {
      await logout();
    } catch (err) {
      // ignore errors on logout
    }
    setUser(null);
    localStorage.removeItem(STORAGE_KEY);
  };

  return (
    <BrowserRouter>
      <Layout user={user} onLogout={handleLogout}>
        <Routes>
          <Route
            path="/login"
            element={user ? <Navigate to="/diagramas" replace /> : <LoginPage onLogin={handleLogin} />}
          />
          <Route
            path="/diagramas"
            element={(
              <PrivateRoute user={user}>
                <DiagramsPage />
              </PrivateRoute>
            )}
          />
          <Route
            path="/diagramas/:id"
            element={(
              <PrivateRoute user={user}>
                <DiagramEditorPage />
              </PrivateRoute>
            )}
          />
          <Route
            path="/diagramas/:id/multimedia"
            element={(
              <PrivateRoute user={user}>
                <MediaManagerPage />
              </PrivateRoute>
            )}
          />
          <Route path="/" element={<Navigate to={user ? '/diagramas' : '/login'} replace />} />
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </Layout>
    </BrowserRouter>
  );
}
