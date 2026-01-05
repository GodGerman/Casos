import React, { useEffect, useState } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import Layout from './components/Layout.jsx';
import LoginPage from './pages/LoginPage.jsx';
import DiagramsPage from './pages/DiagramsPage.jsx';
import DiagramEditorPage from './pages/DiagramEditorPage.jsx';
import MediaManagerPage from './pages/MediaManagerPage.jsx';
import NotFoundPage from './pages/NotFoundPage.jsx';
import { logout } from './services/auth.js';

// Llave de storage para persistir sesion del usuario entre recargas.
const STORAGE_KEY = 'session_user';

/**
 * Enrutador protegido que redirige a /login si no hay usuario.
 *
 * Se recibe el usuario desde estado global y, si es null,
 * retorna un <Navigate> que reemplaza el historial.
 *
 *
 * @param {{ user: object|null, children: React.ReactNode }} props props del componente.
 * @returns {JSX.Element} children o redireccion al login.
 */
function PrivateRoute({ user, children }) {
  if (!user) {
    return <Navigate to="/login" replace />;
  }
  return children;
}

/**
 * Componente raiz que gestiona sesion y rutas principales.
 *
 * Se restaura sesion desde localStorage en el primer render,
 * expone handlers de login/logout y define rutas protegidas con PrivateRoute.
 *
 *
 * @returns {JSX.Element} arbol principal de la aplicacion.
 */
export default function App() {
  const [user, setUser] = useState(null);

  useEffect(() => {
    // Restaura sesion guardada en localStorage si existe.
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored) {
      try {
        setUser(JSON.parse(stored));
      } catch (err) {
        // JSON corrupto: limpia el storage para evitar loops.
        localStorage.removeItem(STORAGE_KEY);
      }
    }
  }, []);

  /**
   * Persiste usuario autenticado en memoria y storage.
   *
   * Se actualiza el estado y escribe el JSON en localStorage
   * para mantener la sesion tras refresh.
   *
   *
   * @param {object} userInfo datos de usuario autenticado.
   * @returns {void} no retorna valor; actualiza estado local.
   */
  const handleLogin = (userInfo) => {
    setUser(userInfo);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(userInfo));
  };

  /**
   * Cierra sesion en backend y limpia storage local.
   * No lanza error al usuario si el backend falla.
   *
   * Se intenta llamar /api/auth/logout y, pase lo que pase,
   * limpia el estado y localStorage para forzar salida local.
   *
   *
   * @returns {Promise<void>} no retorna valor; limpia estado local.
   */
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
