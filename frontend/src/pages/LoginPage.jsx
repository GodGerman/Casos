import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AlertMessage from '../components/AlertMessage.jsx';
import Loading from '../components/Loading.jsx';
import { login } from '../services/auth.js';

export default function LoginPage({ onLogin }) {
  const navigate = useNavigate();
  const [nombre_usuario, set_nombre_usuario] = useState('');
  const [contrasena, set_contrasena] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // Auto-dismiss error after 3 seconds
  React.useEffect(() => {
    if (error) {
      const timer = setTimeout(() => {
        setError('');
      }, 3000);
      return () => clearTimeout(timer);
    }
  }, [error]);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');

    // Validate empty fields
    if (!nombre_usuario.trim() || !contrasena.trim()) {
      setError('Ingresa usuario y contrasena.');
      return;
    }

    setLoading(true);
    try {
      const result = await login(nombre_usuario.trim(), contrasena.trim());
      if (result && result.ok) {
        onLogin({
          id_usuario: result.id_usuario,
          id_rol: result.id_rol,
          nombre_usuario: result.nombre_usuario,
          nombre_rol: result.nombre_rol
        });
        navigate('/diagramas');
      } else {
        // Invalid credentials: show error and clear inputs
        setError('Credenciales invalidas.');
        set_nombre_usuario('');
        set_contrasena('');
      }
    } catch (err) {
      const mensaje = err?.data?.mensaje || 'Error al iniciar sesion.';
      setError(mensaje);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      <div className="card shadow-sm login-card">
        <div className="card-body">
          <h3 className="mb-3">Inicio de sesion</h3>
          <AlertMessage type="danger" message={error} toast={true} />
          <form onSubmit={handleSubmit}>
            <div className="mb-3">
              <label className="form-label" htmlFor="nombre_usuario">Usuario</label>
              <input
                id="nombre_usuario"
                className="form-control"
                type="text"
                value={nombre_usuario}
                onChange={(event) => set_nombre_usuario(event.target.value)}
                placeholder="ADMINISTRADOR"
              />
            </div>
            <div className="mb-3">
              <label className="form-label" htmlFor="contrasena">Contrasena</label>
              <input
                id="contrasena"
                className="form-control"
                type="password"
                value={contrasena}
                onChange={(event) => set_contrasena(event.target.value)}
                placeholder="1234"
              />
            </div>
            <button className="btn btn-primary w-100" type="submit" disabled={loading}>
              {loading ? 'Ingresando...' : 'Ingresar'}
            </button>
          </form>
          {loading && <div className="mt-3"><Loading text="Validando credenciales..." /></div>}
        </div>
      </div>
    </div>
  );
}
