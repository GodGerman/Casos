import React, { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import AlertMessage from '../components/AlertMessage.jsx';
import Loading from '../components/Loading.jsx';
import {
  crearDiagrama,
  eliminarDiagrama,
  listarDiagramas
} from '../services/diagramas.js';
import { validarDiagrama } from '../utils/validators.js';

const ESTADOS = [
  { value: 'ACTIVO', label: 'Activo', color: 'success' },
  { value: 'BORRADOR', label: 'Borrador', color: 'warning' },
  { value: 'ARCHIVADO', label: 'Archivado', color: 'secondary' }
];

export default function DiagramsPage() {
  const navigate = useNavigate();
  const [diagramas, setDiagramas] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [filtro, setFiltro] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({
    nombre: '',
    descripcion: '',
    estado: 'ACTIVO',
    ancho_lienzo: 1280,
    alto_lienzo: 720
  });

  const cargar = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await listarDiagramas();
      setDiagramas(data.diagramas || []);
    } catch (err) {
      setError(err?.data?.mensaje || 'No se pudieron cargar los diagramas.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    cargar();
  }, []);

  const handleChange = (event) => {
    const { name, value } = event.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleCreate = async (event) => {
    event.preventDefault();
    setError('');
    setSuccess('');
    const errores = validarDiagrama(form);
    if (errores.length > 0) {
      setError(errores.join(' '));
      return;
    }
    try {
      const payload = {
        nombre: form.nombre.trim(),
        descripcion: form.descripcion.trim() || null,
        estado: form.estado,
        ancho_lienzo: Number(form.ancho_lienzo),
        alto_lienzo: Number(form.alto_lienzo)
      };
      const result = await crearDiagrama(payload);
      if (result?.id_diagrama) {
        setSuccess('Diagrama creado.');
        setForm({ nombre: '', descripcion: '', estado: 'ACTIVO', ancho_lienzo: 1280, alto_lienzo: 720 });
        setShowForm(false);
        await cargar();
        navigate(`/diagramas/${result.id_diagrama}`);
      }
    } catch (err) {
      setError(err?.data?.mensaje || 'Error al crear el diagrama.');
    }
  };

  const handleDelete = async (e, id) => {
    e.preventDefault(); // prevent link navigation if inside a link
    e.stopPropagation();
    if (!window.confirm('¿Deseas eliminar este diagrama permanentemente?')) {
      return;
    }
    setError('');
    setSuccess('');
    try {
      await eliminarDiagrama(id);
      setSuccess('Diagrama eliminado.');
      await cargar();
    } catch (err) {
      setError(err?.data?.mensaje || 'Error al eliminar el diagrama.');
    }
  };

  const filtrados = useMemo(() => {
    if (!filtro.trim()) {
      return diagramas;
    }
    const texto = filtro.toLowerCase();
    return diagramas.filter((d) => (d.nombre || '').toLowerCase().includes(texto));
  }, [diagramas, filtro]);

  return (
    <div className="container-xl">
      <div className="d-flex flex-column flex-md-row justify-content-between align-items-md-center mb-4 gap-3">
        <div>
          <h1 className="h3 mb-1 fw-bold text-primary-400">Mis Diagramas</h1>
          <p className="text-secondary mb-0">Gestiona y edita tus diagramas UML</p>
        </div>
        <div className="d-flex gap-2">
          <button
            className="btn btn-primary d-flex align-items-center gap-2"
            onClick={() => setShowForm(!showForm)}
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <line x1="12" y1="5" x2="12" y2="19"></line>
              <line x1="5" y1="12" x2="19" y2="12"></line>
            </svg>
            Nuevo Diagrama
          </button>
        </div>
      </div>

      <AlertMessage type="danger" message={error} />
      <AlertMessage type="success" message={success} />

      {showForm && (
        <div className="card mb-4 border-primary-500 shadow-lg animate-fade-in">
          <div className="card-body">
            <div className="d-flex justify-content-between align-items-center mb-3">
              <h5 className="card-title mb-0 text-primary-300">Crear Nuevo Diagrama</h5>
              <button className="btn-close btn-close-white" onClick={() => setShowForm(false)}></button>
            </div>
            <form onSubmit={handleCreate}>
              <div className="row g-3">
                <div className="col-md-6">
                  <label htmlFor="nombre" className="form-label text-sm text-secondary">Nombre del Proyecto</label>
                  <input
                    id="nombre"
                    name="nombre"
                    className="form-control"
                    placeholder="Ej. Sistema de Ventas"
                    value={form.nombre}
                    onChange={handleChange}
                    maxLength={120}
                    required
                    autoFocus
                  />
                </div>
                <div className="col-md-3">
                  <label htmlFor="estado" className="form-label text-sm text-secondary">Estado</label>
                  <select
                    id="estado"
                    name="estado"
                    className="form-select"
                    value={form.estado}
                    onChange={handleChange}
                  >
                    {ESTADOS.map((e) => (
                      <option key={e.value} value={e.value}>{e.label}</option>
                    ))}
                  </select>
                </div>
                <div className="col-md-3">
                  <label className="form-label text-sm text-secondary">Tamaño (px)</label>
                  <div className="input-group">
                    <input name="ancho_lienzo" type="number" className="form-control" placeholder="W" value={form.ancho_lienzo} onChange={handleChange} />
                    <span className="input-group-text bg-dark-700 border-dark-600">x</span>
                    <input name="alto_lienzo" type="number" className="form-control" placeholder="H" value={form.alto_lienzo} onChange={handleChange} />
                  </div>
                </div>
                <div className="col-12">
                  <label htmlFor="descripcion" className="form-label text-sm text-secondary">Descripción (Opcional)</label>
                  <textarea
                    id="descripcion"
                    name="descripcion"
                    className="form-control"
                    rows="2"
                    placeholder="Breve descripción del propósito del diagrama..."
                    value={form.descripcion}
                    onChange={handleChange}
                  />
                </div>
                <div className="col-12 text-end">
                  <button type="button" className="btn btn-link text-secondary text-decoration-none me-2" onClick={() => setShowForm(false)}>Cancelar</button>
                  <button type="submit" className="btn btn-primary px-4">Crear Proyecto</button>
                </div>
              </div>
            </form>
          </div>
        </div>
      )}

      <div className="mb-4">
        <div className="input-group w-100" style={{ maxWidth: '400px' }}>
          <span className="input-group-text bg-dark-800 border-dark-700 text-secondary">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
          </span>
          <input
            type="text"
            className="form-control border-start-0 ps-0"
            placeholder="Buscar diagramas..."
            value={filtro}
            onChange={(e) => setFiltro(e.target.value)}
          />
        </div>
      </div>

      {loading ? (
        <div className="py-5 text-center">
          <Loading text="Cargando tus diagramas..." />
        </div>
      ) : (
        <>
          {filtrados.length === 0 ? (
            <div className="text-center py-5 opacity-50">
              <div className="mb-3">
                <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1" strokeLinecap="round" strokeLinejoin="round" className="text-secondary"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect><line x1="3" y1="9" x2="21" y2="9"></line><line x1="9" y1="21" x2="9" y2="9"></line></svg>
              </div>
              <h4>No se encontraron diagramas</h4>
              <p>Crea uno nuevo para empezar a diseñar.</p>
            </div>
          ) : (
            <div className="row g-4">
              {filtrados.map((diag) => (
                <div key={diag.id_diagrama} className="col-md-6 col-lg-4 col-xl-3">
                  <div className="card h-100 diagram-card hover-lift transition-all group">
                    <div className="card-body d-flex flex-column">
                      <div className="d-flex justify-content-between align-items-start mb-3">
                        <div className="p-2 rounded bg-primary-900 text-primary-400">
                          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                            <circle cx="12" cy="12" r="10"></circle>
                            <circle cx="12" cy="12" r="3"></circle>
                          </svg>
                        </div>
                        <span className={`badge bg-${ESTADOS.find(e => e.value === diag.estado)?.color || 'secondary'} bg-opacity-10 text-${ESTADOS.find(e => e.value === diag.estado)?.color || 'secondary'} border border-${ESTADOS.find(e => e.value === diag.estado)?.color || 'secondary'}`}>
                          {diag.estado}
                        </span>
                      </div>

                      <h5 className="card-title fw-bold text-white mb-2 text-truncate" title={diag.nombre}>
                        {diag.nombre}
                      </h5>
                      <p className="card-text text-secondary text-sm flex-grow-1 line-clamp-3">
                        {diag.descripcion || 'Sin descripción'}
                      </p>

                      <div className="mt-4 pt-3 border-top border-dark-700 d-flex justify-content-between align-items-center">
                        <Link
                          to={`/diagramas/${diag.id_diagrama}`}
                          className="btn btn-sm btn-outline-primary stretched-link-custom z-10"
                        >
                          Abrir Editor
                        </Link>

                        <div className="dropdown z-20">
                          <button className="btn btn-icon btn-sm text-secondary hover-text-white" type="button" data-bs-toggle="dropdown" aria-expanded="false">
                            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="1"></circle><circle cx="19" cy="12" r="1"></circle><circle cx="5" cy="12" r="1"></circle></svg>
                          </button>
                          <ul className="dropdown-menu dropdown-menu-end dropdown-menu-dark shadow-lg border-dark-700">
                            <li>
                              <Link className="dropdown-item" to={`/diagramas/${diag.id_diagrama}/multimedia`}>
                                Gestor Multimedia
                              </Link>
                            </li>
                            <li><hr className="dropdown-divider border-dark-700" /></li>
                            <li>
                              <button className="dropdown-item text-danger" onClick={(e) => handleDelete(e, diag.id_diagrama)}>
                                Eliminar
                              </button>
                            </li>
                          </ul>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </>
      )}
    </div>
  );
}
